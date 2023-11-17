(ns tailor.shears
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.java.shell :as shell]
   [clojure.set :as set]
   [clojure.string :as s]
   [tailor.helper :as helper]))

(defn index-by
  ([key-fn coll]
   (into {} (map (juxt key-fn identity) coll)))
  ([key-fn coll keys-to-keep]
   (into {} (map (juxt key-fn #(select-keys % keys-to-keep)) coll))))

(defn- var-named [var-name {var-defs :var-definitions}]
  (filter #(= (symbol var-name) (:name %)) var-defs))

(defn- from-var [var-name usages]
  (filter #(= (symbol var-name) (:from-var %)) usages))

(defn- kondo-analysis [files]
  (:analysis (clj-kondo/run!
              {:lint files
               :skip-lint true
               :config {:analysis true}})))

(defn find-var-defs
  "Returns a list of var defs with that matches the given symbol-str"
  [symbol-str file-path]
  (let  [analysis (kondo-analysis [file-path])]
    {:matches (var-named symbol-str analysis)
     :analysis analysis}))

(defn- cut [from to file-path]
  (when (and from to) (shell/sh "sed" "-n" (str from "," to "p") file-path)))

(defn- analyzed-ns [which analysis]
  (:name (which (:namespace-definitions analysis))))

(defn top-level-forms
  "Adds :top-level-forms to result, considering only the first namespace declared"
  [result]
  ;if more than one should throw an error?
  (let [analysis (:analysis result)
        var-usages (:var-usages analysis)
        ns (analyzed-ns first analysis)
        top-level (filter #(and (not (contains? % :from-var)) (= (:from %) ns)) var-usages)]
    (assoc result :top-level-forms top-level)))

(defn rows [usages-or-defs]
  (set (map :row usages-or-defs)))

(defn matching-rows
  "Adds :matching-rows to the result, which are top level rows that matches with target/matching var. WARNING: brittle approach"
  [{:keys [matches top-level-forms] :as result}]
  (assoc result :matching-rows (set/intersection (rows top-level-forms) (rows matches))))

(defn mark
  "Mark organizes matches by index in order to make easier to shear/cut the source code"
  [{:keys [matches matching-rows]}]
  ;maybe map all, not only the first matching row, other rows can have valid results
  (get (index-by :row matches) (first matching-rows)))

(defn shear [root-matches file-path]
  (:out (cut (:row root-matches) (:end-row root-matches) file-path)))

(defn shear-matches [result file]
  (-> result
      top-level-forms
      matching-rows
      mark
      (shear file)))

(defn shear-top-level
  "Returns the source code of the top level var var defined in a given file
   def-str : the name of the target var
   file    : path to the clj source code file"
  [def-str file]
  (-> (find-var-defs def-str file)
      (shear-matches file)))

(defn- usage-info [var-usage]
  (select-keys var-usage [:to :name :from :from-var :alias]))

;TODO:unit test this!
(defn ns-usages [ns-matches-map usage]
  (let [namespace-match ((:to usage) ns-matches-map)]
    (when namespace-match
      {:name (:name usage)
       :alias (:alias usage)
       :from (:from usage)
       :ns (:name namespace-match)
       :filename (:filename namespace-match)})))

(defn usages
  "Return a list of single level/direct usages of a given var, with the agregated usage-info
  :ns :alias :filename :name :from"
  [target-var file]
  (let [analysis        (kondo-analysis file)
        ns-map          (index-by :name (:namespace-definitions analysis) [:name :filename])
        usages          (map usage-info (:var-usages analysis))
        matches         (from-var target-var usages)
        ns-matches-map  (select-keys ns-map (map :to (from-var target-var usages)))
        ns-matches      (map #(ns-usages ns-matches-map %) matches)]
    (filter identity ns-matches)))

(defn shear-dependency [var-usage]
  (str (helper/ns-declare (:ns var-usage) ) (shear-top-level (:name var-usage) (:filename var-usage))))

(defn deep-shear
  [target-var target-file-path classpath-files-vec]
  (let [var-usages          (usages target-var classpath-files-vec)  ; should return a list in order make conj work properly
        target-ns           (:from (first var-usages))
        usages-src          (s/join (map shear-dependency var-usages))
        top-level-src       (str (helper/ns-declare target-ns var-usages) "\n" (shear-top-level target-var target-file-path))]
    (str usages-src "\n" top-level-src)))

(comment
  (helper/ns-declare 'my-fn (usages "my-fn"  ["./testResources/deep/1/root.clj"
                    "./testResources/deep/1/other_ns.clj"
                    "./testResources/deep/1/another.clj"
                    "./testResources/deep/1/root_dependency.clj"]))
  (spit "/tmp/result.clj" (deep-shear "my-fn" "./testResources/deep/1/root.clj"
                                      ["./testResources/deep/1/root.clj"
                                       "./testResources/deep/1/other_ns.clj"
                                       "./testResources/deep/1/another.clj"
                                       "./testResources/deep/1/root_dependency.clj"])))

