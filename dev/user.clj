(ns user
  (:require [myapp.core :as core]))

(defn -main [& args]
  (apply core/-main args))
