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

(defn encode-token
  "Encodes a single reference token according to RFC 6901."
  [token]
  (-> token
      (str/replace "~" "~0")
      (str/replace "/" "~1")))

(defn pointer-append
  "Adds value to existing json-pointer and returns a new one"
  [pointer val]
  (str pointer "/" (encode-token val)))

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

(defn add-pointer
  "Adds :json-pointer metadata to `node`"
  [node pointer]
  (vary-meta node assoc :json-pointer pointer))

(defn annotate-tree
  "Walks tree and add `:json-pointer` metadata to every node"
  ([node]
   (annotate-tree node ""))
  ([node pointer]
   (cond
     (map? node)
     (-> (reduce-kv (fn [m k v]
                      (assoc m
                             k
                             (annotate-tree v
                                            (pointer-append pointer k))))
                    {}
                    node)
         (add-pointer pointer))
     (sequential? node)
     (-> (into []
               (map-indexed (fn [idx x]
                              (annotate-tree x
                                             (pointer-append pointer
                                                             idx))))
               node)
         (add-pointer pointer))
     :else node)))
