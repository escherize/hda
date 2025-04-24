(ns user
  (:require [myapp.core :as core]
            [mount.core :as mount]
            [nrepl.server :as nrepl]))

(defn start-nrepl-server []
  (let [server (nrepl/start-server :port 7888 :bind "0.0.0.0")]
    (println "nREPL server started on port 7888")
    server))

(defn -main [& args]
  (start-nrepl-server)
  (apply core/-main args))
