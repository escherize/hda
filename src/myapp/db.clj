(ns myapp.db
  (:require
   [hikari-cp.core :as hikari]
   [hugsql.core :as hugsql]
   [mount.core :as mount :refer [defstate]]
   [myapp.config :refer [config]]
   [next.jdbc :as jdbc]))

(defstate ds
  :start
  (hikari/make-datasource
    {:jdbc-url (:jdbc-url config)
     :maximum-pool-size 20
     :pool-name "myapp-pool"})
  :stop
  (do (println "closing the connection pool...")
      (hikari/close-datasource ds)))

(defstate db
  :start
  (do (println "starting db...")
      {:datasource ds})
  :stop
  (println "stopping db..."))

(comment

  (jdbc/execute! ds ["select 1 + 1"])

  (hugsql/def-db-fns "queries.sql")

  (create-users-table db)

  (insert-user db {:email "aa" :name  "bb" :password-hash "cc"})

  (user-by-email db {:email "aa"})


  )
