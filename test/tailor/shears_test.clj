(ns tailor.shears-test
  (:require [clojure.test :refer [deftest is testing]]
            [tailor.shears :refer [shear-top-level]]))

(deftest test-shear-top-level
  (testing "Shears a simple def top level"
    (is (= "(def x \"banana\")\n" (shear-top-level "x" "./testResources/sample.clj"))))

  (testing "Shears a simple defn top level"
    (is (= "(defn my-fn []\n  (def x \"orange\")\n  (print \"bla\"))\n" (shear-top-level "my-fn" "./testResources/sample.clj")))))
