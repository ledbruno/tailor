(ns deep.1.root)
(defn- inner-fn [arg1]
  (println "inner fn" arg1))
(defn inner-indirection []
  (inner-fn 1))
