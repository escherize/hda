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
  [:. {:data-signals "{response: '', answer: 'bread'}" ;; json ok here
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

;; Login page components
(defn login-header []
  [:div.text-center
   [:h2.text-3xl.font-extrabold.text-gray-900.dark:text-white "Welcome"]
   [:p.mt-2.text-sm.text-gray-600.dark:text-gray-400 "Sign in or create your account"]])

(defn email-input []
  [:div
   [:label.sr-only {:for "email"} "Email address"]
   [:input.appearance-none.rounded-none.relative.block.w-full.px-3.py-2.border.border-gray-300.dark:border-gray-700.placeholder-gray-500.dark:placeholder-gray-400.text-gray-900.dark:text-white.rounded-t-md.focus:outline-none.focus:ring-indigo-500.focus:border-indigo-500.focus:z-10.sm:text-sm.bg-white.dark:bg-gray-700
    {:type "email" :id "email" :name "email" :placeholder "Email address"
     :data-bind "email" :required true :autocomplete "email"}]])

(defn password-input []
  [:div
   [:label.sr-only {:for "password"} "Password"]
   [:input.appearance-none.rounded-none.relative.block.w-full.px-3.py-2.border.border-gray-300.dark:border-gray-700.placeholder-gray-500.dark:placeholder-gray-400.text-gray-900.dark:text-white.focus:outline-none.focus:ring-indigo-500.focus:border-indigo-500.focus:z-10.sm:text-sm.bg-white.dark:bg-gray-700
    {:type "password" :id "password" :name "password" :placeholder "Password"
     :data-bind "password" :required true :autocomplete "current-password"}]])

(defn screen-name-input []
  [:div
   [:label.sr-only {:for "screen-name"} "Screen Name (optional)"]
   [:input.appearance-none.rounded-none.relative.block.w-full.px-3.py-2.border.border-gray-300.dark:border-gray-700.placeholder-gray-500.dark:placeholder-gray-400.text-gray-900.dark:text-white.rounded-b-md.focus:outline-none.focus:ring-indigo-500.focus:border-indigo-500.focus:z-10.sm:text-sm.bg-white.dark:bg-gray-700
    {:type "text" :id "screen-name" :name "screen-name" :placeholder "Screen Name (optional)"
     :data-bind "screenName"}]])

(defn login-info-text []
  [:div.text-sm.text-gray-600.dark:text-gray-400.p-4.bg-gray-100.dark:bg-gray-900.rounded-lg.space-y-2
   [:p "Enter your email and password to sign in. New users will be registered automatically."]
   [:p "Screen name is optional - one will be generated if not provided."]])

(defn login-button []
  [:div
   [:button.group.relative.w-full.flex.justify-center.py-2.px-4.border.border-transparent.text-sm.font-medium.rounded-md.text-white.bg-indigo-600.hover:bg-indigo-700.focus:outline-none.focus:ring-2.focus:ring-offset-2.focus:ring-indigo-500.transition-colors.duration-300.ease-in-out
    {:type "submit"
     :data-attr-disabled "$email == '' || $password == ''"}
    [:span.absolute.left-0.inset-y-0.flex.items-center.pl-3
     [:svg.h-5.w-5.text-indigo-500.group-hover:text-indigo-400 
      {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 20 20" :fill "currentColor" :aria-hidden "true"}
      [:path {:fill-rule "evenodd" :d "M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" :clip-rule "evenodd"}]]]
    "Sign in / Register"]])

(defn login-form []
  [:form.mt-8.space-y-6 {:action "/login" :method "post"}
   [:div.rounded-md.shadow-sm.-space-y-px
    (email-input)
    (password-input)
    (screen-name-input)]
   (login-info-text)
   (login-button)])

(defn login []
  [:div.min-h-screen.flex.items-center.justify-center.bg-gradient-to-r.from-purple-500.to-blue-600.py-12.px-4.sm:px-6.lg:px-8
   [:div.max-w-md.w-full.space-y-8.bg-white.dark:bg-gray-800.p-10.rounded-xl.shadow-2xl
    (login-header)
    (login-form)]])

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
    ;; Examples:
    ;; [:hr]
    ;; [signal-example]
    ;; [:hr]
    ;; [input-example]
    ]])
