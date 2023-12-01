(ns deep.3.def-multi)

(defmulti greeting
  (fn[x] (get x "language")))

;params is not used, so we could have used [_]
(defmethod greeting "English" [params]
 "Hello!")

(defmethod greeting "French" [params]
 "Bonjour!")
