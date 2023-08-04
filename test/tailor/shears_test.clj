(ns tailor.shears-test
  (:require [clojure.test :refer [deftest is testing]]
            [tailor.shears :refer [#_var-def-at-root deep]]))

(deftest test-deep-level-0
  (testing "Cuts a simple def at root"
    (is (= "(def x \"banana\")\n" (deep "x" "./testResources/sample.clj" 0))))

  (testing "Cuts another simple defn at root"
    (is (= "(defn my-fn []\n  (def x \"orange\")\n  (print \"bla\"))\n" (deep "my-fn" "./testResources/sample.clj" 0)))))

#_(deftest test-deep-getting-into-level-1
  (testing "Cuts one level deep"
    (is (= "(defn my-fn []\n(def x \"orange\")\n(print \"bla\"))\n(defn internal-call[]\n  (println \"bla\")\n  (my-fn))\n" (deep "internal-call" "./testResources/sample.clj" 1)))))


#_(deftest test-shear-def-at-root-deep-n
  (testing "Cuts a simple defn at root"
    (is (= "(defn internal-call[]\n  (println \"bla\")\n  (my-fn))\n" (var-def-at-root "internal-call" "./testResources/sample.clj")))))


