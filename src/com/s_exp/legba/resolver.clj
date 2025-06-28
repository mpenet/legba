(ns s-exp.legba.resolver
  "We don't really need this as we expand the full schema with
  io.swagger.v3.parser.OpenAPIV3Parser. Could be nice to impelement if we want
  to make it standalone and free of any json parser.

  FIXME not complete"
  (:require [clojure.string :as str]
            [s-exp.legba.json-pointer :as jsp]))

(defn ref-type [ref]
  (case (nth ref 0)
    \# :local
    \/ :relative
    (if (str/starts-with? ref "http")
      :absolute
      (throw (ex-info "Invalid ref pointer"
                      {:type :s-exp.legba.resolver/ref-type-error
                       :ref ref})))))

(defn local-ref
  [ref]
  (java.net.URLDecoder/decode (subs ref 1)))

(defn resolve-ref
  [schema ref ctx]
  (case (ref-type ref)
    :local (jsp/query schema (local-ref ref))
    ;; :relative //
    (jsp/query schema ref)))

(defn expand
  ([schema]
   (expand schema schema {}))
  ([schema ctx]
   (expand schema schema ctx))
  ([schema node ctx]
   (cond
     (or (set? node) (sequential? node))
     (into (empty node)
           (map #(expand schema % ctx))
           node)

     (map? node)
     (let [$id (get node "$id")
           $ref (get node "$ref")
           ctx (cond-> ctx
                 $id
                 (assoc :base-uri $id))]
       (if $ref
         (expand schema (resolve-ref schema $ref ctx) ctx)
         (reduce-kv (fn [m k v]
                      (assoc m
                             k
                             (expand schema v ctx)))
                    (empty node)
                    node)))

     :else node)))

;; (def doc
;;   {"$id" "http://foo.com/"
;;    "foo" {"$ref" "#/bar/baz"}
;;    "bar" {"baz" {"yolo" 1}}})

;; ;; (expand doc)

;; (expand doc {})
