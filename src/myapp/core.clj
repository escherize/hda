(ns myapp.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [ring.util.response :refer [response]]
            [mount.core :refer [defstate start stop]]
            [datascript.core :as d]
            [datascript.storage-sql :as dsql]))

;; Define a simple schema for Datascript
(def schema
  {:person/name {:db/unique :db.unique/identity}})

;; Mount-managed state for the Datascript connection
(defstate db
  :start
  (let [conn (d/create-conn schema)]
    ;; Initialize the SQLite storage; data will be persisted in "data.db"
    (dsql/initialize-conn conn {:sqlite-file "data.db"})
    conn)
  :stop
  (println "Stopping Datascript DB..."))

;; Define a basic set of routes
(defroutes app-routes
  (GET "/" []
    (response
      "<!DOCTYPE html>
      <html>
      <head>
        <meta charset='utf-8'>
        <title>HTMX with Clojure</title>
        <!-- Include HTMX via CDN -->
        <script src='https://unpkg.com/htmx.org@1.7.0'></script>
      </head>
      <body>
        <h1>Welcome to the Clojure HTMX App</h1>
        <!-- Add your HTMX-powered elements here -->
      </body>
      </html>"))
  (route/not-found "Not Found"))

;; Mount-managed state for the HTTP server
(defstate http-server
  :start
  (let [port 3000]
    (println "Starting HTTP server on port" port "...")
    (run-jetty app-routes {:port port :join? false}))
  :stop
  (println "Stopping HTTP server..."))

;; Entry point for the application
(defn -main []
  (start))  ;; Starts all mount states (db and http-server)