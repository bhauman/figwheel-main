(ns figwheel.main.async-result
  (:require [figwheel.repl]))

#?(:cljs (defn send [result-str]
           (let [msg {:figwheel-event "async-result" :value result-str}]
             (figwheel.repl/debug [:async-result (pr-str msg)])
             (figwheel.repl/respond-to-connection msg)))
   :clj (defn listen [prom]
          (figwheel.repl/add-listener
           ::listener
           (fn [{:keys [response] :as msg}]
             (when (= "async-result" (:figwheel-event response))
               (deliver prom (:value response)))))))
