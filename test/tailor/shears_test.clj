(ns tailor.shears-test
  (:require [clojure.test :refer [deftest is testing]]
            [tailor.shears :refer [shear-def-at-root]]))

(deftest shear-def-at-root
  (testing "Cuts a simple def "
    (is (= "(def x \"banana\")\n" (shear-def-at-root "x" "./testResources/sample.clj"))))

  (testing "Cuts a simple defn"
    (is (= "(defn my-fn []\n  (print \"bla\"))\n" (shear-def-at-root "my-fn" "./testResources/sample.clj")))))
