(ns myapp.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [myapp.routes :as routes]
            [myapp.config :refer [config]]
            [mount.core :as mount :refer [defstate]])
  (:gen-class))

;; Mount-managed state for the HTTP server
(defstate http-server
  :start
  (let [port (or (-> config :server :port) 3334)
        server (run-jetty #'routes/app {:port port :join? false})]
    (println "Starting HTTP server on port" port "...")
    server)
  :stop
  (do (println "Stopping HTTP server...")
      (.stop http-server)))

;; Entry point for the application
(defn -main []
  (mount/start))  ;; Starts all mount states


(comment

  (mount/stop))
