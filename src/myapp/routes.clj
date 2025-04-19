(ns myapp.routes
  (:require [reitit.ring :as rt]
            [myapp.views :as views]
            [ring.util.response :refer [response redirect]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [myapp.db :as db]
            [mount.core :refer [defstate]]
            [huff2.core :as h]
            [clojure.string :as str]))

(defn page [content]
  (h/page (views/top content)))

(defn html [& content]
  (str (h/html [:<> content])))

;; User authentication middleware using standard Ring conventions
(defn wrap-authentication
  "Standard Ring middleware for user authentication from token.
   Gets token from authorization header or cookie."
  [handler]
  (fn [req]
    (let [token (or (when-let [auth-header (get-in req [:headers "authorization"])]
                      (when (str/starts-with? auth-header "Bearer ")
                        (subs auth-header 7)))
                    (get-in req [:cookies "auth-token" :value]))]
      (if-not token
        (handler req)  ;; No token, continue to next middleware
        (if-let [user (db/auth-token->user @db/conn token)]
          (handler (assoc req :identity user)) ;; Using :identity as standard Ring convention
          (handler req))))))  ;; Invalid token, but still continue (authenticated routes can check later)

(defn authenticated?
  "Predicate to check if request has authenticated user"
  [req]
  (boolean (:identity req)))

(defn wrap-require-authentication
  "Middleware that ensures a request is authenticated.
   If not, it returns a 401 Unauthorized response."
  [handler]
  (fn [req]
    (if (authenticated? req)
      (handler req)
      (-> (response {:error "Authentication required"})
          (assoc :status 401)))))

(defn list-users [_]
  (response (html [:h1 "userz"])))

(let [*a (atom 0)] (defn inc! [] (swap! *a inc)))

(defn- user-home [user]
  [:div.space-y-6
   [:div.bg-indigo-50.dark:bg-indigo-900.p-6.rounded-lg.border-l-4.border-indigo-500
    [:div.flex.items-center.space-x-4
     [:div.flex-shrink-0
      [:div.h-12.w-12.rounded-full.bg-indigo-600.flex.items-center.justify-center
       [:span.text-xl.font-bold.text-white (-> user :user/screen-name first str)]]]

     [:div.flex-1
      [:h2.text-2xl.font-bold.text-indigo-800.dark:text-indigo-200
       (str "Welcome, " (:user/screen-name user))]
      [:p.text-indigo-600.dark:text-indigo-300 "Glad to see you here today!"]]]]

   [:div.flex.items-center.justify-between.mt-8.pt-6.border-t.border-gray-200.dark:border-gray-700
    [:div.text-sm.text-gray-500.dark:text-gray-400 "Signed in as " [:span.font-medium (:user/email user)]]
    [:a.inline-flex.items-center.px-4.py-2.border.border-transparent.text-sm.font-medium.rounded-md.text-white.bg-red-600.hover:bg-red-700.focus:outline-none.focus:ring-2.focus:ring-offset-2.focus:ring-red-500.transition-colors.duration-300
     {:href "/logout"}
     [:svg.mr-2.-ml-1.h-4.w-4
      {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 20 20" :fill "currentColor"}
      [:path {:fill-rule "evenodd" :d "M3 3a1 1 0 00-1 1v12a1 1 0 001 1h12a1 1 0 001-1V9.5a.5.5 0 01.5-.5h1a.5.5 0 01.5.5V16a3 3 0 01-3 3H3a3 3 0 01-3-3V4a3 3 0 013-3h12a3 3 0 013 3v2.5a.5.5 0 01-.5.5h-1a.5.5 0 01-.5-.5V4a1 1 0 00-1-1H3z" :clip-rule "evenodd"}]
      [:path {:fill-rule "evenodd" :d "M19 10a.5.5 0 01-.5.5h-13a.5.5 0 010-1h13a.5.5 0 01.5.5z" :clip-rule "evenodd"}]
      [:path {:fill-rule "evenodd" :d "M16 10a.5.5 0 01-.5.5h-1a.5.5 0 01-.5-.5V3a.5.5 0 01.5-.5h1a.5.5 0 01.5.5v7z" :clip-rule "evenodd"}]]
     "Logout"]]])

(defn- sign-in []
  [:div.flex.flex-col.items-center.justify-center.space-y-6.py-12
   [:div.text-center
    [:svg.mx-auto.h-16.w-16.text-indigo-500
     {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
     [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "1.5" :d "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"}]]
    [:h2.mt-4.text-xl.font-bold.text-gray-700.dark:text-gray-200 "Sign in to access your account"]
    [:p.mt-2.text-gray-500.dark:text-gray-400 "Create an account or sign in to get started"]]
   [:a.inline-flex.items-center.px-6.py-3.border.border-transparent.text-base.font-medium.rounded-md.text-white.bg-indigo-600.hover:bg-indigo-700.focus:outline-none.focus:ring-2.focus:ring-offset-2.focus:ring-indigo-500.transition-colors.duration-300
    {:href "/login"}
    [:svg.mr-2.-ml-1.h-5.w-5
     {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 20 20" :fill "currentColor"}
     [:path {:fill-rule "evenodd" :d "M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" :clip-rule "evenodd"}]]
    "Sign In / Register"]
   [:p.text-sm.text-gray-500.dark:text-gray-400 "No account needed - sign up instantly"]])

(defn- dashboard-header []
  [:div.flex.items-center.justify-between.border-b.border-gray-200.dark:border-gray-700.pb-4.mb-4
   [:h1.text-3xl.font-bold.text-gray-900.dark:text-white "Dashboard"]
   [:div.text-sm.text-gray-500.dark:text-gray-400 (str "Visitors: " (inc!))]])

(defn- dashboard-container [content]
  [:div.min-h-screen.bg-gradient-to-br.from-indigo-400.to-purple-500
   [:div.container.mx-auto.px-4.py-12
    [:div.max-w-3xl.mx-auto.bg-white.dark:bg-gray-800.rounded-xl.shadow-2xl.overflow-hidden
     [:div.px-6.py-8.sm:px-10
      (dashboard-header)
      content]]]])

(defn home [req]
  (response
    (page
      (dashboard-container
        (if-let [user (:identity req)]
          (user-home user)
          (sign-in))))))

(defn login-error [error-message]
  (page
    [:div.min-h-screen.flex.items-center.justify-center.bg-gradient-to-r.from-red-500.to-pink-600.py-12.px-4.sm:px-6.lg:px-8
     [:div.max-w-md.w-full.space-y-8.bg-white.dark:bg-gray-800.p-10.rounded-xl.shadow-2xl
      [:div.text-center
       [:svg.mx-auto.h-16.w-16.text-red-600
        {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24" :stroke "currentColor"}
        [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "1.5" 
                :d "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"}]]
       
       [:h2.mt-4.text-2xl.font-extrabold.text-gray-900.dark:text-white "Login Error"]
       [:p.mt-2.text-red-600.dark:text-red-400.text-lg error-message]]
      
      [:div.mt-8
       [:div.rounded-md.shadow-sm
        [:a.group.relative.w-full.flex.justify-center.py-3.px-4.border.border-transparent.text-sm.font-medium.rounded-md.text-white.bg-indigo-600.hover:bg-indigo-700.focus:outline-none.focus:ring-2.focus:ring-offset-2.focus:ring-indigo-500.transition-colors.duration-300
         {:href "/login"}
         [:span.absolute.left-0.inset-y-0.flex.items-center.pl-3
          [:svg.h-5.w-5.text-indigo-500.group-hover:text-indigo-400
           {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 20 20" :fill "currentColor"}
           [:path {:fill-rule "evenodd"
                   :d "M10 18a8 8 0 100-16 8 8 0 000 16zm.707-10.293a1 1 0 00-1.414-1.414l-3 3a1 1 0 000 1.414l3 3a1 1 0 001.414-1.414L9.414 11H13a1 1 0 100-2H9.414l1.293-1.293z"
                   :clip-rule "evenodd"}]]]
         "Return to Login"]]]
      
      [:div.mt-4.text-center.text-sm.text-gray-500.dark:text-gray-400
       "Need help? " [:a.text-indigo-600.hover:text-indigo-500.dark:text-indigo-400 {:href "/"} "Contact support"]]]]))

(defn login-page [_]
  (response (page (views/login))))

(defn login-handler
  "Handle login/registration form submission.
   Authenticates user and sets session cookie on success."
  [request]
  (try
    (let [params (:params request)
          email (get params "email")
          password (get params "password")
          screen-name (get params "screen-name")]
      (if (and email password)
        (let [result (try
                       (db/authenticate db/conn email password screen-name)
                       (catch Exception e
                         {:error (.getMessage e)}))]
          (if (:error result)
            (-> (response (login-error (:error result)))
                (assoc :status 400))
            (-> (redirect "/")
                (assoc-in [:cookies "auth-token"] 
                          {:value (:auth-token result)
                           :max-age 86400
                           :path "/"
                           :http-only true}))))
        (-> (response (login-error "Email and password are required"))
            (assoc :status 400))))
    (catch Exception e
      (-> (response (login-error (str "An unexpected error occurred: " (.getMessage e))))
          (assoc :status 500)))))

(defn logout-handler
  "Handles user logout by invalidating the auth token cookie"
  [_]
  (-> (redirect "/")
      (assoc-in [:cookies "auth-token"]
                {:value "" :max-age 0 :path "/"})))

(defstate app
  :start
  (-> (rt/ring-handler
        (rt/router
          [""
           ["/" {:get home}]
           ["/login" {:get login-page
                      :post login-handler}]
           ["/logout" {:get logout-handler}]
           ["/api/users" {:get (comp wrap-require-authentication list-users)}]])
        (rt/create-default-handler))
      wrap-authentication
      wrap-cookies
      wrap-keyword-params
      wrap-params))
