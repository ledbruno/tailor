(ns deep.1.other-ns)
(defn call-fn [arg1]
  (println arg1)
  (another/child-call 1))
(ns deep.1.another)
(defn another-fn [a b c d]
  (println a b c d))
(ns deep.1.root-dependency)
(defn just-for-root [])
(ns deep.1.root-dependency)
(defn another-just [])
(ns deep.1.another)
(defn child-call [arg1]
  (println arg1))

(ns deep.1.root
(:require [deep.1.other-ns :as other-ns]
[deep.1.another :as another]
[deep.1.root-dependency :as root-dependency]))
(defn my-fn
  []
  (other-ns/call-fn 1)
  (println)
  (another/another-fn 1 2 3 4)
  (root-dependency/just-for-root)
  (root-dependency/another-just))
