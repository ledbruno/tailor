(ns tailor.shears
  (:require [clj-kondo.core :as clj-kondo]
            [clojure.java.shell :as shell]))

(defn- var-named [var-name {var-defs :var-definitions}]
  (filter #(= var-name (:name %)) var-defs))

#_(defn parent-def [var-name analysis]
    (map #(find-var-defs (:from-var %) analysis) (filter #(= var-name (:name %)) (:var-usages analysis))))

(defn- find-var-defs
  [symbol-str file-path]
  "Returns a list of var defs with that matches the given symbol-str"
  (var-named (symbol symbol-str) (:analysis (clj-kondo/run!
                                             {:lint [file-path]
                                              :config {:analysis true}}))))

(defn- cut [from to file-path]
  (when (and from to) (shell/sh "sed" "-n" (str from "," to "p") file-path)))

(defn- at-root [found-defs]
  )

(defn shear-def-at-root [def-str file]
  (let [def-found (at-root (find-var-defs def-str file))]
    (:out (cut (:row def-found) (:end-row def-found) file))))
