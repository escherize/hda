(ns myapp.view)

(defn kitchen-sink []
  [:div {:class "min-h-screen bg-base-200"}

   ;; Header
   [:header {:class "bg-base-100 shadow-md p-4"}
    [:div {:class "container mx-auto"}
     [:h1 {:class "text-3xl font-bold"} "Component Showcase"]]]

   ;; Main content area using a grid layout for component cards.
   [:main {:class "container mx-auto p-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8"}

    ;; --- Alert Card ---
    [:div {:class "card bg-base-100 shadow-md p-4"}
     [:h2 {:class "card-title text-xl mb-2"} "Alert"]
     [:div {:class "alert alert-info"}
      [:div "Info: Software update available!"]]
     [:div {:class "alert alert-success mt-2"}
      [:div "Success: Data saved successfully."]]]

    ;; --- Button Card ---
    [:div {:class "card bg-base-100 shadow-md p-4"}
     [:h2 {:class "card-title text-xl mb-2"} "Buttons"]
     (for [mod ["" "btn-outline" "btn-soft" "btn-active" "btn-ghost" "btn-link"]]
       [:div {:class "card bg-base-100 shadow-md p-4"}
        [:p {:class "card-title text-xl mb-2"} "Buttons: " mod]
        [:div {:class "space-x-2 space-y-2"}
         (for [[clazz title] [["btn btn-neutral" "Neutral"]
                              ["btn btn-primary" "Primary"]
                              ["btn btn-secondary" "Secondary"]
                              ["btn btn-accent" "Accent"]
                              ["btn btn-info" "Info"]
                              ["btn btn-success" "Success"]
                              ["btn btn-warning" "Warning"]
                              ["btn btn-error" "Error"]]]
           [:button {:class (str clazz " " mod)} title])]])]

    ;; --- Card Example ---
    [:div {:class "card w-full bg-base-100 shadow-md"}
     [:figure
      [:img {:src "https://via.placeholder.com/400x200" :alt "Card image"}]]
     [:div {:class "card-body"}
      [:h2 {:class "card-title"} "Card Title"]
      [:p "A concise description inside a card."]
      [:div {:class "card-actions justify-end"}
       [:button {:class "btn btn-primary"} "Action"]]]]

    ;; --- Form Inputs Card ---
    [:div {:class "card bg-base-100 shadow-md p-4"}
     [:h2 {:class "card-title text-xl mb-2"} "Form Inputs"]
     [:div {:class "form-control space-y-2"}
      [:label {:class "label"}
       [:span {:class "label-text"} "Username"]]
      [:input {:type "text" :placeholder "Enter username" :class "input input-bordered"}]
      [:label {:class "label"}
       [:span {:class "label-text"} "Email"]]
      [:input {:type "email" :placeholder "Enter email" :class "input input-bordered"}]]]

    ;; --- Navbar Card (Static Demo) ---
    [:div {:class "card bg-base-100 shadow-md p-4"}
     [:h2 {:class "card-title text-xl mb-2"} "Navbar Demo"]
     [:div {:class "navbar bg-base-100 shadow"}
      [:div {:class "flex-1"}
       [:a {:class "btn btn-ghost normal-case text-xl"} "Brand"]]
      [:div {:class "flex-none"}
       [:ul {:class "menu menu-horizontal p-0"}
        [:li [:a "Home"]]
        [:li [:a "About"]]
        [:li [:a "Contact"]]]]]]

    ;; --- Footer Card (Just for demo purposes) ---
    [:div {:class "card bg-base-100 shadow-md p-4"}
     [:h2 {:class "card-title text-xl mb-2"} "Footer Demo"]
     [:footer {:class "footer p-4 bg-base-200 text-center"}
      [:p "© 2025 Your Company"]]]]

   ;; Footer (global)
   [:footer {:class "bg-base-100 text-center p-4 mt-8"}
    [:p "© 2025 Your Company. All rights reserved."]]])
