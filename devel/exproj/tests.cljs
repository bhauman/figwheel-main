(ns exproj.tests
  (:require
   [figwheel.main.async-result :as async-result]
   [figwheel.main.testing :refer-macros [run-tests run-tests-async]]
   [cljs.test :refer-macros [deftest is]]))

(deftest hello
  (is false))

(defn -main [& args]
  (run-tests-async 5000)
  #_(js/setTimeout #(async-result/send :figwheel.main.result/system-exit-1) 3000)
  #_[:figwheel.main.result/async-wait 1000 ::timed-out]
  #_45
  #_:figwheel.main.result/system-exit-1
  )

#_(run-tests)
