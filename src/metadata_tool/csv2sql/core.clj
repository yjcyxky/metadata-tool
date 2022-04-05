(ns metadata-tool.csv2sql.core
  (:require [clojure.data.csv]
            [clojure.edn :as edn]
            [clojure.java.jdbc :as sql]
            [local-fs.core :as fs]
            [clojure.string :as clj-str]
            [metadata-tool.csv2sql.guess-schema :as guess]
            [metadata-tool.csv2sql.files :as files]
            [metadata-tool.csv2sql.json :as json]
            [metadata-tool.csv2sql.csvs :as csv]
            [metadata-tool.csv2sql.util :as util]
            [notify-api.adapter.dingtalk :as dingtalk])
  (:import [clojure.lang PersistentHashMap]))

(defn autodetect-sql-schemas!
  "Scans through the subdirectories of CSVDIR, infers the column data types,
  and stores the inferred schema in CSVDIR so that you may manually edit it
  before loading it in with MAKE-SQL-TABLES."
  [csvdir]
  (doseq [dir (files/list-subdirectories csvdir)]
    ; https://clojuredocs.org/clojure.core/printf#example-542692d4c026201cdc327038
    (printf "Autodetecting schema for: %s\n" dir)
    (flush)
    (let [tablename (.getName ^java.io.File dir)
          schema (guess/scan-csvdir-and-make-schema dir)]
      (when-not (empty? schema)
        (let [table-sql (guess/table-definition-sql-string tablename schema)]
          (println (util/table-schema-file dir) schema)
          (spit (util/table-schema-file dir) schema)
          (spit (util/table-sql-file dir) table-sql))))))

(defn get-default-db-config
  [workdir]
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     (fs/join-paths workdir "sqlite-database.db")})

(defn format-db-config
  [{:keys [db-type db-host db-port db-name db-user db-passwd workdir]}]
  (let [config {:host     db-host
                :port     db-port
                :dbname   db-name
                :user     db-user
                :password db-passwd}]
    (cond
      (= db-type "postgresql") (merge config {:dbtype "postgresql"})
      (= db-type "mysql") (merge config {:dbtype "mysql"})
      :else (get-default-db-config workdir))))

(defn connection-ok?
  "A predicate that tests if the database is connected."
  [db]
  (= {:result 15} (first (sql/query db ["select 3*5 as result"]))))

(defn drop-existing-sql-tables!
  "For each subdirectory in DIRNAME, drop any tables with the same name."
  [db csvdir]
  (doseq [table-name (map (fn [f] (.getName ^java.io.File f))
                          (files/list-subdirectories csvdir))]
    (let [cmd (format "DROP TABLE IF EXISTS %s;" table-name)]
      (sql/db-do-commands db cmd))))

(defn make-sql-tables!
  "Makes the SQL tables from whatever is in the database. "
  [db csvdir]
  (doseq [sql-file (map (fn [f] (.getAbsolutePath ^java.io.File f))
                        (files/list-files-of-type csvdir "sql"))]
    (println sql-file)
    (let [table-sql (slurp sql-file)]
      (println table-sql)
      (sql/db-do-commands db table-sql))))

(defn insert-csv!
  "Inserts the rows of the CSV into the database, converting the rows to the appropriate
  type as they are loaded. Lazy, so it works on very large files. If a column is not
  found in the schema, it is omitted and not inserted into the database. "
  [db table csvfile schema]
  (with-open [reader (util/bom-reader csvfile)]
    (let [sep (csv/guess-separator csvfile)
          csv-rows (clojure.data.csv/read-csv reader :separator sep)
          [header typed-rows] (guess/parse-csv-rows-using-schema schema csv-rows)
          cnt (atom 0)
          chunk-size 1000]
      (doseq [chunk-of-rows (partition-all chunk-size typed-rows)]
        (let [line-num (swap! cnt inc)]
          (print ".")
          (flush))
        (sql/insert-multi! db table header chunk-of-rows)))))

(defn insert-all-csvs!
  "Loads all the subdirectories of DATA_DIR as tables. Optional hashmap MANUAL-OPTIONS
  lets you decide how to customize various tables; for example, you may want to set
  an optional table."
  [db csvdir]
  (doseq [dir (files/list-subdirectories csvdir)]
    (let [tablename (.getName ^java.io.File dir)
          schema (edn/read-string (slurp (util/table-schema-file dir)))]
      (when-not (empty? schema)
        (doseq [csvfile (files/list-files-of-type dir "csv|tsv|txt")]
          (print (format "\nLoading: %s " csvfile))
          (insert-csv! db tablename csvfile schema))))))

(defn syncdb!
  "Sync all metadata tables in the data directory to the database."
  [^String workdir & {:keys [^PersistentHashMap database-config ^PersistentHashMap notification-config]
                      :or {database-config {}
                           notification-config {}}}]
  (let [db-config (format-db-config (merge database-config {:workdir workdir}))]
    ;; get-default-db-config will return a sqlite db config, so the following code will never executes?
    (when-not (connection-ok? db-config)
      (throw (Exception. (str "Unable to connect to DB: " db-config))))

    (drop-existing-sql-tables! db-config workdir)
    (json/convert-jsons-to-csvs! workdir)
    (guess/setup-strict-mode true)
    (autodetect-sql-schemas! workdir)
    (make-sql-tables! db-config workdir)
    (insert-all-csvs! db-config workdir)

    ;; You need to refresh metabase when you use mysql or postgresql.
    (when (or true (not-empty database-config))
      (util/refresh-metabase! (:metabase-url database-config) (:dataset-id database-config)
                              (:auth-key database-config) (:auth-value database-config)))

    ;; Notification: XXX dataset is updated.
    (when (not-empty notification-config)
      (let [dingtalk-access-token (:dingtalk-access-token notification-config)
            dingtalk-access-secret (:dingtalk-access-secret notification-config)
            metabase-url (clj-str/replace (:metabase-url database-config) #"/$" "")
            details-url (format "%s/browser/%s" metabase-url (:dataset-id database-config))
            basename (fs/basename workdir)]
        (dingtalk/setup-dingtalk dingtalk-access-token dingtalk-access-secret)
        (dingtalk/send-action-card! "MetadataTool Notification"
                                    (format "Dataset %s is updated, please click the card to access." basename)
                                    "Browse the dataset"
                                    details-url)))))
