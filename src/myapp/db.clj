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
  :start
  (doto (org.sqlite.SQLiteDataSource.)
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

(defn screen-name->user [db screen-name]
  (d/pull db [:user/screen-name
              :user/magic-link
              :user/link-generated-at
              :user/first-login-at]
          (ffirst (d/q '[:find ?id
                         :in ?name $
                         :where [?id :user/screen-name ?name]]
                       screen-name
                       db))))

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
      (not= magic-link (:user/magic-link user))
      "wrong link"

      (> (now!) (+ (:user/link-generated-at user) expiration-time))
      "timed out"

      :else
      (transact-and-store! conn [{:user/screen-name screen-name
                                  :user/first-login-at (now!)}]))))

(comment

  (signup conn "person")

  (screen-name->user @conn "person")
  ;; => #:user{:link-generated-at 1743656312, :magic-link "magic_9UMu6TmBvx92v5wM8g94hP", :screen-name "person"}

  (login conn "person" "magic_9UMu6TmBvx92v5wM8g94hP")


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
