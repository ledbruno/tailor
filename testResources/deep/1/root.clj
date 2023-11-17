(ns deep.1.root
  (:require
   [deep.1.another :as another]
   [deep.1.other-ns :as other-ns]
   [deep.1.root-dependency :as root-dependency]))

(defn my-fn
  []
  (other-ns/call-fn 1)
  (println)
  (another/another-fn 1 2 3 4)
  (root-dependency/just-for-root)
  (root-dependency/another-just))

(defn root-to-other []
  (other-ns/call-fn 1))
