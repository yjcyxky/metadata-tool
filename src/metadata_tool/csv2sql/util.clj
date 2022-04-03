(ns metadata-tool.csv2sql.util
  (:require [clojure.java.io :as io]
            [clojure.string :as clj-str]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [camel-snake-kebab.core :as csk])
  (:import [org.apache.commons.io.input BOMInputStream]))

(set! *warn-on-reflection* true)

(defn alphanumeric?
  "TRUE when the string is completely alphanumeric."
  [^String string & strict-mode?]
  (let [strict-mode? (first strict-mode?)
        pattern (if strict-mode? #"[a-z_0-9]" #"[a-z_A-Z0-9]")]
    (and (> (count string) 0)
         (= string (apply str (re-seq pattern string))))))

(defn spaces-to-underscores
  "Converts spaces to underscores."
  [^String string]
  (clj-str/replace string #"\s" "_"))

(defn periods-to-underscores
  "Converts periods to underscores."
  [^String string]
  (clj-str/replace string #"\." "_"))

(defn camel-to-snake
  "Converts camel to snake case."
  [^String string]
  (csk/->snake_case string :separator #"[.\-\& ]"))

(defn bom-reader
  "Remove `Byte Order Mark` and return reader"
  [^String filepath]
  (-> filepath
      io/input-stream
      BOMInputStream.
      io/reader))

(defn table-schema-file
  [^java.io.File dir]
  (io/file (format "%s-schema.edn" (.getAbsolutePath dir))))

(defn table-sql-file
  [^java.io.File dir]
  (io/file (format "%s.sql" (.getAbsolutePath dir))))

(defn matching-csv-for-json
  [^java.io.File jsonfile]
  (io/file (format "%s.csv" (.getAbsolutePath jsonfile))))

;; ------------------------------------------- Metabase ---------------------------------------------
(defn make-cookie
  [key value]
  {:cookies {key {:path "/", :value value}}})

(defn make-auth
  [auth-type auth-key auth-value]
  (when (= auth-type "cookie")
    (make-cookie auth-key auth-value)))

(def http-options {:unexceptional-status #(<= 200 % 299)
                   :socket-timeout 3000
                   :connection-timeout 3000})

(defn send-notification!
  [url auth-type auth-key auth-value]
  (print "Send notification to " url)
  (client/post url (merge http-options
                          (make-auth auth-type auth-key auth-value)))
  (print "\t\tSuccess.\n"))

(defn metabase-auth-url
  [base-url]
  ; http://metabase.3steps.cn/api/session
  (str base-url "/api/session"))

(defn metabase-auth
  [base-url username password]
  (let [body (:body (client/post (metabase-auth-url base-url)
                                 (merge http-options
                                        {:content-type :json
                                         :body (json/write-str {:username username :password password})})))
        session-id (:id (json/read-str body :key-fn keyword))]
    {:auth-key "metabase.SESSION"
     :auth-value session-id}))

(defn metabase-notification-url
  [base-url dataset notification-type]
  ; http://metabase.3steps.cn/api/database/6/rescan_values
  (str base-url "/api/database/" dataset "/" notification-type))

(defn refresh-metabase!
  "Refresh metabase manually for making the new uploading data take effect."
  [metabase-url dataset-id username password]
  (let [notification-types "rescan_values,sync_schema"
        notification-types (clj-str/split notification-types #",")
        auth (metabase-auth metabase-url username password)]
    (doseq [type notification-types]
      (send-notification! (metabase-notification-url metabase-url dataset-id type)
                          "cookie"
                          (:auth-key auth)
                          (:auth-value auth)))))
