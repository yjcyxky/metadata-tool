(defproject com.github.yjcyxky/metadata-tool "0.1.0"
  :description "For Metadata QC & QA."
  :url "https://github.com/yjcyxky/metadata-tool.git"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "1.1.0"]
                 [selmer "1.12.50"]
                 [org.clojure/data.json "2.4.0"]
                 [commons-codec "1.15"]
                 [org.clj-commons/byte-streams "0.2.10"]]
  :main metadata-util.core
  :aot [metadata-util.core]
  :test-paths ["test"]
  :resource-paths ["resources"]
  :plugins [[lein-cloverage "1.0.13"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]
            [lein-changelog "0.3.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.0"]]}}
  :deploy-repositories [["releases" :clojars]]
  :aliases {"update-readme-version" ["shell" "sed" "-i" "" "s/\\\\[com\\.github\\.yjcyxky\\\\/metadata-tool \"[0-9.]*\"\\\\]/[com\\.github\\.yjcyxky\\\\/metadata-tool \"${:version}\"]/" "README.md"]}
  :release-tasks [["shell" "git" "diff" "--exit-code"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["changelog" "release"]
                  ["update-readme-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["vcs" "push"]])