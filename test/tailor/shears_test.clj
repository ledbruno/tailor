(ns tailor.shears-test
  (:require [clojure.test :refer [deftest is testing]]
            [tailor.shears :refer [shear-top-level deep-shear usages deep matching-usages destination-symbol]]))

(deftest test-usage-symbol
  (is (= 'my-ns/my-fn (destination-symbol {:ns 'my-ns :name 'my-fn}))))

(deftest test-matching-usages
  (is (= '({:from-var my-fn
            :from target-ns})
         (matching-usages 'target-ns/my-fn
                          '({:from-var my-fn
                             :from other-ns}
                            {:from-var my-fn
                             :from target-ns})))))

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

(def ^:private classpath-2
  ["./testResources/deep/2/top_level.clj"
   "./testResources/deep/2/deep_1.clj"
   "./testResources/deep/2/deep_2.clj"
   "./testResources/deep/2/deep_3.clj"])

(deftest test-usages
  (testing "Empty usages"
    (is (empty? (usages 'deep.1.root-dependency/just-for-root classpath-1))))
  (testing "Some usages"
    (is (= '({:name call-fn,
              :alias other-ns,
              :from deep.1.root,
              :ns deep.1.other-ns,
              :from-var my-fn
              :filename "./testResources/deep/1/other_ns.clj"}
             {:name another-fn,
              :alias another,
              :from deep.1.root,
              :from-var my-fn
              :ns deep.1.another,
              :filename "./testResources/deep/1/another.clj"}
             {:name just-for-root,
              :alias root-dependency,
              :from deep.1.root,
              :from-var my-fn
              :ns deep.1.root-dependency,
              :filename "./testResources/deep/1/root_dependency.clj"}
             {:name another-just,
              :alias root-dependency,
              :from deep.1.root,
              :from-var my-fn
              :ns deep.1.root-dependency,
              :filename "./testResources/deep/1/root_dependency.clj"})
           (usages 'deep.1.root/my-fn classpath-1))))

  (testing "Brings by namespace (handles conflicted var names betwen ns)"
    (is (= '({:alias deep-1,
              :filename "./testResources/deep/2/deep_1.clj",
              :from deep.2.top-level,
              :name run,
              :from-var run
              :ns deep.2.deep-1}) (usages 'deep.2.top-level/run
                                          classpath-2)))))

(deftest test-deep
  (testing "One level deep"
    (is (= '({:alias other-ns,
              :filename "./testResources/deep/1/other_ns.clj",
              :from deep.1.root,
              :from-var my-fn
              :name call-fn,
              :ns deep.1.other-ns}
             {:name child-call,
              :alias another,
              :from-var call-fn
              :from deep.1.other-ns,
              :ns deep.1.another,
              :filename "./testResources/deep/1/another.clj"})
           (deep '({:alias other-ns,
                    :filename "./testResources/deep/1/other_ns.clj",
                    :from deep.1.root,
                    :from-var my-fn
                    :name call-fn,
                    :ns deep.1.other-ns})
                 classpath-1))))

  (testing "One level deep"
      (is (= '({:alias other-ns,
                :filename "./testResources/deep/1/other_ns.clj",
                :from deep.1.root,
                :name call-fn,
                :ns deep.1.other-ns}
               {:name child-call,
                :from-var call-fn
                :alias another,
                :from deep.1.other-ns,
                :ns deep.1.another,
                :filename "./testResources/deep/1/another.clj"})
             (deep '({:alias other-ns,
                      :filename "./testResources/deep/1/other_ns.clj",
                      :from deep.1.root,
                      :name call-fn,
                      :ns deep.1.other-ns})
                   classpath-1)))))

(deftest test-deep-shear
  (testing "Shallow defn"
    (is (= "\n(ns deep.1.root-dependency)\n(defn just-for-root [])\n" (deep-shear 'deep.1.root-dependency/just-for-root  "./testResources/deep/1/root_dependency.clj"
                                                                                  classpath-1))))

  (testing "1 level depth defn, no requires from the last/bottom usage"
      (is  (= (slurp "./testResources/expected/call-fn.clj")
              (deep-shear 'deep.1.other-ns/call-fn  "./testResources/deep/1/other_ns.clj"
                          classpath-1))))

  (testing "1 level depth defn, WITH requires from the last/bottom usage"
    (is  (= (slurp "./testResources/expected/with-require.clj")
            (deep-shear 'deep.1.root/root-to-other  "./testResources/deep/1/root.clj"
                        classpath-1))))

;TODO improve this test to check actual order...
  #_(testing "Evaluable code, respect dependency order"
      (is  (= (slurp "./testResources/expected/my-fn.clj")
              (deep-shear 'deep.1.root/my-fn  "./testResources/deep/1/root.clj"
                          classpath-1))))

  #_(testing "1 level depth"
      (is  (= (slurp "./testResources/expected/level_1.clj")
              (deep-shear 'deep.2.top-level/run "./testResources/deep/2/top_level.clj"
                          classpath-2)))))

(comment
  (require '[clojure.tools.namespace.repl :refer [refresh]])
  (refresh))
