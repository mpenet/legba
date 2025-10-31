(ns s-exp.legba.openapi-schema
  (:require [s-exp.legba.json :as json]
            [s-exp.legba.json-pointer :as json-pointer]
            [s-exp.legba.json-schema :as json-schema])
  (:import (com.fasterxml.jackson.databind JsonNode)
           (com.networknt.schema
            Schema
            SchemaRegistry
            SchemaRegistry$Builder
            SchemaRegistryConfig
            InputFormat
            OutputFormat
            SchemaLocation)
           (com.networknt.schema.dialect Dialects)
           (com.networknt.schema.path PathType)))

(set! *warn-on-reflection* true)

(def schema-registry-config
  "Default reusable `SchemaRegistryConfig` instance.

  This configures the validator to:
  - Enable JSON Schema reference preloading and caching
  - Enable format assertions (per the JSON Schema spec)
  - Set maximum reference nesting depth to 40
  - Handle `nullable` fields correctly
  - Use `JSON_PATH` for error paths in validation results

  This config can be reused for schema load/validation for performance and consistency."
  (let [b (SchemaRegistryConfig/builder)]
    (doto b
      (.preloadSchema true)
      (.cacheRefs true)
      (.formatAssertionsEnabled true)
      (.pathType PathType/JSON_PATH))
    (.build b)))

(defn get-schema
  "Returns json-schema from schema-registry at `schema-uri`/`json-pointer`"
  ^Schema
  [^SchemaRegistry schema-registry schema-uri json-pointer]
  (.getSchema schema-registry
              (.resolve (SchemaLocation/of schema-uri)
                        (str "#" json-pointer))))

(defn load-schema
  "Loads JSON or YAML schema from `schema-uri` and returns
  map (of :openapi-schema, :schema-uri, :schema-registry) that contains all the
  necessary information to perform `validate!` calls later (minus a JSON
  pointer)."
  [^String schema-uri]
  (let [schema-registry (SchemaRegistry/withDialect
                         (Dialects/getOpenApi31)
                         (fn [^SchemaRegistry$Builder builder]
                           (doto builder
                             (.schemaCacheEnabled true)
                             (.schemaRegistryConfig schema-registry-config))))
        openapi-schema (-> schema-registry
                           (get-schema schema-uri "")
                           .getSchemaNode
                           (json/json-node->clj {:key-fn identity})
                           (json-pointer/annotate-tree))]
    {:openapi-schema openapi-schema
     :schema-uri schema-uri
     :schema-registry schema-registry}))

(defn validate
  "Validates a `val` against `schema`"
  [{:as _schema :keys [schema-uri schema-registry]}
   sub-schema val
   & {:as _opts
      :keys [validation-result]
      :or {validation-result json-schema/validation-result}}]
  (let [ptr (:json-pointer (meta sub-schema))
        schema (get-schema schema-registry schema-uri ptr)]
    (validation-result
     (if (instance? JsonNode val)
       (.validate schema ^JsonNode val
                  OutputFormat/RESULT)
       (.validate schema (or ^String val "")
                  InputFormat/JSON
                  OutputFormat/RESULT)))))
