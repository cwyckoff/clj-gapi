(defproject clj-gapi "1.0.3"
  :description "A simple pure clojure interface for Google web services"
  :license {:name "The Apache Software License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :url "https://github.com/ianbarber/clj-gapi"
  :dependencies [[org.clojure/clojure "1.6.0"]

                 [cheshire "5.5.0"]
                 [clj-http "0.7.8"]
                 [medley "0.6.0"]]
  :profiles {:dev {:plugins [[lein-expectations "0.0.7"]]
                   :dependencies [[clj-time "0.5.1"]
                                  [clj-http-fake "1.0.1"]
                                  [expectations "2.0.9"]]}})
