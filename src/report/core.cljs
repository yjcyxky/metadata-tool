(ns report.core
  (:require [reagent.core :as reagent]
            ["escher" :as escher]))

(def options {:menu 'all' :fill_screen true})

(defn d3-select
  [id]
  (.d3_select escher/libs id))

(defn builder
  [comp error data]
  (if error (js/console.log error)
      (.Builder escher data nil nil (d3-select comp) options)))

(defn create-builder
  [comp]
  (partial builder comp))

(defn d3-json
  [path callback]
  (.d3_json escher/libs path callback))

(defn init-and-config-chart [comp]
  (let [dom (reagent/dom-node comp)
        chart (d3-json "https://nordata-cdn.oss-cn-shanghai.aliyuncs.com/choppy/metabolic-pathway-data.json" (create-builder "#chart"))]
    (js/console.info chart dom)))

(defn mount-chart [comp]
  (js/console.info "mounting.....")
  (init-and-config-chart comp))

(defn chart-inner []
  (js/console.info "inner...")
  (reagent/create-class
   {:component-did-mount   mount-chart
    :reagent-render        (fn []
                             (js/console.info "inner rendering...")
                             [:div#chart {:style {:width "100%"
                                                  :height "100%"}}])}))

(defn chart-outer []
  (js/console.info "outer....")
  [chart-inner])

(defn start []
  (reagent/render-component [chart-outer]
                            (. js/document (getElementById "app"))))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))
