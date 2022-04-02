(ns metadata-tool.config
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :refer [expound-str]]
            [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [clojure.java.io :refer [file]]
            [metadata-tool.util :as util]
            [camel-snake-kebab.core :as csk]))

(def default-config {:notification-types "rescan_values,sync_schema"
                     :auth-type "cookie"
                     :db-type "mysql"
                     :db-port 3306
                     :notification-enabled true
                     :notification-plugin "dingtalk"})

(def default-keys [:data-dir 
                   :db-host :db-name :db-user :db-passwd :db-type :db-port
                   :metabase-url :dataset-id :auth-type :auth-key :auth-value

                   :dingtalk-access-token :dingtalk-access-secret 
                   :dingtalk-username :notification-types
                   :notification-enabled :notification-plugin
                   
                   :enable-syncdb :enable-notify])

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
(s/def ::data-dir (s/and string? exists?))

;; --------------------------- SyncDB ---------------------------
(s/def ::db-type #(some? ((keyword %) #{:mysql :postgresql})))

(s/def ::db-host string?)

;; More details on https://stackoverflow.com/q/13621307
(s/def ::db-port (s/int-in 1024 65535))

(s/def ::db-name string?)

(s/def ::db-user string?)

(s/def ::db-passwd string?)

;; Metabase Updating
(s/def ::metabase-url string?)

(s/def ::dataset-id nat-int?)

(s/def ::auth-key string?)

(s/def ::auth-value string?)

(s/def ::database (s/keys :req-un [::db-host ::db-port ::db-name ::db-user ::db-passwd
                                   ::metabase-url ::dataset-id ::auth-key ::auth-value]))

;; --------------------------- Notifications ---------------------------
;; Send Notifications - Dingtalk, Email
;; (s/def ::notification-plugin #(some? ((keyword %) #{:dingtalk})))
(s/def ::dingtalk-access-token string?)

(s/def ::dingtalk-access-secret string?)

(s/def ::dingtalk-username string?)

(s/def ::notification (s/keys :req-un [::dingtalk-access-token ::dingtalk-username
                                       ::dingtalk-access-secret]))

(s/def ::enable-syncdb boolean?)
(s/def ::enable-notify boolean?)

(s/def ::config (s/keys :req-un [::data-dir]
                        :opt-un [::db-host ::db-port ::db-name ::db-user ::db-passwd ::metabase-url
                                 ::dataset-id ::auth-key ::auth-value ::db-type ::notification-enabled
                                 ::notification-plugin ::dingtalk-access-token ::dingtalk-access-secret
                                 ::dingtalk-username ::enable-syncdb ::enable-notify]))

(defn config-valid?
  [config]
  (s/valid? ::config config))

(defn db-config-valid?
  [config]
  (s/valid? ::database config))

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

(defn value-str [_spec-name form path value]
  (when (not-empty path)
    (let [variable (csk/->SCREAMING_SNAKE_CASE (name (first path)))
          highlight-variable (util/format-color :red variable)]
      (format "Environment variable %s has an invalid value: %s" highlight-variable value))))

(defn- debug
  [spec config debug?]
  (if debug?
    (expound-str spec config {:show-valid-values? true :print-specs? false
                              :theme :figwheel-theme :value-str-fn value-str})
    (util/format-color :red "Configuration Error: please run with -D option for more information.\n")))

(def debug-config (partial debug ::config))

(def debug-database-config (partial debug ::database))

(def debug-notification-config (partial debug ::notification))
