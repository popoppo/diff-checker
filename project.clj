(defproject diff-checker "0.1.0-SNAPSHOT"
  :description "Checks target diffs"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.3.6"]
                 [clj-http "2.3.0"]
                 [clj-time "0.11.0"]
                 [enlive "1.1.6"]
                 [environ "1.1.0"]]

  :aliases {"check" ^:pass-through-help ["run" "-m" "diff-checker.core"]
            "get-hash" ^:pass-through-help ["run" "-m" "cmd.get-hash"]}

  ;; :plugins []

  :source-paths ["src"]

  :plugins [[lein-kibit "0.1.5"]
            [jonase/eastwood "0.2.5"]]

  ;; :main ^:skip-aot diff-checker.core
  ;; :target-path "target/%s"
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [org.clojure/test.check "0.9.0"]
                                  [alembic "0.3.2"]]}
             :uberjar {:aot :all}})
