(ns myapp.config
  (:require [mount.core :refer [defstate]]))

(defstate config
  :start
  {:server {:port "3333"}

   :jdbc-url
   "jdbc:sqlite:target/db.sqlite"

   :schema
   {:user/screen-name       {:db/unique :db.unique/identity}
    :user/magic-link        {}
    :user/link-generated-at {:db/type :db.type/long}
    :user/signup-at         {:db/type :db.type/long}

    :todo/title     {}
    :todo/done?     {:db/type :db.type/boolean}
    :todo/done-date {:db/type :db.type/long}
    :todo/user      {:db/valueType :db.type/ref}}})
