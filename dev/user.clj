(ns user
  (:require [myapp.core :as core]
            [nrepl.server :as nrepl]
            [mount.core :as mount]
            [myapp.db :as db]
            [clj-reload.core :as reload]))

(defn start-nrepl-server []
  (let [server (nrepl/start-server :port 7888 :bind "0.0.0.0")]
    (println "nREPL server started on port 7888")
    server))

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(defn reset []
  (stop)
  (start))

(defn -main [& args]
  (start-nrepl-server)
  (apply core/-main args))

;; Test functions
(defn test-create-user! []
  (db/create-user! db/conn "test@example.com" "password123" "testuser"))

(defn test-authenticate [email password & [screen-name]]
  (db/authenticate db/conn email password screen-name))

(defn test-get-user-by-screen-name [screen-name]
  (db/screen-name->user @db/conn screen-name))

(defn test-get-user-by-email [email]
  (db/email->user @db/conn email))

(defn test-auth-token->user [token]
  (db/auth-token->user @db/conn token))

(comment
  ;; Create a test user
  (test-create-user!)
  
  ;; Get the user by email
  (test-get-user-by-email "test@example.com")
  
  ;; Authenticate with correct password
  (test-authenticate "test@example.com" "password123")
  
  ;; Authenticate with incorrect password
  (test-authenticate "test@example.com" "wrong-password")
  
  ;; Create a new user with auto-generated screen name
  (test-authenticate "new@example.com" "newpassword")
  )

(defn reload-all []
  (reload/reload))
