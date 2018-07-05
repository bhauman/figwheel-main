(ns figwheel.main.testing
  (:require
   [clojure.string :as string]
   #?@(:cljs [[goog.log :as glog]
              [cljs.test]]
       :clj [[figwheel.main.logging :as log]
             [cljs.env]]))
  #?(:cljs (:require-macros
            [figwheel.main.testing]
            [cljs.test])))

#?(:cljs
   (do




     )
   :clj
   (do

(defn namespace-has-test? [ns-sym]
  (some :test (vals (get-in @cljs.env/*compiler* [:cljs.analyzer/namespaces ns-sym :defs]))))

(defn get-test-namespaces []
  (let [sources (:sources @cljs.env/*compiler*)]
    (vec (doall (filter namespace-has-test? (mapv :ns sources))))))

(defmacro test-local-namespaces []
  (when-let [test-nses (not-empty (get-test-namespaces))]
    `(cljs.test/run-tests ~@(map #(list 'quote %) test-nses))))


))
