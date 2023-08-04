(ns tailor.shears
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.java.shell :as shell]
   clojure.string))

(defn index-by [key-fn coll]
  (into {} (map (juxt key-fn identity) coll)))

(defn- var-named [var-name {var-defs :var-definitions}]
  (filter #(= var-name (:name %)) var-defs))

(defn- search-var-defs
  "Returns a list of var defs with that matches the given symbol-str"
  [symbol-str file-path]
  (let  [analysis (:analysis (clj-kondo/run!
                              {:lint [file-path]
                               :config {:analysis true}}))]
    {:matches (var-named (symbol symbol-str) analysis)
     :analysis analysis}))

(defn- cut [from to file-path]
  (when (and from to) (shell/sh "sed" "-n" (str from "," to "p") file-path)))

(defn- analyzed-ns [which analysis]
  (:name (which (:namespace-definitions analysis))))

(defn- ns-var-usages [ns var-str var-usages]
  (map :name (filter #(= ns (:to %)) (filter #(= (symbol var-str) (:from-var %)) var-usages))))

(defn from-var
  "Returns the first var def on the root level, considering only the first namespace declared"
  [{analysis :analysis matches :matches} ns]
  ;if more than one should throw an error?
  (let [var-usages (:var-usages analysis)
        level-forms (filter #(and (not (:from-var %)) (= (:from %) ns)) var-usages)
        level-row-matches (clojure.set/intersection (set (map :row level-forms)) (set (map :row matches)))]
    ;maybe map all, not only the first found
    (get (index-by :row matches) (first level-row-matches))))


(defn- shear-def [var-def file]
  (:out (cut (:row var-def) (:end-row var-def) file)))

(defn- dig [ns-vars-to-dig analysis deep-count]
  (let [deep-count (- 1 deep-count)]
    (while (< 1 deep-count)
      (map str ns-vars-to-dig))))

(defn deep
  [var-str file deep-count]
  (let [{analysis :analysis :as search-result} (search-var-defs var-str file) 
        ns (analyzed-ns first analysis) 
        root-level-result (from-var search-result ns)
        ns-vars-to-dig (ns-var-usages ns var-str (:var-usages analysis))
        _ (println :to-dig ns-vars-to-dig)
        dug (dig ns-vars-to-dig analysis deep-count)
        _ (println :dug dug)
        ]
    #_(clojure.string/join (map #(shear-def % file) all-matches))
    (shear-def root-level-result file)
    ))


#_(defn var-def-at-root
  "Returns the source code of the var defined at the root level
   def-str : the name of the target var
   file    : path to the clj source code file"
  [def-str file]
  (let [def-found (from-var (search-var-defs def-str file))]
    (shear-def def-found file)))
