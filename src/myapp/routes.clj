(ns myapp.routes
  (:require
   [cheshire.core :as json]
   [compojure.core :refer [defroutes GET]]
   [huff2.core :as h]
   [mount.core :as mount :refer [defstate]]
   [myapp.db]
   [myapp.id :as id]
   [clojure.data :as data]
   [ring.middleware.params :refer [wrap-params]]
   [ring.util.response :refer [response]]
   [starfederation.datastar.clojure.adapter.ring :refer [->sse-response
                                                         on-close on-open]]
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

(defn tutorial-buzzer [_]
  (response
    (page
      [:div {:data-signals (json/generate-string {:response "" :answer ""})
             :data-computed-correct "$response.toLowerCase() == $answer"}
       [:div#question ""]
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

(def colors
  ["#2D4F5C" ;;- Deep teal-blue
   "#3A5768" ;;- Dark cyan-blue
   "#475F74" ;;- Medium steel blue
   "#546880" ;;- Cooler blue-gray
   "#61708C" ;;- Soft lavender-blue
   "#6E7898" ;;- Light periwinkle-blue
   "#FF6B2B" ;;- Vibrant fire orange
   ])

(defn color-n [n]
  (nth colors (mod n (count colors))))

(defn color->n [color]
  (try (loop [i 0]
         (if (= color (nth colors i))
           i
           (recur (inc i))))
       (catch Exception _ nil)))

(defn cycle-color [color]
  (let [n (color->n color)]
    (if n (color-n (inc n)) color)))

(let [t (atom 0)]
  (defn inc! [] (swap! t inc)))

(def rows 50)
(def cols 50)

(defn in-bounds? [rows cols x y]
  (and (>= x 0) (< x rows) (>= y 0) (< y cols)))

(def state (atom
             (apply merge-with merge
                    (for [x (range rows)
                          y (range cols)]
                      {x {y (color-n 0)}}))))

(defn cell-id [x y]
  (str "cell_" x "_" y))

(defn cell [x y bg-color]
  [:div {:id (cell-id x y)
         ;; :data-on-mouseenter (d*/sse-get (str "/bump?x=" x "&y=" y))
         ;; :data-on-mouseexit (d*/sse-get (str "/bump?x=" x "&y=" y))
         :data-on-click (d*/sse-get (str "/bump?x=" x "&y=" y))
         :style {:cursor :crosshair
                 :aspect-ratio "1"
                 :background-color bg-color
                 ;; :border "1px solid white"
                 }}
   " "])

(defn compute-board [state]
  [:#board
   {:style {:display "grid"
            :grid-template-columns (str "repeat(" cols ", 1fr)")
            :gap "0px"}}
   (map (fn [y]
          [:<>
           (map (fn [x]
                  [cell x y (get-in @state [x y])])
                (range cols))])
        (range rows))])

(defn home [_]
  (response
    (page
      [:div {:data-signals (json/generate-string {:user_id (id/id "user")})}
       (compute-board state)])))

(def *connections
  "user_id -> sse conn"
  (atom {}))

(defn broadcast-fragment! [fragment]
  (when (> (rand) 0.99)
    (println "\nbroadcasting fragment: " fragment))
  (future
    (doseq [[_user c] @*connections]
      (try
        (d*/merge-fragment! c fragment)
        (catch Exception _e
          (do (println "error sending to" c)
              (swap! *connections disj c)))))))

(defstate state-atom-watcher
  :start (add-watch state :state-atom-watcher
                    (fn [k r old-state new-state]
                      (let [[_ new _] (data/diff old-state new-state)
                            _ (def new new)
                            ;;_ (prn "NEW VALUE:" new)
                            ;; new is {x {y color}}
                            x (first (keys new))
                            y->c (get new x)
                            y (first (keys y->c))
                            color (first (vals y->c))]
                        ;;(prn ["broadcast with" x y color])
                        (broadcast-fragment!
                          (html [cell x y color])))))
  :stop (remove-watch state :state-atom-watcher))

(comment

  (def x 1)
  (def y 2)

  ;; TODO: what the hell do we build with it?
  ;; TODO: learn sse metaphysics

  (broadcast-fragment!
    (html (cell 0 0 "red")))

  )

(defn question [req respond raise]
  (respond (->sse-response
             req
             {on-open (fn [sse]
                        (swap! *connections conj sse)
                        (d*/merge-fragment! sse (html [:div#question "wut"]))
                        (d*/merge-signals! sse (json/generate-string {:response "" :answer "wop"})))
              on-close
              (fn [sse]
                (swap! *connections disj sse)
                (println "closed connection"))})))

(defn bump-cell [x y] (swap! state update-in [x y] cycle-color))

(defn bump-cell-handler [{params :params :as req} respond raise]
  (def bcreq req)
  (let [{:strs [x y datastar]} params
        {:keys [user_id]} (json/parse-string datastar keyword)
        x (Integer/parseInt x)
        y (Integer/parseInt y)]
    (let [resp (if (contains? @*connections user_id)
                 (response "ok")
                 (->sse-response
                   req
                   {on-open (fn [sse]
                              (println "adding conn" [user_id sse])
                              (swap! *connections assoc user_id sse))
                    on-close (fn [sse]
                               (swap! *connections dissoc user_id)
                               (println "closed connection" user_id))}))]
      ;; add the connection before upating state
      (future
        (bump-cell x y)
        (doseq [x' (range (- x 3) (+ x 4))
                y' (range (- y 3) (+ y 4))
                :when (in-bounds? rows cols x' y')]
          (bump-cell x' y'))
        (doseq [x' (range (- x 2) (+ x 3))
                y' (range (- y 2) (+ y 3))
                :when (in-bounds? rows cols x' y')]
          (bump-cell x' y'))
        (doseq [x' (range (- x 1) (+ x 2))
                y' (range (- y 1) (+ y 2))
                :when (in-bounds? rows cols x' y')]
          (bump-cell x' y')))
      (respond resp))))

(defroutes routes
  (GET "/" req (home req))
  (GET "/bump" [] bump-cell-handler)
  ;; Then host public files from resources:

  (route/resources "/static"))

(def app
  (-> routes
      wrap-params))

;; =>
