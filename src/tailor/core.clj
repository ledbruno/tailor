(ns tailor.core)

;(defonce prompt-menu
 ; {:create-unit-test {:briefing "You are a code assistant, that helps to generate code from the instructions below:"
  ;                    :task "Your task is to create one or many unit tests to the target code."
   ;                   :restrictions "Use only the library clojure test. Your output should be just clojure test code. Do not comment your ideas of how you get to the result, just the resultant clojure expression"}
;   :replace-vars {:briefing "You are a code assistant, that helps to generate code from the instructions below:"
 ;                 :task "Your task is to change the target code, replacing all variable values to the replacement value provided"
  ;                :restrictions "Your output should be just clojure code. Do not comment your ideas of how you get to the result, just the resultant clojure expression"}})

(defonce prompt-menu {})

(defn- resolve-file [snippet]
  (slurp (:path snippet)))

(defn code 
  "Loads source code into a snippet
  Supported snippet types
  - :file -> loads the whole file
  - TODO! :file-chunk -> loads a piece of a file
  - TODO! :clojure-ns -> loads the source of a Clojure namespace from the repl
  - TODO! :clojure-symbol -> loads the source of a Clojure symbol from the repl"
  [snippet]
  (cond
    (= :file (:type snippet)) (assoc-in snippet [:code] (resolve-file snippet))))

(defn- code-snippets [snippets]
  (map code snippets))

(defn- with-code [context-template]
  (update context-template :snippets code-snippets))

(defn- snippet-and-desc [snippet]
  (str (:description snippet) "\n'''" (:code snippet) "'''\n"))

(defn- snippets [snippets]
  (str "This are some code snippets, that you can use as context on your task. \n" (clojure.string/join (map snippet-and-desc snippets))))

(defn register 
  "Adds a new prompt context to the menu"
  [key template]
  (assoc-in prompt-menu [key] (with-code template)))

(defn prompt 
  "Builds a prompt string from the context provided"
  [context]
  (clojure.string/join "\n" [(:briefing context) (snippets (:snippets context)) (:task context) (:restrictions context)]))


