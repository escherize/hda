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
  [

"#040b06" "#122b2b" "#324f6c" "#ff513d"

   ;; "#6B7584" ;;- Muted steel blue
 ;; "#6F7983" ;;- Soft blue-gray
 ;; "#737B83" ;;- Light blue-gray
 ;; "#787D82" ;;- Pale blue-gray
 ;; "#FF4820"
   ;;- Bright tomato-red orange
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

(defn default-state []
  (apply merge-with merge
         (for [x (range rows)
               y (range cols)]
           {x {y (color-n 0)}})))

(defonce state (atom (default-state)))

(defn cell-id [x y]
  (str "cell_" x "_" y))

(defn cell [x y bg-color]
  [:div {:id (cell-id x y)
         ;; :data-on-mouseenter (d*/sse-get (str "/bump?x=" x "&y=" y))
         ;; :data-on-mouseexit (d*/sse-get (str "/bump?x=" x "&y=" y))
         :data-on-mousedown (d*/sse-get (str "/bump?x=" x "&y=" y))
         :style {:cursor :crosshair
                 :aspect-ratio "1"
                 :background-color bg-color
                 ;; :border "1px solid white"
                 }}
   " "])

(defn compute-board [state]
  [:<>
   [:#board
    {:style {:display "grid"
             :grid-template-columns (str "repeat(" cols ", 1fr)")
             :gap "0px"}}
    (map (fn [y]
           [:<>
            (map (fn [x]
                   [cell x y (get-in @state [x y])])
                 (range cols))])
         (range rows))]])

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

(defn counter []
  (let [color->count (sort-by first (frequencies (mapcat vals (vals @state))))
        total (reduce + (map second color->count))
        color->width (into {}
                           (map (fn [[color count]]
                                  (let [width (int (* 100 (/ count total)))]
                                    [color (if (> width 0)
                                             (str width "%")
                                             "0%")]))
                                color->count))]
    [:#counter {:style {:display "flex"
                        :height "100px"
                        :width "100% !important"
                        :box-sizing "border-box"
                        :border "5px solid black"}}
     [:<>
      (for [[color count] color->count]
            [:.
             {:style {:width (get color->width color)
                      :margin 0
                      :height "100%"
                      :background-color color
                      :font-weight "bold"
                      :font-size "16px"}} count])]]))

(defn home [_]
  (response
    (page
      [:div {:data-signals (json/generate-string {:user_id (id/id "user")})}
       (counter)
       (compute-board state)
       [:button.btn-btn-primary
        {:data-on-click (d*/sse-get "/reset")}
        "Reset"]])))


(defstate state-atom-watcher
  :start (add-watch state :state-atom-watcher
                    (fn [k r old-state new-state]
                      (let [[_ new _] (data/diff old-state new-state)
                            _ (def new new)]
                        ;;(prn ["broadcast with" x y color])
                        (doseq [x (keys new)
                                [y color] (get new x)]
                          (broadcast-fragment!
                            (html [cell x y color])))
                        (broadcast-fragment!
                          (html [counter])))))
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

(defn bump-cell [x y]
  (swap! state update-in [x y] cycle-color)
  (response "ok"))

(defn circular-orbit [center distance]
  (let [[cx cy] center
        sequence
        ;; Start from the top (center-x, center-y - distance) and move clockwise
        (concat
          ;; Top: right-moving from (cx-d, cy-d) to (cx+d, cy-d)
          (for [x (range (- cx distance) (+ cx distance 1))]
            [x (- cy distance)])
          ;; Right: down-moving from (cx+d, cy-d+1) to (cx+d, cy+d)
          (for [y (range (+ (- cy distance) 1) (+ cy distance 1))]
            [(+ cx distance) y])
          ;; Bottom: left-moving from (cx+d-1, cy+d) to (cx-d, cy+d)
          (for [x (range (dec (+ cx distance)) (dec (- cx distance)) -1)]
            [x (+ cy distance)])
          ;; Left: up-moving from (cx-d, cy+d-1) to (cx-d, cy-d+1)
          (for [y (range (dec (+ cy distance)) (- cy distance) -1)]
            [(- cx distance) y]))]
    sequence))

(defn cell-pattern [x y]
  (concat
    [[x y]]
    (circular-orbit [x y] 1)
    (reverse (circular-orbit [x y] 2))
    (circular-orbit [x y] 3)
    (reverse (circular-orbit [x y] 4))))

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
      (future (doseq [[x' y'] (cell-pattern x y)]
                (Thread/sleep 20)
                (bump-cell x' y')))
      (respond resp))))

(defn reset-handler [_]
  (reset! state (default-state)))

(defroutes routes
  (GET "/" req (home req))
  (GET "/bump" [] bump-cell-handler)
  (GET "/reset" req (reset-handler req)))

(def app
  (-> routes wrap-params))
