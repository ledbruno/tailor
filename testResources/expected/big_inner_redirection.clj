(ns deep.1.big-internal)
(defn- other-map [element n]
  (println element n))
(ns deep.1.big-internal)
(defn other-thing [n]
  (println n)
  (map #(other-map % n) [99 99 99]))
(ns deep.1.big-internal)
(defn call-fn [arg1]
  (println arg1)
  (map other-thing [1 2 3]))

(ns deep.1.big-internal)
(defn starting []
  (call-fn 1))
