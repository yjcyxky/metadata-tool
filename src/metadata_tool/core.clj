(ns metadata-tool.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [local-fs.core :refer [directory?]]
            [clojure.string :as clj-str]
            [metadata-tool.version :refer [version]]
            [metadata-tool.util :as util]
            [metadata-tool.config :as conf]
            [metadata-tool.csv2sql.core :as csv2sql]))

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
      (let [config (conf/init-config! options)
            valid? (and (conf/common-config-valid? config)
                        (if (:enable-syncdb config) (conf/db-config-valid? config) true)
                        (if (:enable-notify config) (conf/notification-config-valid? config) true))
            debug-mode (:debug options)
            error-msg (do
                        (when (not valid?) (conf/debug-common-config config debug-mode))
                        (when (:enable-syncdb config) (conf/debug-database-config config debug-mode))
                        (when (:enable-notify config) (conf/debug-notification-config config debug-mode)))]
        {:config config
         :exit-message (if valid? nil (format "%s\n%s" error-msg (usage summary)))}))))

(defn destroy!
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (shutdown-agents)
  (println "Metadata tool has shutdown!"))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     ;; Force shutdown
     (destroy!)
     (println {:what :uncaught-exception
               :exception ex
               :where (str "Uncaught exception on" (.getName thread))}))))

(defn -main
  "Launch metadata-tool."
  [& args]
  (let [{:keys [config exit-message ok?]} (validate-args args)]
    (when exit-message
      (exit (if ok? 0 1) exit-message))

    ;; Shutdown
    ;; (.addShutdownHook (Runtime/getRuntime) (Thread. destroy!))

    ;; Sync Data to database
    (println (util/format-color :green "Sync all metadata to database..."))
    (csv2sql/syncdb! (:data-dir config)
                     :database-config (if (conf/db-config-valid? config)
                                        (conf/get-db-config config)
                                        {})
                     :notification-config (if (conf/notification-config-valid? config)
                                            (conf/get-notification-config config)
                                            {}))
    (println (util/format-color :green "\n\nSync successfully!"))

    ;; Make a QC & QA report

    (destroy!)))
