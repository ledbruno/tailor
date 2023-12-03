(ns tailor.shears
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.java.shell :as shell]
   [clojure.string :as s]
   [tailor.helper :as helper]))

(defn index-by
  ([key-fn coll]
   (into {} (map (juxt key-fn identity) coll)))
  ([key-fn coll keys-to-keep]
   (into {} (map (juxt key-fn #(select-keys % keys-to-keep)) coll))))

(defn- def-methods [target-symbol usages]
  (filter #(and (:defmethod %)
                (= (symbol (namespace target-symbol)) (:from %))
                (= (symbol (name target-symbol)) (:name %)))
          usages))

(defn- fix-end-row [defmethod-usage grouped-matches]
  (when-let [match (first (get grouped-matches (:row defmethod-usage)))]
    (assoc match
           :end-row
           (:end-row defmethod-usage))))

(defn- correct-end-row [defmethod-matches all-usages]
  (let [grouped-matches (group-by :row defmethod-matches)
        defmethod-usages (filter #(= 'defmethod (:name %)) all-usages)
        match (map #(fix-end-row % grouped-matches) defmethod-usages)]
    match))

(defn- symbol-match [target-symbol {var-defs :var-definitions usages :var-usages}]
  (concat (filter #(and (= (symbol (namespace target-symbol)) (:ns %))
                        (= (symbol (name target-symbol)) (:name %)))
                  var-defs)
          (correct-end-row (def-methods target-symbol usages) usages)))

(defn matching-usages [s-var usages]
  (filter #(and (= (symbol (namespace s-var)) (:from %))
                (= (symbol (name s-var)) (:from-var %)))
          usages))

(defn kondo-analysis [files]
  (:analysis (clj-kondo/run!
              {:lint files
               :skip-lint true
               :config {:analysis true}})))

(def memoized-kondo (memoize kondo-analysis))

(defn top-level
  "Returns top level forms of the given namespace"
  [ns classpath]
  (->> (memoized-kondo classpath)
       :var-usages
       (filter #(and (not (contains? % :from-var)) (= (:from %) ns)))))

(defn top-level-rows
  [ns classpath]
  (set (map :row (top-level ns classpath))))

(defn matching-top-level-rows
  "Get the list of row matches that are also top level forms of a given NS"
  [ns classpath matches]
  (filter #(some #{(:row %)} (top-level-rows ns classpath)) matches))

(defn symbol-matches
  "Returns a list of var defs that matches the given target symbol"
  [target-symbol classpath]
  (->> (memoized-kondo classpath)
       (symbol-match target-symbol)))

(defn matching-top-level [target-symbol classpath]
  (->> (symbol-matches target-symbol classpath)
       (matching-top-level-rows (symbol (namespace target-symbol)) classpath)))

(defn- cut [from to file-path]
  (when (and from to) (shell/sh "sed" "-n" (str from "," to "p") file-path)))

(defn file-shear [match]
  (:out (cut (:row match) (:end-row match) (:filename match))))

(defn shear-top-level
  "Returns the sheared source code of the top level form defined on the classpath
  - target-symbol : namespaced symbol that represents the top-level form to be sheared
  - classpath : vector of files/paths source code that will be analyzed"
  [target-symbol classpath]
  (s/join (map file-shear (matching-top-level target-symbol classpath))))

(defn destination-symbol
  "Used to create target usage symbol on getting deep into usages
  In other words, it gets the usage destination-info"
  [usage]
  (symbol (name (:ns usage)) (name (:name usage))))

(defn- usage-info [var-usage]
  (select-keys var-usage [:to :name :from :from-var :alias]))

;TODO:unit test this!
(defn ns-usage-info [ns-matches-map usage]
  (let [namespace-match ((:to usage) ns-matches-map)]
    (when namespace-match
      {;destination-info
       :ns (:name namespace-match)
       :name (:name usage)
       :alias (:alias usage)
       :filename (:filename namespace-match)
       ;origin info
       :from (:from usage)
       :from-var (:from-var usage)})))

(defn inner-usages
  "Return a list of single inner usages of a given var, with the agregated ns-usage-info
  {:ns :alias :filename :name :from}
  An inner usage is a usage of a target symbol that is declared by one of the namespaces declared in provided namespace"
  [target-symbol classpath]
  (let [analysis        (memoized-kondo classpath)
        ns-map          (index-by :name (:namespace-definitions analysis) [:name :filename])
        usages          (map usage-info (:var-usages analysis))
        matches         (matching-usages target-symbol usages)
        ns-matches-map  (select-keys ns-map (map :to matches))
        ns-matches      (map #(ns-usage-info ns-matches-map %) matches)
        non-nil-matches (filter identity ns-matches)]
    non-nil-matches))

(defn- self-dependency
  [usage]
  (not= (:from usage) (:ns usage)))

(defn- deep-usages [usage classpath]
  (inner-usages (destination-symbol usage) classpath))

(defn- external-usages
  "Return a list of usages :from target symbol, excluding any clojure.core usage that will not need a (:require)"
  [target-symbol classpath]
  (let [analysis               (memoized-kondo classpath)
        usages                 (map usage-info (:var-usages analysis))
        matches                (matching-usages target-symbol usages)
        without-clojure-core   (filter #(not= 'clojure.core (:to %)) matches)]
    (map #(assoc % :ns (:to %)) without-clojure-core)))

(defn- dependencies
  "Returns inner + external usages, so the (:require ) part can be properly create for inner and exernal deps"
  [symbol classpath]
  (->> (inner-usages symbol classpath)
       (concat (external-usages symbol classpath))
       (filter self-dependency)))

(defn append-with [symbols classpath header-src]
  (str header-src (s/join (map #(shear-top-level % classpath) symbols))))

(defn- source [[namespace symbols] classpath]
  (->> (map #(dependencies % classpath) symbols)
       flatten
       distinct
       (helper/ns-declare namespace)
       (append-with symbols classpath)))

(defn- shear-ns-symbols
  "Retuns the source code from the provided namespaces and symbols maps (ns-map) and classpath
  e.g: ns-map -> {   my-ns    [my-ns/symbol1,my-ns/symbol2]
                     other-ns [other-ns/symbol3]}"
  [ns-map classpath]
  (s/join (map #(source % classpath) ns-map)))

(defn deep
  [parent-usages classpath depth]
  (let [child-usages (map #(deep-usages % classpath) parent-usages)]
    (if (or (= 0 depth) (empty? child-usages))
      child-usages
      (distinct (flatten (conj (map #(deep % classpath (- depth 1)) child-usages) parent-usages))))))

(defn- usages->ns-map
  "Returns a ns-map from usages
   ns-map -> {   my-ns    [my-ns/symbol1,my-ns/symbol2]
                 other-ns [other-ns/symbol3]}"
  [all-matching-usages]
  (->> all-matching-usages
       (group-by :ns)
       (map (fn [[ns usages]]
              {ns (distinct (map #(symbol (name (:ns %)) (name (:name %))) usages))}))
       (into {})))

(defn shear
  [all-matching-usages target-symbol classpath]
  (-> (usages->ns-map all-matching-usages)
      (update (symbol (namespace target-symbol)) #(conj (into [] %) target-symbol))
      (shear-ns-symbols classpath)))

(defonce max-depth 10)
(defn deep-shear
  ([target-symbol classpath depth]
   (-> (inner-usages target-symbol classpath)
       (deep classpath depth)
       reverse
       (shear target-symbol classpath)))
  ([target-symbol classpath]
   (deep-shear target-symbol classpath max-depth)))
