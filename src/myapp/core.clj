(ns myapp.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [myapp.routes :as routes]
            [mount.core :as mount :refer [defstate]]))

;; Mount-managed state for the HTTP server
(defstate http-server
  :start
  (let [port 3000
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
