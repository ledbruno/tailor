(ns deep.2.deep-3)
(defn run[]
  (println "end of road"))
(ns deep.2.deep-2
(:require [deep.2.deep-3 :as deep-3]))
(defn run[]
  (deep-3/run))
(ns deep.2.deep-1
(:require [deep.2.deep-2 :as deep-2]))
(defn run[]
  (deep-2/run))
(ns deep.2.top-level
(:require [deep.2.deep-1 :as deep-1]))
(defn run[]
  (deep-1/run))
