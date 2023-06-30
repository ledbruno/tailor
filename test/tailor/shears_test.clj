(ns tailor.shears-test
  (:require [clojure.test :refer [deftest is testing]]
            [tailor.shears :refer [shear-def-at-root]]))

(deftest test-shear-def-at-root
  (testing "Cuts a simple def at root"
    (is (= "(def x \"banana\")\n" (shear-def-at-root "x" "./testResources/sample.clj"))))

  (testing "Cuts a simple defn at root"
    (is (= "(defn my-fn []\n  (def x \"orange\")\n  (print \"bla\"))\n" (shear-def-at-root "my-fn" "./testResources/sample.clj")))))
