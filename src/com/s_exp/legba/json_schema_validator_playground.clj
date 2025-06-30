(ns com.s-exp.legba.json-schema-validator-playground
  (:import (com.networknt.schema JsonSchemaFactory
                                 SchemaValidatorsConfig
                                 InputFormat
                                 OutputFormat
                                 SchemaLocation)
           (com.networknt.schema SpecVersion$VersionFlag PathType)
           (com.networknt.schema.oas OpenApi30 OpenApi31)))

;; This creates a schema factory that will use Draft 2020-12 as the default if
;; $schema is not specified in the schema data. If $schema is specified in the
;; schema data then that schema dialect will be used instead and this version is
;; ignored.

;; (.getIri (OpenApi31/getInstance))

(def json-schema-factory
  (JsonSchemaFactory/getInstance
   SpecVersion$VersionFlag/V202012
   (fn [builder]
     (doto builder
       (.metaSchema (OpenApi31/getInstance))
       (.defaultMetaSchemaIri (.getIri (OpenApi31/getInstance)))))))

(def schema-validator-config
  (doto (SchemaValidatorsConfig.)
    (.setPreloadJsonSchema true)
    (.setCacheRefs true)
    (.setFormatAssertionsEnabled true)
    (.setTypeLoose true)
    (.setPathType PathType/JSON_POINTER)))

(def schema
  (.getSchema json-schema-factory

              ;; (SchemaLocation/of "classpath://schema/oas/3.0/petstore.json#/paths/~1pet/post/requestBody/Pet/content")
              ;; (SchemaLocation/of "classpath://schema/oas/3.0/petstore.json#/paths/~1pet/post/requestBody")
              ;; (SchemaLocation/of "classpath://schema/oas/3.1/petstore.json#/paths/~1pet/post/requestBody/content/application~1json/schema")
              (.resolve (SchemaLocation/of "classpath://schema/oas/3.1/petstore.json") "#/paths/~1pet/post/requestBody/content/application~1json/schema")
              schema-validator-config))

(.resolve (SchemaLocation/of "classpath://schema/oas/3.0/petstore.json")
          "#/paths/~1pet/post/requestBody/content/application~1json/schema")

(prn schema)

(do
  (def input "{\"name\": \"foo\", \"photoUrls\":[1]}")
  (.validate schema
             input
             InputFormat/JSON
             OutputFormat/DEFAULT))
