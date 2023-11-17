(ns deep.1.other-ns 
  (:require
   [deep.1.root :as unused-on-purpouse]
   [deep.1.another :as another]))

(defn call-fn [arg1]
  (println arg1)
  (another/child-call 1))

(defn one-to-many [arg1]
  (another/fn1 1)
  (another/fn2 2))
