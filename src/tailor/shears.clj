(ns tailor.shears
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.java.shell :as shell]
   [clojure.set :as set]))

(defn index-by [key-fn coll]
  (into {} (map (juxt key-fn identity) coll)))

(defn- var-named [var-name {var-defs :var-definitions}]
  (filter #(= var-name (:name %)) var-defs))

(defn find-var-defs
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

(defn top-level-forms
  "Returns all top level forms, considering only the first namespace declared"
  [analysis]
  ;if more than one should throw an error?
  (let [ns (analyzed-ns first analysis)]
    (filter #(and (not (contains? % :from-var)) (= (:from %) ns)) (:var-usages analysis))))

(defn rows [usages-or-defs]
  (set (map :row usages-or-defs)))

(defn row-match
  "Find vars that are declared on same row as top level forms. WARNING: brittle approach"
  [matches top-level-forms]
  (set/intersection (rows top-level-forms) (rows matches)))

(defn mark [matches top-level-form-matches]
  ;maybe map all, not only the first match
  (get (index-by :row matches) (first top-level-form-matches)))

(defn shear [root-matches file-path]
  (:out (cut (:row root-matches) (:end-row root-matches) file-path)))

(defn var-def-at-root
  "Returns the source code of the var defined at the root level
   def-str : the name of the target var
   file    : path to the clj source code file"
  [def-str file]
  (let [result  (find-var-defs def-str file)
        matches (:matches result)
        def-found (mark matches (row-match matches (top-level-forms (:analysis result))))]
    (shear def-found file)))

(comment
  (defn cleanup [var-usage]
    (assoc (select-keys var-usage [:ns :row :name :end-row]))))
