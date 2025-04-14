(ns myapp.config
  (:require [mount.core :refer [defstate]]
            [clj-yaml.core :as yaml]))

(defn get-config []
  (when-let [config-path (or (System/getenv "HDA_CONFIG") "hda_config.yml")]
    (when-let [config-contents (try (slurp config-path) (catch Exception _ nil))]
      (when-let [config-map (yaml/parse-string config-contents)]
        config-map))))

(defstate config
  :start
  (merge
    (get-config)
    {:server {:port 3337}

     :jdbc-url
     "jdbc:sqlite:target/db.sqlite"

     :dev/mode :dev ;; :prod ?

     :schema
     {:user/screen-name       {:db/unique :db.unique/identity}
      :user/magic-link        {}
      :user/link-generated-at {:db/type :db.type/long}
      :user/signup-at         {:db/type :db.type/long}

      :todo/title     {}
      :todo/done?     {:db/type :db.type/boolean}
      :todo/done-date {:db/type :db.type/long}
      :todo/user      {:db/valueType :db.type/ref}}}))

(defn dev? []
  (= :dev (:dev/mode config)))
