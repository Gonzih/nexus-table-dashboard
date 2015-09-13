(ns nexus-dashboard.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [goog.string :refer [format]]
            [goog.string.format]
            [clojure.string :as string]
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
    [:i.owf.owf-3x {:class (format "owf-%d-%s" id day-or-night)}]))

(defn parse-time [string]
  (time-format/parse (time-format/formatters :mysql) string))

(defn format-time [dt]
  (time-format/unparse (time-format/formatter "HH:mm") dt))

(defn format-date [dt]
  (time-format/unparse (time-format/formatter "d MMM") dt))

(defonce popup-content (atom nil))

(defn show-popup! [data]
  (reset! popup-content data))

(defn hide-popup! []
  (reset! popup-content nil))

(defn popup-component []
  (when @popup-content
    [:div.popup
     {:on-click hide-popup!}
     @popup-content]))

(def request-fullscreen! (or js/document.documentElement.requestFullscreen
                             js/document.documentElement.mozRequestFullscreen
                             js/document.documentElement.webkitRequestFullscreen
                             identity))

(defn summary-component [{{:keys [temp_min temp_max grnd_level humidity]} :main :keys [rain snow weather] :as data}]
  [:span.summary
   [:div.header
    [:img {:src (format "http://openweathermap.org/img/w/%s.png" (-> weather first :icon))}]
    (-> weather first :description string/capitalize)]
   [:div.info
    (format "Temperature — %d℃ - %d℃" temp_min temp_max)
    (when rain (format "Rain volume — %fmm" (:3h rain)))
    (when snow (format "Snow volume — %fmm" (:3h snow)))
    (format "Humidity — %d%" humidity)
    (format "Atmospheric pressure — %fhPa" grnd_level)
    (format "Wind — %fm/sec" (-> data :wind :speed))
    (format "Clouds — %d%" (-> data :clouds :all))]])

(defn day-component [first-date {:keys [dt_txt weather main] :as data}]
  (let [date (-> dt_txt parse-time format-date)
        extra-class (if (= date first-date)
                      "current-day"
                      "future-day")]
    ^{:key data}
    [:span.day
     {:class extra-class
      :on-click #(show-popup! [#'summary-component data])}
     [:div.date date]
     [:div.time (->> dt_txt parse-time format-time)]
     [:div.icon (->> weather first weather-icon-component)]
     [:div.temp (->> main :temp (format "%d"))]]))

(defn weather-report-component []
  (if-let [time-slots (:list @weather-state)]
    [:div.weather-report
     (map (partial day-component
                   (-> @weather-state :list first :dt_txt parse-time format-date))
          (take 25 time-slots))]))

(defn home-page []
  [:div
   [:div.title
    {:on-click request-fullscreen!}
    [:h2 (-> @weather-state :city :name)]]
   [weather-report-component]
   [popup-component]])

(defn current-page []
  [:div [home-page]])

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
