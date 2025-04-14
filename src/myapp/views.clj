(ns myapp.views
  (:require
   [myapp.config :as c]
   [clojure.walk :as walk]
   [hickory.core :as hik]))

(def ->hiccup
  (memoize
    (fn ->hiccup [html-string]
      (walk/postwalk
        (fn [x] (if (and (vector? x) (keyword? (first x)) (= {} (second x)))
                  (vec (cons (first x) (drop 2 x)))
                  x))
        (hik/as-hiccup
          (first (hik/parse-fragment
                   html-string)))))))



(defn signal-example []
  [:.
   {:data-signalsf "{response: '', answer: 'bread'}" ;; json ok here
    :data-computed-correct "$response.toLowerCase() == $answer"}
   [:.#question "What do you put in a toaster?"]
   [:button {:data-on-click "$response = prompt('Answer:') ?? ''"} "BUZZ"]
   [:. {:data-show "$response != ''"} "You answered “" [:span {:data-text "$response"}] "”.\n"
    [:span {:data-show "$correct"} "That is correct ✅"]
    "\n    "
    [:span {:data-show "!$correct"} "The correct answer is “\n      "
     [:span {:data-text "$answer"}] "\n      ” 🤷\n    "] "\n  "] "\n"])

(defn input-example []
  [:.
   [:input {:data-bind "input"}]
   [:.
    [:.bg-green-800 {:data-show "$input != ''"} "NOT EMPTY"]
    [:.bg-red-800 {:data-show "$input == ''"} "EMPTY"]]
   [:. {:data-class-hidden "$input == ''"} "howdy"]
   [:. {:data-text "$input"} "!"]
   [:. {:data-text "$input.toUpperCase()"}]
   [:. {:data-computed-repeated "$input.repeat(2)"}
    [:. {:data-text "$repeated"}]]
   [:button.btn.btn-primary
    {:data-attr-disabled "$input == ''"}
    "SAVE"]
   ;; <button data-on-click="$input = ''">Reset</button>
   [:button.btn.btn-primary {:data-on-click "$input = ''"}
    "Reset"]])

(defn top [content]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:title "HTMX w/ Clojure"]
    [:script "tailwind.config = {darkMode:'class'}"]
    (if (c/dev?)
      [:script {:src "https://cdn.tailwindcss.com"}]
      (->hiccup "<link href=\"https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css\" rel=\"stylesheet\">"))
    (->hiccup "<script type=\"module\" src=\"https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-beta.11/bundles/datastar.js\"></script>")
    [:script {:src "https://unpkg.com/htmx.org@1.7.0"}]]
   [:body.bg-white.dark:bg-gray-800.text-black.dark:text-white
    content
    [:hr]
    [signal-example]
    [:hr]
    [input-example]]])
