(ns tailor.shears
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.java.shell :as shell]
   clojure.string))

(defn index-by [key-fn coll]
  (into {} (map (juxt key-fn identity) coll)))

(defn- var-named [var-name {var-defs :var-definitions}]
  (filter #(= var-name (:name %)) var-defs))

(defn- matches
  "Returns a list of var defs with that matches the given symbol-str"
  [symbol-str kondo-analyisis]
  (var-named (symbol symbol-str) kondo-analyisis))

(defn- cut [from to file-path]
  (when (and from to) (shell/sh "sed" "-n" (str from "," to "p") file-path)))

[{:key 1
  :b 2}]

(defn- analyzed-ns [which analysis]
  (:name (which (:namespace-definitions analysis))))

(defn- ns-var-usages [ns var-str var-usages]
  (map :name (filter #(= ns (:to %)) (filter #(= (symbol var-str) (:from-var %)) var-usages))))

(defn from-var
  "Returns the first var def on the root level, considering only the first namespace declared"
  [analysis matches ns]
  ;if more than one should throw an error?
  (let [var-usages (:var-usages analysis)
        level-forms (filter #(and (not (:from-var %)) (= (:from %) ns)) var-usages)
        level-row-matches (clojure.set/intersection (set (map :row level-forms)) (set (map :row matches)))]
    ;maybe map all, not only the first found
    (get (index-by :row matches) (first level-row-matches))))

(defn- shear-def [var-def file]
  (:out (cut (:row var-def) (:end-row var-def) file)))

(defn- dig [ns-vars-to-dig kondo-analysis deep-count]
  (while (not= 0 deep-count)
    (println :really-digging)
    (map str ns-vars-to-dig)))

(defn deep
  [var-str file deep-count]
  (let [kondo-analysis (:analysis (clj-kondo/run!
                                   {:lint [file]
                                    :config {:analysis true}}))
        result-matches (matches var-str kondo-analysis)
        ns (analyzed-ns first kondo-analysis)
        root-level-result (from-var kondo-analysis result-matches ns)
        ns-vars-to-dig (ns-var-usages ns var-str (:var-usages kondo-analysis))
        _ (println :to-dig ns-vars-to-dig)
        dug (dig ns-vars-to-dig kondo-analysis deep-count)
        _ (println :dug dug)]
    #_(clojure.string/join (map #(shear-def % file) all-matches))
    (shear-def root-level-result file)))

#_(defn var-def-at-root
    "Returns the source code of the var defined at the root level
   def-str : the name of the target var
   file    : path to the clj source code file"
    [def-str file]
    (let [def-found (from-var (search-var-defs def-str file))]
      (shear-def def-found file)))
