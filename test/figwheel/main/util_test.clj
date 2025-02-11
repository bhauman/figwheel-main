(ns figwheel.main.util-test
  (:require [clojure.test :refer [deftest testing is]]
            [figwheel.main.util :as sut]))

(deftest dir-on-classpath?
  (is (sut/dir-on-classpath? "src"))
  (is (sut/dir-on-classpath? "src/foo")
      "Since src dir is on classpath then anything inside it should be on classpath too")
  (is (not (sut/dir-on-classpath? "blah"))))

(deftest dir-on-current-classpath?
  (is (sut/dir-on-current-classpath? "src"))
  (is (sut/dir-on-current-classpath? "src/foo")
      "Since src dir is on classpath then anything inside it should be on classpath too")
  (is (not (sut/dir-on-current-classpath? "blah"))))
