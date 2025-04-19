(ns myapp.db
  (:require [myapp.config :refer [config]]
            [myapp.id :refer [id]]
            [datascript.core :as d]
            [mount.core :as mount :refer [defstate]]
            [datascript.storage.sql.core :as storage-sql]
            [cognitect.transit :as t])
  (:import [java.io
            ByteArrayOutputStream
            ByteArrayInputStream]))

(defstate datasource
  :start (doto (org.sqlite.SQLiteDataSource.)
           (.setUrl (:jdbc-url config)))
  :stop
  (println "Stopping datasource..."))

(defstate storage
  :start
  (storage-sql/make
    datasource
    {:dbtype :sqlite
     :table "datascript"
     :freeze-bytes
     (fn ^bytes [obj]
       (with-open [out (ByteArrayOutputStream.)]
         (t/write (t/writer out :msgpack) obj)
         (.toByteArray out)))

     :thaw-bytes
     (fn [^bytes b]
       (t/read (t/reader (ByteArrayInputStream. b) :msgpack)))})
  :stop
  (println "Stopping storage..."))

(defstate conn
  :start
  (or (d/restore-conn storage)
      (d/create-conn (:schema config) {:storage storage}))
  :stop
  (println "Stopping conn..."))

;;;;; db functions:

(defn now! [] (quot (System/currentTimeMillis) 1000))

(defn transact-and-store! [conn entities]
  (let [o (d/transact! conn entities)]
    (d/store @conn)
    o))

(defn create-user!
  "Creates a new user with the given email, password, and optional screen name.
   Returns the user data with the generated auth token."
  [conn email password & [screen-name]]
  (let [auth-token (id "auth")
        now (now!)
        screen-name (or screen-name (str "user-" (subs (id "") 0 8)))
        user-data {:user/email email
                 :user/password password
                 :user/screen-name screen-name
                 :user/auth-token auth-token
                 :user/created-at now}]
    (transact-and-store! conn [user-data])
    {:email email
     :screen-name screen-name
     :auth-token auth-token}))

(defn screen-name->user [db screen-name]
  (d/pull db [:user/screen-name
              :user/email
              :user/password
              :user/auth-token
              :user/created-at
              :user/magic-link
              :user/link-generated-at
              :user/first-login-at]
          (ffirst (d/q '[:find ?id
                         :in ?name $
                         :where [?id :user/screen-name ?name]]
                       screen-name
                       db))))

(defn email->user [db email]
  (let [query-result (d/q '[:find ?id
                           :in ?email $
                           :where [?id :user/email ?email]]
                         email
                         db)
        entity-id (ffirst query-result)]
    (when entity-id
      (d/pull db [:user/screen-name
                 :user/email
                 :user/password
                 :user/auth-token
                 :user/created-at]
             entity-id))))

(comment
  (d/q '[:find ?email :in $ :where [_ :user/email ?email]] db)
  ;; => #{["test@example.com"]}
  )

(defn auth-token->user [db auth-token]
  (let [query-result (d/q '[:find ?id
                           :in ?token $
                           :where [?id :user/auth-token ?token]]
                         auth-token
                         db)
        entity-id (ffirst query-result)]
    (when entity-id
      (d/pull db [:user/screen-name
                 :user/email
                 :user/auth-token
                 :user/created-at]
             entity-id))))
                       
;; This is a debug function that doesn't use "!" in the name
(defn make-user
  "Creates a new user without the ! in the function name, for REPL debugging."
  [conn email password & [screen-name]]
  (let [auth-token (id "auth")
        now (now!)
        screen-name (or screen-name (str "user-" (subs (id "") 0 8)))
        user-data {:user/email email
                 :user/password password
                 :user/screen-name screen-name
                 :user/auth-token auth-token
                 :user/created-at now}]
    (transact-and-store! conn [user-data])
    {:email email
     :screen-name screen-name
     :auth-token auth-token}))

(defn make-auto-user
  "Creates a user with an auto-generated screen name for REPL debugging."
  [conn email password]
  (let [auto-name (str "user-" (subs (id "random") 0 8))]
    (make-user conn email password auto-name)))

(defn authenticate
  "Authenticate user by email and password. If user doesn't exist, create a new one with the provided screen name.
   Returns the user data with auth token."
  [conn email password & [screen-name]]
  (let [existing-user (email->user @conn email)]
    (if existing-user
      (if (= (:user/password existing-user) password)
        {:email email
         :screen-name (:user/screen-name existing-user)
         :auth-token (:user/auth-token existing-user)}
        {:error "Incorrect password"})
      (if screen-name
        (make-user conn email password screen-name)
        (make-auto-user conn email password)))))

(comment (screen-name->user @conn "person"))

(defn signup
  "Logs in a user by screen name, generating and storing a magic link along with a timestamp (seconds since epoch)."
  [conn screen-name]
  (let [magic-link (id "magic")
        now (now!)]
    (transact-and-store! conn [{:user/screen-name screen-name
                                :user/magic-link magic-link
                                :user/link-generated-at now}])
    magic-link))

(def expiration-time (* 60 30))

(defn login [conn screen-name magic-link]
  (let [user (screen-name->user @conn screen-name)]
    (cond
      (not user)
      "No user by that name."

      (not= magic-link (:user/magic-link user))
      "wrong link."

      (> (now!) (+ (:user/link-generated-at user) expiration-time))
      "timed out."

      :else
      (transact-and-store! conn
                           [{:user/screen-name screen-name
                             :user/first-login-at (now!)}]))))

(comment

  (signup conn "person")

  (screen-name->user @conn "person")
  ;; => #:user{:link-generated-at 1743656312, :magic-link "magic_9UMu6TmBvx92v5wM8g94hP", :screen-name "person"}

  (login conn "person" (:user/magic-link (screen-name->user @conn "person")))

  (mapv #(into [] %) (sort-by first (:eavt @conn)))

  ;; Function to add a todo for a given user.
  (defn add-todo
    "Adds a todo with the given title for the user identified by screen-name.
   Throws an error if the user is not found."
    [conn screen-name title]
    (let [user-eid (d/q '[:find ?e .
                          :in $ ?screen
                          :where [?e :user/screen-name ?screen]]
                        @conn screen-name)]
      (if user-eid
        (d/transact! conn [{:todo/title title
                            :todo/done false
                            :todo/user user-eid}])
        (throw (ex-info "User not found" {:screen-name screen-name})))))

  ;; Function to mark a todo as done by its entity id.
  (defn mark-todo-done
    "Marks the todo (by its entity id) as done and sets the done date to the current time (seconds since epoch)."
    [conn todo-id]
    (let [now (quot (System/currentTimeMillis) 1000)]
      (d/transact! conn [{:db/id todo-id
                          :todo/done true
                          :todo/done-date now}]))))
