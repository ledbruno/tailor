(ns sample)

(defn my-fn []
  (def x "orange")
  (print "bla"))

(defn internal-call[]
  (println "bla")
  (my-fn))

(def x "banana")
