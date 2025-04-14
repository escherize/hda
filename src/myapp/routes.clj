(ns myapp.routes
  (:require [reitit.ring :as rt]
            [myapp.views :as views]
            [ring.util.response :refer [response]]
            [myapp.db]
            [mount.core :refer [defstate]]
            [huff2.core :as h]))

(defn page [content]
  (h/page (views/top content)))

(defn html [& content]
  (str (h/html [:<> content])))

(defn list-users [_]
  (response (html [:h1 "userz"])))

(let [*a (atom 0)] (defn inc! [] (swap! *a inc)))

(defn home [_]
  (response (page [:h1 "hi guyse " (inc!)])))

(defstate app
  :start
  (rt/ring-handler
    (rt/router
      [""
       ["/" {:get home}]
       ["/api/users" {:get list-users}]])
    (rt/create-default-handler)))
