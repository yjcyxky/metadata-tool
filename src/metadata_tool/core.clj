(ns metadata-tool.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [local-fs.core :refer [directory?]]
            [clojure.string :as clj-str]
            [metadata-tool.version :refer [version]]
            [metadata-tool.config :refer [init-config! config-valid? get-debug-msg]]
            [clojure.tools.logging :as log]))

(def cli-options
  [["-d" "--data-dir PATH" "Data Directory"
    :validate [#(directory? %) "Must be a valid directory"]]
   ["-v" "--version" "Show version" :default false]
   ["-D" "--debug" "Show debug messages" :default false]
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
            debug-mode (:debug options)]
        (when (and debug-mode (not valid?))
          (println (get-debug-msg config)))
        {:config config
         :valid? valid?
         :exit-message (usage summary)}))))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main
  "Launch metadata-tool."
  [& args]
  (let [{:keys [config valid? exit-message ok?]} (validate-args args)]
    (when exit-message
      (exit (if ok? 0 1) exit-message))
    (when valid?
      (println "Everything is okay!" config))
    (shutdown-agents)))
