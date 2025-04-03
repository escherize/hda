(ns myapp.routes
  (:require [reitit.ring :as rt]
            [ring.util.response :refer [response]]
            [myapp.db]
            [mount.core :refer [defstate]]
            [huff2.core :as h]))

(defn page [content]
  (h/page [:html
           [:head
            [:meta {:charset "utf-8"}]
            [:title "HTMX w/ Clojure"]
            [:script "tailwind.config = {darkMode:'class'}"]
            ;;     <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
            [:script {:src "https://cdn.tailwindcss.com"}]
            [:script {:src "https://unpkg.com/htmx.org@1.7.0"}]]
           [:body.bg-white.dark:bg-gray-800.text-black.dark:text-white content]]))

(defn html [& content]
  (str (h/html [:<> content])))

(defn list-users [_]
  (response (html [:h1 "userz"])))

(let [*a (atom 0)] (defn inc! [] (swap! *a inc)))

(defn home [_]
  (response (page [:h1 "hi" (inc!)])))

(defstate app
  :start
  (rt/ring-handler
    (rt/router
      [""
       ["/" {:get home}]
       ["/api/users" {:get list-users}]])
    (rt/create-default-handler)))
