(ns myapp.routes
  (:require
   [cheshire.core :as json]
   [compojure.core :refer [defroutes GET]]
   [huff2.core :as h]
   [myapp.db]
   [ring.util.response :refer [response]]
   [starfederation.datastar.clojure.adapter.ring :refer [on-open on-close ->sse-response]]
   [starfederation.datastar.clojure.api :as d*]))

(defn page [content]
  (h/page [:html
           [:head
            [:meta {:charset "utf-8"}]
            [:title "hi"]
            [:link {:href "https://cdn.jsdelivr.net/npm/daisyui@5" :rel "stylesheet" :type "text/css"}]
            [:script {:src "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"}]
            [:link {:href "https://cdn.jsdelivr.net/npm/daisyui@5/themes.css" :rel "stylesheet" :type "text/css"}]
            [:script {:src "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-beta.11/bundles/datastar.js" :type "module"}]]
           [:body.bg-white.dark:bg-gray-800.text-black.dark:text-white
            {:data-theme "retro"}
            content]]))

(defn html [& content]
  (str (h/html [:<> content])))

(defn home [_]
  (response
    (page
      [:div {:data-signals (json/generate-string {:response "" :answer ""})
             :data-computed-correct "$response.toLowerCase() == $answer"}
       [:div#question ""]
       [:div#apple ""]
       [:button.btn.btn-primary
        {:data-on-click (d*/sse-get "/question") :data-show "$answer == ''"}
        "Fetch a question"]
       [:button.btn.btn-primary
        {:data-show "$answer != ''"
         :data-on-click "$response = prompt('answer:') ?? ''"}
        "BUZZ"]
       [:. {:data-show "$response != ''"}
        "you answered '" [:span {:data-text "$response"}] "'."
        [:. {:data-show "$correct"} "yep!"]
        [:. {:data-show "!$correct"} "nope: it is '" [:span {:data-text "$answer"}] "'."]]])))

(def *connections (atom #{}))

(defn broadcast-fragment! [fragment]
  (doseq [c @*connections]
    (try
      (d*/merge-fragment! c fragment)
      (catch Exception _e
        (do (println "error sending to" c)
            (swap! *connections disj c))))))

(comment

  (future
    (dotimes [n 1000]
      (Thread/sleep 100)
      (broadcast-fragment!
        (html [:div#question (str "wee: " n)]))))

  )

(defn question [req respond raise]
  (let [resp (->sse-response
               req
               {on-open (fn [sse]
                          (def sse sse)
                          (println "!?")
                          (swap! *connections conj sse)
                          (d*/merge-fragment! sse (html [:div#question "wut"]))
                          (d*/merge-signals! sse (json/generate-string {:response "" :answer "wop"})))
                on-close
                (fn [sse]
                  (swap! *connections disj sse)
                  (println "closed connection"))})]
    (prn ["RESP" resp]
         (respond resp))))

(defroutes app
  (GET "/" req (home req))
  (GET "/question" [] question))

;; =>
