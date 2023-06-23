(ns tailor.shears
  (:require [clj-kondo.core :as clj-kondo]
            [clojure.java.shell :as shell]))

(defn var-named [var-name {var-defs :var-definitions}]
  (filter #(= var-name (:name %)) var-defs))

#_(defn parent-def [var-name analysis]
    (map #(find-var-defs (:from-var %) analysis) (filter #(= var-name (:name %)) (:var-usages analysis))))

(defn find-var-def
  [symbol-str file-path]
  (var-named (symbol symbol-str) (:analysis (clj-kondo/run!
                                             {:lint [file-path]
                                              :config {:analysis true}}))))
(defn cut [from to file-path]
  (when (and from to) (shell/sh "sed" "-n" (str from "," to "p") file-path)))

(defn def-shear [def-str file]
  (let [def-found (first (find-var-def def-str file))]
    (:out (cut (:row def-found) (:end-row def-found) file))))

(def-shear "my-fn" "/tmp/foo.clj")
