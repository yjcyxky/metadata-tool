(ns metadata-util.core
  (:require [selmer.parser :refer [render render-file]]
            [metadata-util.formatter :refer [gen-embeded-data get-path cache-resource-file!]])
  (:import [clojure.lang PersistentHashMap]))

(defn- render-html!
  [^String html-string data]
  (render html-string {:embeded_data data}))

(defn- render-html-file!
  [^String html-path data]
  (render-file html-path {:embeded_data data}))

(defn render-to-file!
  [^String dest-html-path ^PersistentHashMap data]
  (let [js-file (cache-resource-file! "report/js/main.js")
        formatted-data (gen-embeded-data data {} js-file)
        src-html-path (.toString (cache-resource-file! "report/index.html"))
        rendered-html (render-html-file! src-html-path formatted-data)]
    (spit dest-html-path rendered-html)))

(def js-file (cache-resource-file! "report/js/main.js"))

(def formatted-data (gen-embeded-data {} {} js-file))
