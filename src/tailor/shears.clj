(ns tailor.shears
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.java.shell :as shell]
   [clojure.set :as set]
   [clojure.string :as string]))

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
  "Mark organizes matches by index in order to make easier to cut/shear"
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
  (select-keys var-usage [:to :name :from :from-var]))

(defn file-and-var [usage namespaces]
  (let [match ((:to usage) namespaces)]
    (when match (merge match (select-keys usage [:name])))))

(defn usages
  "Return a list of direct usages of a given var, with the :name and :filename, so Tailor can shear it!
  It stops to find usage when limit reachs 0/1"
  [target-var file _limit]
  (let [analysis   (kondo-analysis file)
        ns-map     (index-by :name (:namespace-definitions analysis) [:name :filename])
        usages     (map usage-info (:var-usages analysis))
        ns-matches (select-keys ns-map (map :to (from-var target-var usages)))]
    (filter identity (map #(file-and-var % ns-matches) usages))))

(defn shear-usage [usage-to-shear]
  (shear-top-level (:name usage-to-shear) (:filename usage-to-shear)))

(defn deep-shear [target-var target-file-path classpath-files-vec]
  (let [top-level-src (shear-top-level target-var target-file-path)
        usages-to-shear (usages target-var classpath-files-vec 10) ; should return a list in order make conj work properly
        usage-src (map shear-usage usages-to-shear)
        all-src  (conj usage-src top-level-src)]
    (string/join "\n" all-src)))

(comment
  (usages "my-fn"  ["./testResources/deep/1/root.clj"
                    "./testResources/deep/1/other_ns.clj"
                    "./testResources/deep/1/another.clj"
                    "./testResources/deep/1/root_dependency.clj"] nil)

  (deep-shear "my-fn" "./testResources/deep/1/root.clj"
              ["./testResources/deep/1/root.clj"
               "./testResources/deep/1/other_ns.clj"
               "./testResources/deep/1/another.clj"
               "./testResources/deep/1/root_dependency.clj"])

; ({:name call-fn, :filename "./testResources/deep/1/other_ns.clj"}
  ;  {:name another-fn, :filename "./testResources/deep/1/another.clj"}
  ;  {:name just-for-root,
  ;   :filename "./testResources/deep/1/root_dependency.clj"}
  ;  {:name another-just,
  ;   :filename "./testResources/deep/1/root_dependency.clj"}
  ;  # is the last one expected?
  ;  {:name child-call, :filename "./testResources/deep/1/another.clj"})

  (defn cleanup [var-usage]
    (select-keys var-usage [:ns :row :name :end-row]))
  (-> (find-var-defs "x" "./testResources/sample.clj")
      top-level-forms
      matching-rows))


