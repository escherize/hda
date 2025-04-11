(ns myapp.routes
  (:require
   [huff2.core :as h]
   [mount.core :refer [defstate]]
   [myapp.db]
   [myapp.id :as id]
   [myapp.view :as view]
   [reitit.ring :as rt]
   [ring.util.response :refer [response]]))

(def minified? false)

(defn page [content]
  [:html {:data-theme "dracula"}
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Yo"]
    ;; [:script "tailwind = tailwind || {} ; tailwind.config = {darkMode:'class'}"]
    [:script {:src "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"}]
    [:link {:href "https://cdn.jsdelivr.net/npm/daisyui@5" :rel "stylesheet" :type "text/css"}]
    [:link {:href "https://cdn.jsdelivr.net/npm/daisyui@5/themes.css" :rel "stylesheet" :type "text/css"}]
    (if minified?
      [:script {:src "https://unpkg.com/htmx.org@2.0.4" :integrity "sha384-HGfztofotfshcF7+8n44JQL2oJmowVChPTg48S+jvZoztPfvwD79OC/LTtG6dMp+" :crossorigin "anonymous"}]
      [:script {:src "https://unpkg.com/htmx.org@2.0.4/dist/htmx.js" :integrity "sha384-oeUn82QNXPuVkGCkcrInrS1twIxKhkZiFfr2TdiuObZ3n3yIeMiqcRzkIcguaof1" :crossorigin "anonymous"}])
    [:script {:src "https://unpkg.com/htmx.org@1.7.0"}]]
   [:body
    [:div {:class "min-h-screen flex flex-col bg-base-200"}
     ;; Header/Navbar
     [:header {:class "bg-base-100 shadow-md"}
      [:div {:class "container mx-auto px-4 py-4"}
       [:h1 {:class "text-3xl font-bold"} "map-vals"]]]
     ;; Main Content Area
     [:main {:class "container mx-auto px-4 py-8 flex-grow"}
      content]
     ;; Footer
     [:footer {:class "bg-base-100 text-center py-4"}
      [:p "© 2025 My Website. All rights reserved."]]]]])

(defn html [& content]
  (str (h/html [:<> content])))

(defn list-users [_]
  [:h1 "userz"])

(defonce __
  (let [*a (atom 1)]
    (defn inc! [] (swap! *a inc))))

(defn hr [hic]
  (response (html hic)))

(defn rand-hsl []
  (str "hsl(" (mod (* 2 (inc!)) 360) ",100%," (+ 30 (rand-int 50)) "%)"))

(defn fork [auto?]
  (let [id (id/mini)]
    [:div
     [:h1 "Fork Bomb - " (inc!) " | " id]
     [:div {:style {:margin-left "10px"
                    :border (str "1px solid " (rand-hsl))}}
      [:div {:id id}
       [:button
        (merge {:class "btn btn-primary"
                :hx-post "/fork"
                :hx-target (str "#" id)
                :hx-swap "outerHTML"}
               (when auto? {:hx-trigger "load delay:0.1s"}))
        "What"]]]]))

(defn fork-starter []
  [:div#fork-start [:button.btn.btn-primary.btn-outline
                    {:hx-post "/fork" :hx-target "#fork-start" :hx-swap "afterend"}
                    [:h1 "Start Fork Bomb"]]])

(defn home [_]
  [:div
   [:h1 {:style {:font-size "100px"}} "Welcome."]
   [:label.swap.swap-flip.text-9xl
    [:input {:type "checkbox"}
     [:.swap-on "😈"]
     [:.swap-off "😇"]]]

   [fork-starter]
   [:button.btn.btn-primary
    {:hx-get "/ks" :hx-target "body"}
    "Kitchen Sink"]])

(defstate app
  :start
  (rt/ring-handler
    (rt/router
      [""
       ["/" {:get (fn [req] (hr (page (home req))))}]
       ["/ks" {:get (fn [_] (hr (page [:div.container
                                       [:button {:hx-post "/" :hx-target "#ks"} "close"]
                                       [:button.btn.btn-primary
                                        {:hx-get "/" :hx-target "body"}
                                        "Home"]
                                       [view/kitchen-sink]])))}]
       ["/fork" {:post (fn [_] (hr (into [:<>] (repeatedly (rand-int 3) #(fork true)))))}]
       ["/api/users" {:get (fn [_] (hr list-users))}]])
    (rt/create-default-handler)))
