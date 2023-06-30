(ns tailor.shears
  (:require [clj-kondo.core :as clj-kondo]
            [clojure.java.shell :as shell]))

(defn index-by [key-fn coll]
  (into {} (map (juxt key-fn identity) coll)))

(defn- var-named [var-name {var-defs :var-definitions}]
  (filter #(= var-name (:name %)) var-defs))

(defn- find-var-defs
  "Returns a list of var defs with that matches the given symbol-str"
  [symbol-str file-path]
  (let  [analysis (:analysis (clj-kondo/run!
                              {:lint [file-path]
                               :config {:analysis true}}))]
    {:matches (var-named (symbol symbol-str) analysis)
     :analysis analysis}))

(defn- cut [from to file-path]
  (when (and from to) (shell/sh "sed" "-n" (str from "," to "p") file-path)))

(defn- at-root
  [{analysis :analysis matches :matches}]
  ;if more than one should throw an error?
  (let [ns (:name (first (:namespace-definitions analysis)))
        ns-root-level-forms (filter #(and (not (contains? % :from-var)) (= (:from %) ns)) (:var-usages analysis))
        root-level-row-matches (clojure.set/intersection   (set (map :row ns-root-level-forms)) (set (map :row matches)))]
    ;maybe map all, not only the first found
    (get (index-by :row matches) (first root-level-row-matches))))

(defn shear-def-at-root [def-str file]
  (let [def-found (at-root (find-var-defs def-str file))]
    (:out (cut (:row def-found) (:end-row def-found) file))))
