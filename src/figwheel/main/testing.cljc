(ns figwheel.main.testing
  (:require
   [clojure.string :as string]
   #?@(:cljs [[figwheel.main.async-result :as async-result]
              [cljs.test :refer [report]]]
       :clj [[cljs.analyzer]])))

#?(:cljs
   (do

(defonce test-result-data (atom nil))

(defn failed? [m]
  (not (zero? (+ (:fail m 0) (:error m 0)))))

(defn on-finish-listener [ky listener]
  (add-watch test-result-data ky (fn [_ _ _ m] (listener m))))

;; cljs-test-display     
(defmethod report [:cljs-test-display.core/default :end-run-tests] [m]
  (reset! test-result-data m))

(defmethod report [:cljs.test/default :end-run-tests] [m]
  (reset! test-result-data m))

(defn system-exit-on-fail []
  (on-finish-listener
   ::exit-on-fail
   #(async-result/send
     (if (failed? %)
       :figwheel.main.result/system-exit-1
       ::successly)
     )))

)
   
   :clj
   (do

(defn namespace-has-test? [ns-sym]
  (some :test (vals (get-in @cljs.env/*compiler* [:cljs.analyzer/namespaces ns-sym :defs]))))

(defn get-test-namespaces []
  (let [sources (:sources @cljs.env/*compiler*)]
    (vec (doall (filter namespace-has-test? (mapv :ns sources))))))

(defn ns? [x]
  (and (seq? x) (= (first x) 'quote)))

(defmacro run-tests
  "Differs from cljs.test/run-tests in that this macro by finds all
  the available namespaces in the local sources to test and tests
  them.

  `run-tests` runs all cljs.tests in the given namespaces; prints
  results.  Defaults to running all the namespaces in the local source
  files if no namespaces are given. 

  Will return `:figwheel.main.result/system-exit-1` if tests have
  failed. This is only useful if you are NOT running asynchronous
  tests. The value `:figwheel.main.result/system-exit-1` if returned
  from a `-main` method, will cause a process started with the
  `--main` CLI arg to exit unsuccessfully with 1.
  
  Usage examples: 

      ;; run all tests in local sources
      (run-tests) 
      ;; run tests in 'example.core-tests 
      (run-tests 'example.core-tests) ;; 
      ;; run tests in 'example.core-tests and display with cljs-test-display
      (run-tests (cljs-test-display.core/init!) 'example.core-tests)"
  ([] `(run-tests (cljs.test/empty-env)))
  ([env-or-ns]
   (if (ns? env-or-ns)
     `(run-tests (empty-env) ~env-or-ns)
     (when-let [test-nses (not-empty
                           (map #(list 'quote %)
                                (or (not-empty (get-test-namespaces))
                                    (and cljs.analyzer/*cljs-ns*
                                         [cljs.analyzer/*cljs-ns*]))))]
       `(do
          (run-tests ~env-or-ns ~@test-nses)
          (if (failed? @figwheel.main.testing/test-result-data)
            :figwheel.main.result/system-exit-1
            ::successes)))))
  ([env-or-ns & namespaces]
   `(cljs.test/run-tests ~env-or-ns ~@namespaces)))

;; this helps if you are running async tests and you have a custom
;; reporter
(defmacro run-tests-async
  "This is only supported when run in conjunction with the
  `figwheel.main`'s `--main` CLI option.
  
  This is helpful when running asynchronous tests from a main script
  on the command line. `run-tests-async` will wait for the test run to
  come to an end or time out.

  The first argument `run-tests-async` must be a `wait-time` integer
  that is the number of milliseconds the process should wait for
  completion before timing out.

  The rest of the arguments are the same as figwheel.main.testing/run-tests.

  Usage examples: 

      ;; run all tests in local sources, time out if it takes more than 5 seconds
      (run-tests-async 5000) 
      ;; run tests in 'example.core-tests with 5000 millis timeout
      (run-tests-async 5000 'example.core-tests) ;; 
      ;; run tests in 'example.core-tests and display with cljs-test-display
      (run-tests-async 5000 (cljs-test-display.core/init!) 'example.core-tests)"
  ([wait-time]
   `(run-tests-async ~wait-time (cljs.test/empty-env)))
  ([wait-time env-or-ns & namespaces]
   `(do
      (figwheel.main.testing/system-exit-on-fail)
      ~(if (ns? env-or-ns)
         `(run-tests ~env-or-ns ~@namespaces)
         `(let [reporter# (:reporter ~env-or-ns)]
            (when-not (#{:cljs.test/default :cljs-test-display.core/default} reporter#)
              (defmethod cljs.test/report [reporter# :end-run-tests] [m#]
                (reset! figwheel.main.testing/test-result-data m#)))
            (run-tests ~env-or-ns ~@namespaces)))
      [:figwheel.main.result/async-wait ~wait-time])))

))
