(ns tailor.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [tailor.core :refer [prompt]]))
,
(deftest prompt-test
  (testing "Some simple prompting"
    (is (= (str "You are a code generator.\nThis are some code snippets, that you can use as context on your task. \n"
           "The target code:\n'''(def x 10) (def y 11)'''\nThe replacement value:\n'''{:my-key 123}'''\n\n"
           "Your task is to change the target code, replacing all variable values to the replacement value provided\n"
           "Your output should be just clojure code. Do not comment your ideas of how you get to the result, just the resultant clojure expression")
           (prompt  {:briefing "You are a code generator."
                     :snippets [{:code "(def x 10) (def y 11)"
                                 :description "The target code:"}
                                {:code "{:my-key 123}"
                                 :description "The replacement value:"}]
                     :task "Your task is to change the target code, replacing all variable values to the replacement value provided"
                     :restrictions "Your output should be just clojure code. Do not comment your ideas of how you get to the result, just the resultant clojure expression"})))))
