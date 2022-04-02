(ns metadata-util.core
  (:require [selmer.parser :refer [render render-file]]
            [metadata-util.formatter :refer [gen-embeded-data get-path cache-resource-file!]])
  (:import [clojure.lang PersistentHashMap]))

(defn- render-html!
  [^String html-string data]
  (render html-string {:embeded_data data}))

(defn render-to-file!
  [^String dest-html-path ^PersistentHashMap data]
  (let [js-file (cache-resource-file! "report/js/main.js")
        formatted-data (gen-embeded-data data {} js-file)
        src-html (slurp (.toString (cache-resource-file! "report/index.html")))
        rendered-html (render-html! src-html formatted-data)]
    (spit dest-html-path rendered-html)))
