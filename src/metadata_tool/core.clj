(ns metadata-tool.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [local-fs.core :refer [directory?]]
            [clojure.string :as clj-str]
            [metadata-tool.version :refer [version]]
            [metadata-tool.config :refer [init-config! config-valid?
                                          debug-config debug-database-config
                                          debug-notification-config]]
            [clojure.tools.logging :as log]))

(def cli-options
  [["-d" "--data-dir PATH" "Data Directory"
    :validate [#(directory? %) "Must be a valid directory"]]
   ["-v" "--version" "Show version" :default false]
   ["-D" "--debug" "Show debug messages" :default false]
   ["-m" "--enable-syncdb" "Enable sync to database." :default false]
   ["-n" "--enable-notify" "Enable notify user by dingtalk" :default false]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["MetadataTool - For metadata QC & QC."
        ""
        "Usage: metadata-tool [options]"
        ""
        "Options:"
        options-summary
        ""
        "Please refer to the manual page for more information."]
       (clj-str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clj-str/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      ;; custom validation on arguments
      (:version options)
      {:exit-message (format "v%s" version)}

      :else ; failed custom validation => exit with usage summary
      (let [config (init-config! options)
            valid? (config-valid? config)
            debug-mode (:debug options)
            error-msg (cond
                        (not valid?) (debug-config config debug-mode)
                        (:enable-syncdb config) (debug-database-config config debug-mode)
                        (:enable-notification config) (debug-notification-config config debug-mode))]
        (when error-msg (exit 1 error-msg))
        {:config config
         :exit-message (if valid? nil (usage summary))}))))

(defn -main
  "Launch metadata-tool."
  [& args]
  (let [{:keys [config exit-message ok?]} (validate-args args)]
    (when exit-message
      (exit (if ok? 0 1) exit-message))
    (println "Everything is okay!" config)
    (shutdown-agents)))
