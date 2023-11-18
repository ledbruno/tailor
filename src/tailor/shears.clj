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

(defn matching-usages [s-var usages]
  (filter #(and (= (symbol (namespace s-var)) (:from %))
                (= (symbol (name s-var)) (:from-var %)))
          usages))

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

(defn destination-symbol
  "Used to create target usage symbol on getting deep into usages
  In other words, it gets the usage destination-info"
  [usage]
  (symbol (name (:ns usage)) (name (:name usage))))

(defn origin-symbol
  [usage]
  (symbol (name (:from usage)) (name (:from-var usage))))

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
  (let [analysis        (kondo-analysis classpath)
        ns-map          (index-by :name (:namespace-definitions analysis) [:name :filename])
        usages          (map usage-info (:var-usages analysis))
        matches         (matching-usages target-symbol usages)
        ns-matches-map  (select-keys ns-map (map :to matches))
        ns-matches      (map #(ns-usage-info ns-matches-map %) matches)
        non-nil-matches (filter identity ns-matches)]
    non-nil-matches))

(defn shear-dependency [dep-usage all-usages]
  (let [deps (matching-usages (destination-symbol dep-usage) all-usages)]
    (str (helper/ns-declare (:ns dep-usage) deps) (shear-top-level (:name dep-usage) (:filename dep-usage)))))

(defn- deep-usage [usage classpath]
  (usages (destination-symbol usage) classpath))

(defn deep [var-usages classpath]
  (flatten (conj (map #(deep-usage % classpath) var-usages) var-usages)))

(defn deep-shear
  [target-symbol target-file-path classpath]
  (let [target-var          (name target-symbol)
        target-ns           (namespace target-symbol)
        var-usages          (deep (usages target-symbol classpath) classpath)  ; should return a list in order make conj work properly
        usages-src          (s/join (map #(shear-dependency % var-usages) var-usages))
        top-level-src       (str (helper/ns-declare target-ns (matching-usages target-symbol var-usages)) (shear-top-level target-var target-file-path))]
    (str usages-src "\n" top-level-src)))

(comment
  (deep '({:alias other-ns,
           :filename "./testResources/deep/1/other_ns.clj",
           :from deep.1.root,
           :name call-fn,
           :ns deep.1.other-ns})
        ["./testResources/deep/1/root.clj"
         "./testResources/deep/1/other_ns.clj"
         "./testResources/deep/1/another.clj"
         "./testResources/deep/1/root_dependency.clj"])

  (usages "run" ["./testResources/deep/2/top_level.clj"
                 "./testResources/deep/2/deep_1.clj"
                 "./testResources/deep/2/deep_2.clj"
                 "./testResources/deep/2/deep_3.clj"])

  (usages "my-fn" ["./testResources/deep/1/root.clj"
                   "./testResources/deep/1/other_ns.clj"
                   "./testResources/deep/1/another.clj"
                   "./testResources/deep/1/root_dependency.clj"])
  (spit "/tmp/result.clj" (deep-shear 'deep.1.root/my-fn "./testResources/deep/1/root.clj"
                                      ["./testResources/deep/1/root.clj"
                                       "./testResources/deep/1/other_ns.clj"
                                       "./testResources/deep/1/another.clj"
                                       "./testResources/deep/1/root_dependency.clj"])))


