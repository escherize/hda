(ns myapp.schema
  (:require
   [malli.core :as m]))

(def User
  (m/schema
    [:map {:table-name "user"}
     [:id {:primary-key true} int?]
     [:screen-name [:re "^[a-zA-Z0-9_-]{3,30}$"]]
     [:email :string]
     [:hashed-pw :string]
     [:auth-token :string]
     [:created-at int?]
     [:signup-at int?]]))

(def col-seq
  (memoize
    (fn [mschema]
      (mapv first (m/children mschema)))))

(defn ensure-coll [x]
  (if (coll? x)
    x
    (if (nil? x)
      []
      [x])))

(defn ->insert! [mschema]
  (fn [m]
    (when-not (m/validate mschema m)
      (throw (ex-info "Invalid data" {:schema mschema :data m})))
    {:insert-into [(:table-name (m/properties mschema))]
     :columns (col-seq mschema)
     :values [(mapv (fn [k] (get m k)) (col-seq mschema))]}))

(def user-insert (->insert! User))

(comment
  (col-seq User)
  ;; => [:id :screen-name :email :hashed-pw :auth-token :created-at :signup-at]
  (user-insert {:email "a@a.a"}))
