(defproject tailor "0.1.0-SNAPSHOT"
  :description "Helps you write code automation prompts"
  :url "https://github.com/ledbruno/tailor/"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-kondo "2023.05.26"]]
  :profiles {:test {:resource-paths ["testResources"]
                    :dependencies [[nubank/matcher-combinators "3.8.5"]] }}
  :repl-options {:init-ns tailor.core})
