(ns metadata-tool.csv2sql.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
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
  [string]
  (str/replace string #"\s" "_"))

(defn periods-to-underscores
  "Converts spaces to underscores."
  [string]
  (str/replace string #"\." "_"))

(defn camel-to-snake
  "Converts camel to snake case."
  [string]
  (csk/->snake_case string))

(defn bom-reader
  "Remove `Byte Order Mark` and return reader"
  [filepath]
  (-> filepath
      io/input-stream
      BOMInputStream.
      io/reader))

(defn is-dir?
  "TRUE when a directory exists."
  [path]
  (.isDirectory (io/file path)))