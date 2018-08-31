(ns figwheel.main.async-result
  (:require [figwheel.repl]))

#?(:cljs
   (do
     (defn send [result-str]
       (let [msg {:figwheel-event "async-result" :value result-str}]
         (figwheel.repl/debug [:async-send (pr-str msg)])
         (figwheel.repl/respond-to-connection msg)))
     (defn throw-ex [ex]
       (when ex
         (let [msg {:figwheel-event "async-result"
                    :value
                    {:type ::exception
                     :status :exception
                     :value (try (pr-str ex)
                                 (catch js/Error e
                                   "Error"))
                     :message (.-message ex)
                     :ex-data (ex-data ex)
                     :ua-product (figwheel.repl/get-ua-product)
                     :stacktrace (.-stack ex)}}]
           (figwheel.repl/debug [:async-throw (pr-str msg)])
           (figwheel.repl/respond-to-connection msg))))
     )
   :clj (defn listen [prom]
          (figwheel.repl/add-listener
           ::listener
           (fn [{:keys [response] :as msg}]
             (when (= "async-result" (:figwheel-event response))
               (deliver prom (:value response)))))))
