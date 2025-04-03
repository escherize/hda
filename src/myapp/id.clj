(ns myapp.id
  (:import (java.math BigInteger)
           (java.security SecureRandom)))

(def ^:const base58-alphabet
  "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")

(defn encode-base58
  "Encodes a BigInteger into a Base58 string."
  [^BigInteger num]
  (let [base (BigInteger/valueOf 58)]
    (loop [n num s ""]
      (if (.equals n BigInteger/ZERO)
        (if (empty? s)
          (str (first base58-alphabet))
          s)
        (let [[quotient remainder] (.divideAndRemainder n base)]
          (recur quotient (str (nth base58-alphabet (.intValue remainder)) s)))))))

(defn generate-base58-id
  "Generates a random Base58 encoded id using a 128-bit number."
  []
  (let [random-bytes (byte-array 16)  ; 16 bytes = 128 bits
        sr (SecureRandom.)]
    (.nextBytes sr random-bytes)
    (encode-base58 (BigInteger. 1 random-bytes))))

(defn id
  "Generates a magic link id with a prefix."
  ([] (id "id"))
  ([prefix]
   (str prefix "_" (generate-base58-id))))
