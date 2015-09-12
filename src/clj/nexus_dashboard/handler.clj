(ns nexus-dashboard.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [environ.core :refer [env]]
            [cheshire.core :as json]))

(def home-page
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
    [:body
     [:div#app
      [:h3 "Hi there!"]]
     (include-js "js/app.js")]]))

(defn fetch-weather-data []
  (json/parse-stream (clojure.java.io/reader "http://api.openweathermap.org/data/2.5/forecast?q=Haarlem,nl&mode=json&units=metric")))

(defn weather []
  (str (fetch-weather-data)))

(defroutes routes
  (GET "/" [] home-page)
  (GET "/weather" [] (weather))
  (not-found "Not Found"))

(def app
  (let [handler (wrap-defaults #'routes site-defaults)]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))
