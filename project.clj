(defproject tailor "0.1.0-SNAPSHOT"
  :description "Helps you write code automation prompts"
  :url "https://github.com/ledbruno/tailor/"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.namespace "1.4.4"]
                 [clj-kondo "2023.10.20"]]
  :profiles {:test {:resource-paths ["testResources"]
                    :dependencies [[nubank/matcher-combinators "3.8.5"]]}}
  #_#_:repl-options {:init-ns tailor.shears})
