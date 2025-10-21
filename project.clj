(defproject org.clojars.ledbruno/tailor "0.1.0-SNAPSHOT"
  :description "Helps you shear/cut clojure source code for automation tasks (e.g: AI prompts)"
  :url "https://github.com/ledbruno/tailor/"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.namespace "1.4.4"]
                 [clj-kondo "2023.10.20"]]
  :profiles {:test {:resource-paths ["testResources"]
                    :dependencies [[nubank/matcher-combinators "3.8.5"]]}}
  #_#_:repl-options {:init-ns tailor.shears})
