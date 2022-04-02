(ns metadata-util.formatter
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [byte-streams :refer [to-byte-array]]
            [clojure.string :as clj-str])
  (:import [java.nio.file Path Files Paths LinkOption]
           [java.io File]
           [clojure.lang PersistentHashMap IFn]
           [org.apache.commons.codec.binary Base64]))

(set! *warn-on-reflection* true)

(defn get-path
  [^String path]
  (Paths/get path (into-array String [])))

(defn- assert-exists
  [^Path path]
  (assert (Files/exists path (into-array LinkOption [])) (format "%s doesn't exist." path)))

(defn read-to-byte-array
  [^Path path]
  (-> (.toAbsolutePath path)
      .toString
      File.
      to-byte-array))

(defn read-to-string
  [^Path path]
  (slurp (.toString (.toAbsolutePath path))))

(defn- base64-encode
  [^"[B" content]
  (-> content
      Base64/encodeBase64
      String.))

(defn- format-json
  [^PersistentHashMap data]
  (json/write-str data))

(defn- format-embeded-data [^IFn custom-fn ^PersistentHashMap data]
  (into {} (map (fn [[k v]] (custom-fn {k v})) data)))

(defmulti format-embeded-item (fn [^PersistentHashMap hm] (first (keys hm))))

(defmethod format-embeded-item :icon
  [^PersistentHashMap item]
  {:icon (->> (:icon item)
              read-to-byte-array
              base64-encode
              (format "data:image/png;base64,%s"))})

(defmethod format-embeded-item :data
  [^PersistentHashMap item]
  {:data (format-json (:data item))})

(defmethod format-embeded-item :config
  [^PersistentHashMap item]
  {:config (format-json (:config item))})

(defmethod format-embeded-item :css
  [^PersistentHashMap item]
  {:css (read-to-string (:css item))})

(defmethod format-embeded-item :js_code
  [^PersistentHashMap item]
  {:js_code (read-to-string (:js_code item))})

(defn cache-resource-file!
  [^String filepath]
  (let [resource-file (io/resource filepath)
        filename (last (clj-str/split (.getFile resource-file) #"/"))
        temp-file (File/createTempFile filename nil)]
    (spit temp-file (slurp resource-file))
    (get-path (.toString temp-file))))

(defn gen-embeded-data
  "Format & setup embeded data."
  [^PersistentHashMap data
   ^PersistentHashMap config
   ^Path jspath
   & {:keys [^Path csspath ^Path iconpath]
      :or {csspath (cache-resource-file! "report/css/style.css")
           iconpath (cache-resource-file! "report/image/logo.png")}}]
  (assert-exists jspath)
  (assert-exists csspath)
  (assert-exists iconpath)
  (format-embeded-data format-embeded-item
                       {:data data
                        :config config
                        :js_code jspath
                        :css csspath
                        :icon iconpath}))
