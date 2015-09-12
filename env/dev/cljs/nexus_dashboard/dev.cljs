(ns ^:figwheel-no-load nexus-dashboard.dev
  (:require [nexus-dashboard.core :as core]
            [goog.string :refer [format]]
            [goog.string.format]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(let [hostname (or js/document.location.hostname
                   "localhost")
      ws-path (format "ws://%s:3449/figwheel-ws" hostname)]
  (figwheel/watch-and-reload
   :websocket-url ws-path
   :jsload-callback core/mount-root))

(core/init!)
