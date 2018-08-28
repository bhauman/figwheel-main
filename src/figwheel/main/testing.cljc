(ns figwheel.main.testing
  (:require
   [clojure.string :as string]
   #?@(:cljs [[figwheel.main.async-result :as async-result]
              [goog.dom :as gdom]
              [cljs.test :refer [report]]]
       :clj [[cljs.analyzer]
             [figwheel.main.logging :as log]
             [figwheel.main.util :as fw-util]
             [clojure.java.io :as io]])))

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
       ::success))))

(defn no-auto-tests-display-message [app-id]
  (if (nil? goog/global.document)
    (js/console.log "No tests yet")
    (let [el (gdom/getElement app-id)]
      (gdom/removeChildren el)
      (gdom/append
       el
       (gdom/createDom
        "div" #js {:style (str "margin: 100px auto; border: 1px solid #d0d0d0; color: #444; width: 400px; padding: 2em;"
                               "border-radius: 5px; font-family:sans-serif; background-color: #fdfce2")}
      (gdom/createDom
       "h1" #js {:style ""}
       (gdom/createTextNode "No tests to run yet."))
      (gdom/createDom
       "p" #js {:style ""}
       (gdom/createTextNode
        (str "Figwheel auto-testing isn't able to pick up your testing namespaces until "
             "after you have hot reloaded a source file at least once.")))
      (gdom/createDom
       "p" #js {:style ""}
       (gdom/createTextNode "For example: Try to open a source file with tests in it and save it."))
      (gdom/createDom
       "p" #js {:style ""}
       (gdom/createTextNode "Or perhaps you haven't written any tests yet?"))))))
  
)

)
   
   :clj
   (do

;; should only do this once the first time you cant find
;; tests with the compiler env     
(defn get-test-namespaces-no-compiler-env [source-dirs]
  (->> source-dirs
       (mapcat file-seq)
       (filter #(or (.endsWith (str %) ".cljs")
                    (.endsWith (str %) ".cljc")))
       (filter #(let [content (slurp %)]
                  (.contains content "(deftest ")))
       )

  
  )

     
;; speed this up by simply checking for cljs.test in the requires first
;; or we could consider meta data on the ns as the indicator
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
       `(run-tests ~env-or-ns ~@test-nses))))
  ([env-or-ns & namespaces]
   `(do (cljs.test/run-tests ~env-or-ns ~@namespaces)
        (if (failed? @figwheel.main.testing/test-result-data)
          :figwheel.main.result/system-exit-1
          ::success))))

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


;; we need to generate a testing file

(defn genned-test-ns [build-id]
  (symbol (format "figwheel.main.generated.%s-auto-test-runner" (name build-id))))

(defn testing-file-content [{:keys [test-ns libs run-tests-args]
                             :or {test-ns (genned-test-ns "unknown")
                                  libs []
                                  run-tests-args []}}]
  (format "(ns %s
  (:require [cljs.test :refer-macros [run-tests]]
            %s))

  (run-tests %s)"
          (str test-ns)
          (string/join " " (map pr-str libs))
          (string/join " " (map pr-str run-tests-args))))

(defn no-namespaces-content [{:keys [test-ns app-id] :or {test-ns (genned-test-ns "unknown")
                                                          app-id "app-auto-testing"}}]
  (format "(ns %s
  (:require [figwheel.main.testing]))

  (figwheel.main.testing/no-auto-tests-display-message %s)"
          (pr-str test-ns)
          (pr-str app-id)))

#_(testing-file-content {:test-ns (genned-test-ns "dev")
                         :libs '[[cljs-test-display.core]]
                         :run-tests-args '[(cljs-test-display.core/init!)]})

(defn test-file-output-to [output-dir]
  (str (io/file output-dir "generated-input-files" "gen_test_runner.cljs")))

#_(test-file-output-to "target/cljs-out/dev")


;; todo generate a different file when there are no namespaces to test
;; or create a macro to help in this case
(defn pre-hook [output-to test-display?]
  (fn [cfg]
    (let [namespaces (get-test-namespaces)]
      (let [id (-> cfg :figwheel.main/build :id)
            args (cond-> {:test-ns (genned-test-ns id)}
                   test-display?
                   (assoc :libs (cons '[cljs-test-display.core]
                                      (map vector namespaces))
                          :run-tests-args
                          (cons
                           '(cljs-test-display.core/init! "app-auto-testing")
                           (map #(list 'quote %) namespaces))))
            content (if (not-empty namespaces)
                      (testing-file-content args)
                      (no-namespaces-content (assoc args :app-id "app-auto-testing")))]
        (log/debug "PRE-HOOK")
        (log/debug output-to)
        (log/debug content)
        (let [f (io/file output-to)]
          (io/make-parents f)
          (spit f content)
          (log/debug (.isFile f)))
        
        ))))


(defn add-extra-main [cfg]
  (let [id  (-> cfg :figwheel.main/build :id)
        ns' (genned-test-ns id)]
    (assoc-in cfg [:figwheel.main/config :extra-main-files :auto-testing] {:main ns'})))

(defn add-auto-testing? [{:keys [options ::config] :as cfg}]
  (or (and
       (= :none (get options :optimizations :none))
       (get config :auto-testing true))
      (get config :auto-testing false)))

(defn cljs-test-display? [cfg]
  (nil? (get-in cfg [:options :target])))

(defn add-file-gen [{:keys [options] :as cfg}]
  (let [output-to (test-file-output-to (:output-dir options))
        test-display? (cljs-test-display? cfg)]
    (-> cfg
        ;; add prehook
        (update :figwheel.main/pre-build-hooks conj (pre-hook output-to test-display?))
        ;; add build input
        (update-in [:figwheel.main/config :figwheel.main/build-inputs] conj output-to))))

(defn plugin [cfg]
  (cond-> cfg
    (add-auto-testing? cfg)
    (->
     add-extra-main
     add-file-gen)))

))
