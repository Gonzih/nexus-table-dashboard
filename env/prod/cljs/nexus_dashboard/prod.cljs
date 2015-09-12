(ns nexus-dashboard.prod
  (:require [nexus-dashboard.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
