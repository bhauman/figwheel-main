(ns exproj.tests
  (:require
   [cljs.test :refer-macros [deftest is run-tests]]))

(deftest hello
  (is false))

(run-tests)
