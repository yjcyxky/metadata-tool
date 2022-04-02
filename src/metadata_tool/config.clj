(ns metadata-tool.config
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :refer [expound-str]]
            [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [clojure.tools.logging :as log]
            [clojure.java.io :refer [file]]))

(def default-config {:notification-types "rescan_values,sync_schema"
                     :auth-type "cookie"
                     :db-type "mysql"
                     :db-port 3306
                     :notification-enabled true
                     :notification-plugin "dingtalk"
                     :data-dir ""
                     :db-host ""
                     :db-name ""
                     :db-user ""
                     :db-passwd ""
                     :metabase-url ""
                     :dataset-id 0
                     :auth-key ""
                     :auth-value ""
                     :dingtalk-access-token ""
                     :dingtalk-access-secret ""
                     :dingtalk-username ""})

(defn init-config!
  [args]
  (load-config :resource "config.edn"
               :merge [default-config
                       args
                       (source/from-system-props)
                       (source/from-env)]))

;; -------------------------------- Config Spec --------------------------------
(defn exists?
  [^String filepath]
  (.exists (file filepath)))

;; Metadata Tables
(s/def ::data-dir (s/and string? exists?))

;; Database
(s/def ::db-type #(some? ((keyword %) #{:mysql :postgresql})))

(s/def ::db-host string?)

;; More details on https://stackoverflow.com/q/13621307
(s/def ::db-port (s/int-in 1024 65535))

(s/def ::db-name string?)

(s/def ::db-user string?)

(s/def ::db-passwd string?)

;; Metabase Updating
(s/def ::metabase-url string?)

(s/def ::dataset-id integer?)

(s/def ::auth-key string?)

(s/def ::auth-value string?)

;; Send Notifications - Dingtalk, Email
(s/def ::notification-enabled boolean?)

(s/def ::notification-plugin #(some? ((keyword %) #{:dingtalk})))
(s/def ::dingtalk-access-token string?)

(s/def ::dingtalk-access-secret string?)

(s/def ::dingtalk-username string?)

(s/def ::config (s/keys :req-un [::data-dir ::db-host ::db-port ::db-name ::db-user
                                 ::db-passwd ::metabase-url ::dataset-id ::auth-key
                                 ::auth-value]
                        :opt-un [::db-type ::notification-enabled ::notification-plugin
                                 ::dingtalk-access-token ::dingtalk-access-secret
                                 ::dingtalk-username]))

(defn config-valid?
  [config]
  (let [config (select-keys config (keys default-config))]
    (s/valid? ::config config)))

(defn get-debug-msg
  [config]
  (expound-str ::config config))