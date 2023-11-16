(ns tailor.shears-test
  (:require [clojure.test :refer [deftest is testing]]
            [tailor.shears :refer [shear-top-level deep-shear]]))

(deftest test-shear-top-level
  (testing "Shears a simple def top level"
    (is (= "(def x \"banana\")\n" (shear-top-level "x" "./testResources/sample.clj"))))

  (testing "Shears a simple defn top level"
    (is (= "(defn my-fn []\n  (def x \"orange\")\n  (print \"bla\"))\n" (shear-top-level "my-fn" "./testResources/sample.clj")))))

(def ^:private classpath-1
  ["./testResources/deep/1/root.clj"
   "./testResources/deep/1/other_ns.clj"
   "./testResources/deep/1/another.clj"
   "./testResources/deep/1/root_dependency.clj"])

(deftest test-deep-shear
  (testing "Shallow defn"
    (is (= "(defn just-for-root [])\n" (deep-shear "just-for-root"  "./testResources/deep/1/root_dependency.clj"
                                                   classpath-1))))

  (testing "1 level depth defn"
    (is  (= (str "(defn call-fn [arg1]\n"
                 "  (println arg1)\n"
                 "  (another/child-call 1))\n\n"
                 "(defn child-call [arg1]\n"
                 "  (println arg1))\n")
            (deep-shear "call-fn"  "./testResources/deep/1/other_ns.clj"
                        classpath-1))))

  #_(testing "2 level depth defn"
    (is  (= (str "(defn root-to-other []\n"
                 "  (other-ns/call-fn 1))\n\n"
                 "(defn call-fn [arg1]\n"
                 "  (println arg1)\n"
                 "  (another/child-call 1))\n\n"
                 "(defn child-call [arg1]\n"
                 "  (println arg1))\n")
            (deep-shear "root-to-other"  "./testResources/deep/1/root.clj"
                        classpath-1)))))
