(ns s-exp.legba.json-pointer
  "Implementation of https://datatracker.ietf.org/doc/html/rfc6901 parser and
  resolver"
  (:require [clojure.string :as str]))

(defn decode-token
  "Decodes a single reference token according to RFC 6901."
  [token]
  (-> token
      (str/replace "~1" "/")
      (str/replace "~0" "~")))

(defn parse-json-pointer
  "Parses a JSON Pointer string into a sequence of reference tokens."
  [pointer]
  (if (str/starts-with? pointer "/")
    (eduction (map decode-token)
              (str/split (subs pointer 1) #"/"))
    (case pointer
      "" []
      (throw (ex-info "Invalid JSON Pointer: must start with '/' or be empty"
                      {:type :com.s-exp.legba.json-pointer/invalid-pointer
                       :val pointer})))))

(defn query
  "Resolves a JSON Pointer against a given JSON data structure."
  [json-data pointer]
  (reduce (fn [data key]
            (if (map? data)
              (get data key)
              (if (and (sequential? data)
                       (re-matches #"\d+" key))
                (let [idx (parse-long key)]
                  (if (< idx (count data))
                    (nth data idx)
                    (throw (IndexOutOfBoundsException. (str "Index " key " out of bounds")))))
                (throw (IllegalStateException. (str "Cannot resolve key " key " in unsupported structure"))))))
          json-data
          (parse-json-pointer pointer)))

;; (def doc {"foo" ["bar", "baz"],
;;           "" 0,
;;           "a/b" 1,
;;           "c%d" 2,
;;           "e^f" 3,
;;           "g|h" 4,
;;           "i\\j" 5,
;;           "k\"l" 6,
;;           " " 7,
;;           "m~n" 8})

;; (def out
;;   {"" doc
;;    "/foo" ["bar", "baz"]
;;    "/foo/0" "bar"
;;    "/" 0
;;    "/a~1b" 1
;;    "/c%d" 2
;;    "/e^f" 3
;;    "/g|h" 4
;;    "/i\\j" 5
;;    "/k\"l" 6
;;    "/ " 7
;;    "/m~0n" 8})

;; (def out2
;;   {"#" doc
;;    "#/foo" ["bar", "baz"]
;;    "#/foo/0" "bar"
;;    "#/" 0
;;    "#/a~1b" 1
;;    "#/c%25d" 2
;;    "#/e%5Ef" 3
;;    "#/g%7Ch" 4
;;    "#/i%5Cj" 5
;;    "#/k%22l" 6
;;    "#/%20" 7
;;    "#/m~0n" 8})

;; (doseq [[pointer expected] out]
;;   (prn (= expected (query doc pointer))))

;; (doseq [[pointer expected] out2]
;;   (prn (= expected (query doc pointer))))
