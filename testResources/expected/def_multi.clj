(ns deep.3.def-multi)
(defmulti greeting
  (fn[x] (get x "language")))
(defmethod greeting "English" [params]
(defmethod greeting "French" [params]
