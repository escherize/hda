(ns myapp.id
  (:import (java.math BigInteger)
           (java.security SecureRandom)))

(def ^:const base58-alphabet
  "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")

(def ^:const alphabet
  "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")

(defn id
  "Generates a magic id with a prefix."
  ([] (id "id" 22))
  ([prefix] (id prefix 22))
  ([prefix n]
   (str prefix (apply str (repeatedly n #(rand-nth base58-alphabet))))))

(defn mini []
  "Generates a mini id with a prefix."
  (id (rand-nth alphabet) 5))
