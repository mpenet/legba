(ns s-exp.legba.schema
  (:require [clojure.java.io :as io]
            [jsonista.core :as jsonista]
            [s-exp.legba.json-pointer :as json-pointer])
  (:import (com.fasterxml.jackson.databind JsonNode)
           (com.networknt.schema JsonSchemaFactory
                                 JsonSchemaFactory$Builder
                                 JsonSchema
                                 SchemaValidatorsConfig
                                 InputFormat
                                 OutputFormat
                                 SchemaLocation)
           (com.networknt.schema ValidationResult
                                 ValidationMessage
                                 SpecVersion$VersionFlag
                                 PathType)
           (com.networknt.schema.oas OpenApi31)
           (io.swagger.v3.core.util Json)
           (io.swagger.v3.parser OpenAPIV3Parser)
           (io.swagger.v3.parser.core.models ParseOptions)))

(set! *warn-on-reflection* true)

(def schema-validator-config
  (doto (SchemaValidatorsConfig.)
    (.setPreloadJsonSchema true)
    (.setCacheRefs true)
    (.setFormatAssertionsEnabled true)
    (.setPreloadJsonSchemaRefMaxNestingDepth 40)
    (.setPathType PathType/JSON_PATH)))

(defn load-schema
  [^String schema-resource-file]
  (let [schema-str (slurp (io/resource schema-resource-file))
        parse-options (doto (ParseOptions.)
                        (.setResolveFully true)
                        (.setValidateExternalRefs true))
        json-schema-raw (-> (.readContents (OpenAPIV3Parser.)
                                           schema-str
                                           nil
                                           parse-options)
                            (Json/pretty)
                            (jsonista/read-value)
                            (get "openAPI"))
        openapi-schema (json-pointer/annotate-tree json-schema-raw)]
    {:openapi-schema openapi-schema
     :schema-resource-file schema-resource-file
     :json-schema-factory
     (JsonSchemaFactory/getInstance
      SpecVersion$VersionFlag/V202012
      (fn [^JsonSchemaFactory$Builder builder]
        (doto builder
          (.metaSchema (OpenApi31/getInstance))
          (.defaultMetaSchemaIri (.getIri (OpenApi31/getInstance)))
          (.enableSchemaCache true))))}))

(defn get-schema
  ^JsonSchema [schema schema-resource-file ptr]
  (.getSchema ^JsonSchemaFactory (:json-schema-factory schema)
              (.resolve (SchemaLocation/of (format "classpath://%s" schema-resource-file))
                        (str "#" ptr))
              ^SchemaValidatorsConfig schema-validator-config))

(defn validation-result
  [^ValidationResult r]
  (let [vms (.getValidationMessages r)]
    (when-not (empty? vms)
      (into []
            (map (fn [^ValidationMessage m]
                   {:type (.getType m)
                    :path (.toString (.getInstanceLocation m))
                    :error (.getError m)
                    :message (.getMessage m)}))
            vms))))

(defn validate!
  [{:as schema :keys [schema-resource-file]} sub-schema val
   & {:as _opts
      :keys [validation-result]
      :or {validation-result validation-result}}]
  (let [ptr (:json-pointer (meta sub-schema))
        ^JsonSchema schema (get-schema schema schema-resource-file ptr)]
    (validation-result
     (if (instance? JsonNode val)
       (.validate schema ^JsonNode val
                  OutputFormat/RESULT)
       (.validate schema (or ^String val "")
                  InputFormat/JSON
                  OutputFormat/RESULT)))))
