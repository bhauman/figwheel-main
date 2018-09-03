(ns exproj.other
  (:require
   [exproj.more]
   [figwheel.main.async-result :as ar]
   [figwheel.main.testing :refer-macros [run-tests run-tests-async]]
   [cljs.test :refer-macros [deftest is async]]))

(defn hello-there [] "hello there")

(deftest hello
  (is true))

(defn -main [& args]
  #_(js/setTimeout #(ar/throw-ex (ex-info "Man o man" {})) 3000)
  #_(+ 1 2)
  #_(throw  (ex-info "Campbells soup" {}))
  #_[:figwheel.main.result/async-wait 5000]
  #_(println "ARGS:" (pr-str args))

  (run-tests-async 5000)
  #_(run-tests)
  #_(throw (ex-info "erorring oiut"  {}))
  )

