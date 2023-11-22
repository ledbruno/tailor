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

(defn- symbol-match [target-symbol {var-defs :var-definitions}]
  (filter #(and (= (symbol (namespace target-symbol)) (:ns %))
                (= (symbol (name target-symbol)) (:name %)))
          var-defs))

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
  target-symbol : namespaced symbol that is the shear target"
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

(defn usages
  "Return a list of single level/direct usages of a given var, with the agregated ns-usage-info
  :ns :alias :filename :name :from"
  [target-symbol classpath]
  (let [analysis        (memoized-kondo classpath)
        ns-map          (index-by :name (:namespace-definitions analysis) [:name :filename])
        usages          (map usage-info (:var-usages analysis))
        matches         (matching-usages target-symbol usages)
        ns-matches-map  (select-keys ns-map (map :to matches))
        ns-matches      (map #(ns-usage-info ns-matches-map %) matches)
        non-nil-matches (filter identity ns-matches)]
    non-nil-matches))

(defn- external-usage
  [usage]
  (not= (:from usage) (:ns usage)))

(defn shear-dependency [dep-usage all-usages classpath]
  (let [target-symbol (destination-symbol dep-usage)
        deps (matching-usages target-symbol all-usages)
        external-deps (filter external-usage deps)]
    (str (helper/ns-declare (:ns dep-usage) external-deps) (shear-top-level target-symbol classpath))))

(defn- deep-usages [usage classpath]
  (usages (destination-symbol usage) classpath))

(defn deep
  [parent-usages classpath depth]
  (let [child-usages (map #(deep-usages % classpath) parent-usages)]
    (if (or (= 0 depth) (empty? child-usages))
      child-usages
      (flatten (conj (map #(deep % classpath (- depth 1)) child-usages) parent-usages)))))

(defn- shear
  [var-usages target-symbol classpath]
  (let [target-ns           (namespace target-symbol)
        usages-src          (s/join (map #(shear-dependency % var-usages classpath) var-usages))
        top-level-src       (str (helper/ns-declare target-ns (filter external-usage (matching-usages target-symbol var-usages)))
                                 (shear-top-level target-symbol classpath))]
    (str usages-src "\n" top-level-src)))

(defonce max-depth 10)
(defn deep-shear
  ([target-symbol classpath depth]
   (-> (usages target-symbol classpath)
       (deep classpath depth)
       reverse
       (shear target-symbol classpath)))
  ([target-symbol classpath]
   (deep-shear target-symbol classpath max-depth)))
