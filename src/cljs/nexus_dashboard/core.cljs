(ns nexus-dashboard.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [goog.string :refer [format]]
            [goog.string.format]
            [clojure.walk :refer [keywordize-keys]]
            [cljs.reader :as edn]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [cljs-time.core :as time]
            [cljs-time.format :as time-format])
  (:import goog.History))

(enable-console-print!)

(defonce weather-state (atom {}))

(defn refresh-weather! []
  (prn "Refreshing weather data")
  (go (let [response (<! (http/get "/weather"))
            data (keywordize-keys (edn/read-string (:body response)))]
        (when (:city data)
          (reset! weather-state data)))))

(refresh-weather!)

(defonce weather-loopty-loop
  (js/setInterval refresh-weather! 120000))

;; -------------------------
;; Views

(defn weather-icon-component [icon-name]
  [:img {:src (format "http://openweathermap.org/img/w/%s.png" icon-name)}])

(defn parse-time [string]
  (time-format/parse (time-format/formatters :mysql) string))

(defn format-time [dt]
  (time-format/unparse (time-format/formatter "HH:mm") dt))

(defn home-page []
  [:div
   [:div.title
    [:h2 (-> @weather-state :city :name)]]
   [:div.weather-report
    (for [e (:list @weather-state)]
      ^{:key e}
      [:span.day
       [:div.time (->> e :dt_txt parse-time format-time)]
       [:div.icon (-> e :weather first :icon weather-icon-component)]
       [:div.temp (->> e :main :temp (format "%d"))]])]])

(defn current-page []
  [:div [home-page]])

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
