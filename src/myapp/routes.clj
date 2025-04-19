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

(defn get-token-from-header [req]
  (when-let [auth-header (get-in req [:headers "authorization"])]
    (when (str/starts-with? auth-header "Bearer ")
      (subs auth-header 7))))

(defn get-token-from-cookie [req]
  (get-in req [:cookies "auth-token" :value]))

(defn wrap-auth [handler]
  (fn [req]
    (let [token (or (get-token-from-header req)
                    (get-token-from-cookie req))]
      (if token
        (if-let [user (db/auth-token->user @db/conn token)]
          (handler (assoc req :user user))
          (-> (response {:error "Invalid authentication token"})
              (assoc :status 401)))
        (handler req)))))

(defn auth-required [handler]
  (fn [req]
    (if (:user req)
      (handler req)
      (-> (response {:error "Authentication required"})
          (assoc :status 401)))))

(defn list-users [_]
  (response (html [:h1 "userz"])))

(let [*a (atom 0)] (defn inc! [] (swap! *a inc)))

(defn home [req]
  (response
    (page
      [:div
       [:h1 "hi guyse " (inc!)]
       (if-let [user (:user req)]
         [:div 
          [:p "Welcome, " (:user/screen-name user)]
          [:a.btn.btn-primary {:href "/logout"} "Logout"]]
         [:a.btn.btn-primary {:href "/login"} "Login"])])))

(defn login-page [_]
  (response (page (views/login))))

(defn login-handler [request]
  (println "======================================================")
  (println "LOGIN REQUEST STARTED")
  (println "======================================================")
  (try
    (let [params (:params request)
          form-params (:form-params request)
          email (or (get params "email") (get form-params "email"))
          password (or (get params "password") (get form-params "password"))
          screen-name (or (get params "screen-name") (get form-params "screen-name"))]
      (println "PARAMS: " (pr-str params))
      (println "FORM-PARAMS: " (pr-str form-params))
      (println "EMAIL: " (pr-str email))
      (println "PASSWORD: " (pr-str password))
      (println "SCREEN-NAME: " (pr-str screen-name))
      
      (if (and email password)
        (do
          (println "AUTHENTICATING...")
          (let [existing-user (db/email->user @db/conn email)]
            (println "EXISTING USER: " (pr-str existing-user))
            (if existing-user
              (println "USER EXISTS - CHECKING PASSWORD")
              (println "USER DOES NOT EXIST - WILL CREATE NEW USER")))
          
          (let [result (try
                         (db/authenticate db/conn email password screen-name)
                         (catch Exception e
                           (println "AUTHENTICATION ERROR: " (.getMessage e))
                           {:error (.getMessage e)}))]
            (println "AUTH RESULT: " (pr-str result))
            
            (if (:error result)
              (-> (response (page 
                            [:div.alert.alert-danger 
                              [:h1 "Login Error"] 
                              [:p (:error result)]
                              [:a.btn.btn-primary {:href "/login"} "Try Again"]]))
                  (assoc :status 400))
              (-> (redirect "/")
                  (assoc-in [:cookies "auth-token"] {:value (:auth-token result)
                                                  :max-age 86400
                                                  :path "/"
                                                  :http-only true})))))
        (-> (response (page
                      [:div.alert.alert-danger
                        [:h1 "Login Error"]
                        [:p "Email and password are required"]
                        [:a.btn.btn-primary {:href "/login"} "Try Again"]]))
            (assoc :status 400))))
  (catch Exception e
    (println "UNEXPECTED ERROR: " (.getMessage e))
    (println (.printStackTrace e))
    (-> (response (page
                  [:div.alert.alert-danger
                    [:h1 "Login Error"] 
                    [:p "An unexpected error occurred: " (.getMessage e)]
                    [:a.btn.btn-primary {:href "/login"} "Try Again"]]))
        (assoc :status 500)))))

(defn logout-handler [_]
  (-> (redirect "/")
      (assoc-in [:cookies "auth-token"] {:value ""
                                        :max-age 0
                                        :path "/"})))

(defstate app
  :start
  (-> (rt/ring-handler
        (rt/router
          [""
           ["/" {:get home}]
           ["/login" {:get login-page
                      :post login-handler}]
           ["/logout" {:get logout-handler}]
           ["/api/users" {:get (comp auth-required list-users)}]])
        (rt/create-default-handler))
      wrap-auth
      wrap-cookies
      wrap-keyword-params
      wrap-params))
