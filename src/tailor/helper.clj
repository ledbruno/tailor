(ns tailor.helper
  (:require
   [clojure.string :as s]))

(defn require-str
  [usage]
  (str "[" (name (:ns usage)) (if (:alias usage) (str " :as " (name (:alias usage))) "") "]"))

(defn requires
  [usages]
  (let [dedup-usages  (set (map #(select-keys % [:ns :alias]) usages))]
    (str "(:require " (s/join "\n" (map require-str dedup-usages)) ")")))

(defn ns-declare
  ([ns] 
   (str "(ns " (name ns) ")\n"))
  ([ns usages]
   (if (empty? usages)
     (ns-declare ns)
     (str "(ns " (name ns) "\n" (requires usages) ")\n"))))

(comment
  (ns-declare 'main-ns)
  (ns-declare 'main-ns  [{:ns 'my-ns :alias 'zambas}  {:ns 'my-ns}])

  (ns-declare 'main-ns  [])
  (require-str {:ns 'my-ns :alias 'zambas}) ; "[my-ns :as zambas]"
  (require-str {:ns 'my-ns}) ; "[my-ns]"
  (requires [{:ns 'my-ns :alias 'zambas}  {:ns 'my-ns}])
  (requires [{:ns 'a-ns :alias 'a}  {:name :x :ns 'a-ns :alias 'a}]); duplicated usage
  )


