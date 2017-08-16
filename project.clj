(defproject camdez/honeybadger "0.3.0"
  :description "Clojure library for reporting errors to honeybadger.io"
  :url "https://github.com/camdez/honeybadger"
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[aleph "0.4.3"]
                 [clj-stacktrace "0.2.8"]
                 [prismatic/schema "1.1.6"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/data.json "0.2.6"]]
  :profiles {:dev {:injections [(require 'schema.core)
                                (schema.core/set-fn-validation! true)]}})
