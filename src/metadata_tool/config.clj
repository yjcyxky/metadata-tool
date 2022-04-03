(ns metadata-tool.config
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :refer [expound-str defmsg]]
            [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [clojure.java.io :refer [file]]
            [metadata-tool.util :as util]
            [camel-snake-kebab.core :as csk]))

(def default-config {:data-dir          nil
                     :enable-syncdb     false
                     :enable-notify     false

                     :db-host           nil
                     :db-name           nil
                     :db-user           nil
                     :db-passwd         nil
                     :db-type           "sqlite"
                     :db-port           3306
                     :metabase-url      nil
                     :dataset-id        0
                     :auth-type         nil
                     :auth-key          nil
                     :auth-value        nil

                     :dingtalk-access-token  nil
                     :dingtalk-access-secret nil
                     :dingtalk-username      nil
                     :notification-types     "rescan_values,sync_schema"
                     :notification-enabled   true
                     :notification-plugin    "dingtalk"})

(def common-config-keys [:data-dir :enable-syncdb :enable-notify])

(def db-config-keys [:db-host :db-name :db-user :db-passwd :db-type :db-port
                     :metabase-url :dataset-id :auth-type :auth-key :auth-value])

(def notification-config-keys [:dingtalk-access-token :dingtalk-access-secret
                               :dingtalk-username :notification-types
                               :notification-enabled :notification-plugin])
(def default-keys (concat common-config-keys db-config-keys notification-config-keys))

(defn init-config!
  [args]
  (-> (load-config :resource "config.edn"
                   :merge [default-config
                           args
                           (source/from-system-props)
                           (source/from-env)])
      (select-keys default-keys)))

;; -------------------------------- Config Spec --------------------------------
(defn exists?
  [^String filepath]
  (.exists (file filepath)))

;; Metadata Tables
(defmsg ::data-dir (str "[String] The data directory must exists and contains several metadata tables, "
                        "you can set it by `export DATA_DIR=/path/to/dir` or argument `-d /path/to/dir`"))
(s/def ::data-dir (s/and string? exists?))

;; --------------------------- SyncDB ---------------------------
(defmsg ::db-type (str "[Enum] Only support mysql and postgresql, "
                       "`export DB_TYPE=mysql`"))
(s/def ::db-type #(some? ((keyword %) #{:mysql :postgresql})))

(defmsg ::db-host (str "[String] You can set the database host by "
                       "`export DB_HOST=127.0.0.1`"))
(s/def ::db-host string?)

;; More details on https://stackoverflow.com/q/13621307
(defmsg ::db-port (str "[Postive Integer] You can set the database port (between 1024 and 65535) by "
                       "`export DB_PORT=3306`"))
(s/def ::db-port (s/int-in 1024 65535))

(defmsg ::db-name (str "[String] You can set the database name by "
                       "`export DB_NAME=metadata_tool`"))
(s/def ::db-name string?)

(defmsg ::db-user (str "[String] You can set the database username by "
                       "`export DB_USER=root`"))
(s/def ::db-user string?)

(defmsg ::db-passwd (str "[String] You can set the database password by "
                         "`export DB_PASSWD=root`"))
(s/def ::db-passwd string?)

;; Metabase Updating
(defmsg ::metabase-url (str "[String] You can set the metabase url by "
                            "`export METABASE_URL=http://127.0.0.1/`"))
(s/def ::metabase-url string?)

(defmsg ::dataset-id (str "[Positive Integer] You can set the dataset id by "
                          "`export DATASET_ID=1`"))
(s/def ::dataset-id nat-int?)

(defmsg ::auth-key (str "[String] You can set the auth key by "
                        "`export AUTH_KEY=xxx`"))
(s/def ::auth-key string?)

(defmsg ::auth-value (str "[String] You can set the auth value by "
                          "`export AUTH_VALUE=xxx`"))
(s/def ::auth-value string?)

(s/def ::database (s/keys :req-un [::db-host ::db-port ::db-name ::db-user ::db-passwd
                                   ::metabase-url ::dataset-id ::auth-key ::auth-value]))

;; --------------------------- Notifications ---------------------------
;; Send Notifications - Dingtalk by Email
;; (s/def ::notification-plugin #(some? ((keyword %) #{:dingtalk})))
(defmsg ::dingtalk-access-token (str "[String] You can set the access token of dingtalk by "
                                     "`export DINGTALK_ACCESS_TOKEN=xxx`"))
(s/def ::dingtalk-access-token string?)

(defmsg ::dingtalk-access-secret (str "[String] You can set the access secret of dingtalk by "
                                      "`export DINGTALK_ACCESS_SECRET=xxx`"))
(s/def ::dingtalk-access-secret string?)

(defmsg ::dingtalk-username (str "[String] You can set the username of dingtalk by "
                                 "`export DINGTALK_USERNAME=xxx`"))
(s/def ::dingtalk-username string?)

(s/def ::notification (s/keys :req-un [::dingtalk-access-token ::dingtalk-username
                                       ::dingtalk-access-secret]))

(s/def ::enable-syncdb boolean?)
(s/def ::enable-notify boolean?)

(s/def ::common-config (s/keys :req-un [::data-dir ::enable-syncdb ::enable-notify]
                               :opt-un []))

(defn common-config-valid?
  [config]
  (s/valid? ::common-config config))

(defn db-config-valid?
  [config]
  (s/valid? ::database config))

(defn get-db-config
  [config]
  (select-keys config db-config-keys))

(defn get-notification-config
  [config]
  (select-keys config notification-config-keys))

(defn notification-config-valid?
  [config]
  (s/valid? ::notification config))

(s/fdef value-str
  :args (s/cat
         :spec-name (s/nilable #{:args :fn :ret})
         :form any?
         :path :expound/path
         :value any?)
  :ret string?)

(defn value-str [_spec-name _form path value]
  (let [output-fn (fn [k v] (let [var-name (csk/->SCREAMING_SNAKE_CASE (name k))
                                  highlighted-var-name (util/format-color :red var-name)]
                              (format "Environment variable %s has an invalid value: %s" highlighted-var-name v)))]
    (when (not-empty path) (output-fn (first path) value))))

(defn- debug
  [spec config debug?]
  (if debug?
    (expound-str spec config {:show-valid-values? true :print-specs? false
                              :theme :figwheel-theme :value-str-fn value-str})
    (util/format-color :red "Configuration Error: please run with -D option for more information.\n")))

(def debug-common-config (partial debug ::common-config))

(def debug-database-config (partial debug ::database))

(def debug-notification-config (partial debug ::notification))
