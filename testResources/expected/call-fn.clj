(ns deep.1.another)
(defn child-call [arg1]
  (println arg1))
(ns deep.1.other-ns
(:require [deep.1.another :as another]))
(defn call-fn [arg1]
  (println arg1)
  (another/child-call 1))
