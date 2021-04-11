(ns figwheel.main.system-exit
  (:require
   [clojure.string :as string]
   [figwheel.repl]
   #?@(:cljs [[figwheel.tools.heads-up :as heads-up]]
       :clj [[clojure.java.io :as io]
             [clojure.java.shell :as shell]])))

#?(:cljs
   (defn exit-with-status [status]
     (let [msg {:figwheel-event "system-exit"
                :value status}]
       (figwheel.repl/debug [:exit-with-status (pr-str msg)])
       (figwheel.repl/respond-to-connection msg)))
   :clj
   (defn setup []
     (figwheel.repl/add-listener
      ::system-exit
      (fn [{:keys [response] :as msg}]
        (when (= "system-exit" (:figwheel-event response))
          (let [value (:value response 0)
                value (if (integer? value) value 0)]
            (shutdown-agents)
            (System/exit value)))))))
