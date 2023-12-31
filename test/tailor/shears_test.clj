(ns tailor.shears-test
  (:require [clojure.test :refer [deftest is testing]]
            [tailor.shears :refer [shear-top-level deep-shear inner-usages deep matching-usages destination-symbol symbol-matches]]))

(deftest test-symbol-matches
  (testing "Defmulti/defmethod matches" (is (= '({:end-row 4,
            :name-end-col 19,
            :name-end-row 3,
            :name-row 3,
            :ns deep.3.def-multi,
            :name greeting,
            :defined-by clojure.core/defmulti,
            :filename "./testResources/deep/3/def_multi.clj",
            :col 1,
            :name-col 11,
            :defined-by->lint-as clojure.core/defmulti,
            :end-col 30,
            :row 3}
           {:end-row 8,
            :name-end-col 20,
            :name-end-row 7,
            :name-row 7,
            :name greeting,
            :defmethod true,
            :dispatch-val-str "\"English\"",
            :filename "./testResources/deep/3/def_multi.clj",
            :from deep.3.def-multi,
            :col 12,
            :name-col 12,
            :end-col 20,
            :row 7,
            :to deep.3.def-multi}
           {:end-row 11,
            :name-end-col 20,
            :name-end-row 10,
            :name-row 10,
            :name greeting,
            :defmethod true,
            :dispatch-val-str "\"French\"",
            :filename "./testResources/deep/3/def_multi.clj",
            :from deep.3.def-multi,
            :col 12,
            :name-col 12,
            :end-col 20,
            :row 10,
            :to deep.3.def-multi}) (symbol-matches 'deep.3.def-multi/greeting
                                                   ["./testResources/deep/3/def_multi.clj"])))))
(deftest test-usage-symbol
  (is (= 'my-ns/my-fn (destination-symbol {:ns 'my-ns :name 'my-fn}))))

(deftest test-matching-usages
  (testing "Simple match" (is (= '({:from-var my-fn
                                    :from target-ns})
                                 (matching-usages 'target-ns/my-fn
                                                  '({:from-var my-fn
                                                     :from other-ns}
                                                    {:from-var my-fn
                                                     :from target-ns}))))))

(deftest test-shear-top-level
  (testing "Shears a simple def top level"
    (shear-top-level 'sample/x ["./testResources/sample.clj"])
    (is (= "(def x \"banana\")\n" (shear-top-level 'sample/x ["./testResources/sample.clj"]))))

  (testing "Shears a simple defn top level"
    (is (= "(defn my-fn []\n  (def x \"orange\")\n  (print \"bla\"))\n" (shear-top-level 'sample/my-fn ["./testResources/sample.clj"])))))

(def ^:private classpath-1
  ["./testResources/deep/1/root.clj"
   "./testResources/deep/1/other_ns.clj"
   "./testResources/deep/1/big_internal.clj"
   "./testResources/deep/1/another.clj"
   "./testResources/deep/1/root_dependency.clj"])

(def ^:private classpath-2
  ["./testResources/deep/2/top_level.clj"
   "./testResources/deep/2/deep_1.clj"
   "./testResources/deep/2/deep_2.clj"
   "./testResources/deep/2/deep_3.clj"])

(deftest test-inner-usages
  (testing "Empty usages"
    (is (empty? (inner-usages 'deep.1.root-dependency/just-for-root classpath-1))))
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
           (inner-usages 'deep.1.root/my-fn classpath-1))))

  (testing "Brings by namespace (handles conflicted var names betwen ns)"
    (is (= '({:alias deep-1,
              :filename "./testResources/deep/2/deep_1.clj",
              :from deep.2.top-level,
              :name run,
              :from-var run
              :ns deep.2.deep-1}) (inner-usages 'deep.2.top-level/run
                                          classpath-2)))))

(deftest test-deep-usages
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
              :ns deep.1.another
              :filename "./testResources/deep/1/another.clj"})
           (deep '({:alias other-ns,
                    :filename "./testResources/deep/1/other_ns.clj",
                    :from deep.1.root,
                    :from-var my-fn
                    :name call-fn,
                    :ns deep.1.other-ns})
                 classpath-1
                 10)))

    (testing "Deep until bottom"
      (is (= '({:ns deep.2.deep-1,
                :name run,
                :alias deep-1,
                :filename "./testResources/deep/2/deep_1.clj",
                :from deep.2.top-level,
                :from-var run}
               {:ns deep.2.deep-2,
                :name run,
                :alias deep-2,
                :filename "./testResources/deep/2/deep_2.clj",
                :from deep.2.deep-1,
                :from-var run}
               {:ns deep.2.deep-3,
                :name run,
                :alias deep-3,
                :filename "./testResources/deep/2/deep_3.clj",
                :from deep.2.deep-2,
                :from-var run})
             (deep '({:ns deep.2.deep-1,
                      :name run,
                      :alias deep-1,
                      :filename "./testResources/deep/2/deep_1.clj",
                      :from deep.2.top-level,
                      :from-var run})
                   classpath-2
                   10))))))

(deftest test-deep-shear
  (testing "Shallow defn"
    (is (= "(ns deep.1.root-dependency)\n(defn just-for-root [])\n" (deep-shear 'deep.1.root-dependency/just-for-root
                                                                                classpath-1))))

  (testing "Inner indirection should not add (:requires) ##TODO: also add single ns validation"
    (is (= (slurp "./testResources/expected/inner_indirection.clj")
           (deep-shear 'deep.1.root/inner-indirection
                       classpath-1))))

  (testing "1 level depth defn, no requires from the last/bottom usage"
    (is  (= (slurp "./testResources/expected/call-fn.clj")
            (deep-shear 'deep.1.other-ns/call-fn
                        classpath-1))))

  (testing "1 level depth defn, WITH requires from the last/bottom usage"
    (is  (= (slurp "./testResources/expected/with-require.clj")
            (deep-shear 'deep.1.root/root-to-other
                        classpath-1))))
  (testing "3 levels depth"
    (is  (= (slurp "./testResources/expected/level_3.clj")
            (deep-shear 'deep.2.top-level/run
                        classpath-2))))
  (testing "2 levels depth"
    (is  (= (slurp "./testResources/expected/level_2.clj")
            (deep-shear 'deep.2.top-level/run
                        classpath-2 2))))
  (testing "Defmethod and Defmulti"
    (is (= (slurp "./testResources/expected/def_multi.clj") 
           (deep-shear 'deep.3.def-multi/greeting ["./testResources/deep/3/def_multi.clj"]))))

  (testing "Big and complex Internal indirection flow"
    (is  (= (slurp "./testResources/expected/big_inner_redirection.clj")
            (deep-shear 'deep.1.big-internal/starting
                        classpath-1)))))
(deftest shear-real-code
  (is (= (slurp "./testResources/expected/server.clj")
         (deep-shear 'example.server/start ["./testResources/deep/3/server.clj"]))))

(comment
  (require '[clojure.tools.namespace.repl :refer [refresh]])
  (refresh))
