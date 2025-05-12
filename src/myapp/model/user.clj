(ns myapp.model.user
  (:require
   [malli.core :as m]
   [myapp.db :as db]
   [myapp.schema :as schema]))

(defn id<- [id]
  (db/xcq
    {:select [:*]
     :from   [:user]
     :where  [:= :id id]}))

(defn email<- [email]
  (db/xcq
    {:select [:*]
     :from   [:user]
     :where  [:= :email email]}))

(defn token<- [token]
  (db/xcq
    {:select [:*]
     :from   [:user]
     :where  [:= :auth_token token]}))

#_(defn insert! [user pw]
  (let [hashed-pw (buddy.hashers/derive pw)
        user (assoc user :hashed_pw hashed-pw)]
    (db/xcq {:insert-into :user :values [user]})))

(comment

  (insert! {:screen_name "bill"
            :email "a@b.c"})

  )
