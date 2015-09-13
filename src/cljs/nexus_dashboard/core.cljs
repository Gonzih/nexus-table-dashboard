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

(defn weather-icon-component [{:keys [id icon] :as weather}]
  (let [day-or-night (if (re-find #"d" icon) "d" "n")]
    [:i.owf {:class (format "owf-%d-%s" id day-or-night)}]))

(defn parse-time [string]
  (time-format/parse (time-format/formatters :mysql) string))

(defn format-time [dt]
  (time-format/unparse (time-format/formatter "HH:mm") dt))

(defn format-date [dt]
  (time-format/unparse (time-format/formatter "d MMM") dt))

(def request-fullscreen! (or js/document.documentElement.requestFullscreen
                             js/document.documentElement.mozRequestFullscreen
                             js/document.documentElement.webkitRequestFullscreen
                             identity))

(defn day-component [first-date {:keys [dt_txt weather main] :as data}]
  (let [date (-> dt_txt parse-time format-date)
        extra-class (if (= date first-date)
                      "current-day"
                      "future-day")]
    ^{:key data}
    [:span.day
     {:class extra-class}
     [:div.date date]
     [:div.time (->> dt_txt parse-time format-time)]
     [:div.icon (->> weather first weather-icon-component)]
     [:div.temp (->> main :temp (format "%d"))]]))

(defn weather-report-component []
  (if-let [time-slots (:list @weather-state)]
    [:div.weather-report
     (map (partial day-component
                   (-> @weather-state :list first :dt_txt parse-time format-date))
          (take 35 time-slots))]))

(defn home-page []
  [:div
   [:div.title
    {:on-click request-fullscreen!}
    [:h2 (-> @weather-state :city :name)]]
   [weather-report-component]])

(defn current-page []
  [:div [home-page]])

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
