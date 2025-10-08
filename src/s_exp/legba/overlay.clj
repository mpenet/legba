(ns s-exp.legba.overlay
  "Provides overlay manipulation utilities for OpenAPI schemas, enabling dynamic
  updates or removals on schema documents using OpenAPI Overlay instructions.
https://spec.openapis.org/overlay/latest.html  "
  (:require [jsonista.core :as json])
  (:refer-clojure :exclude [update apply])
  (:import (com.jayway.jsonpath Configuration
                                DocumentContext
                                JsonPath
                                Option
                                PathNotFoundException
                                Predicate)))

(set! *warn-on-reflection* true)

(def ^Configuration conf
  (doto (Configuration/defaultConfiguration)
    (.addOptions (into-array Option [Option/DEFAULT_PATH_LEAF_TO_NULL
                                     Option/SUPPRESS_EXCEPTIONS]))))

(def ^:private preds (into-array Predicate []))

(defn- apply-update
  "Apply an update to the OpenAPI schema at the given target path.

  Parameters:
  - openapi: The OpenAPI schema data structure (map)
  - target: JSONPath expression indicating where the update applies
  - update: The value to set at the target path

  Returns the updated OpenAPI schema."
  [^DocumentContext openapi ^String target update]
  (try
    (.set openapi (JsonPath/compile target preds) update)
    (catch PathNotFoundException _e
      openapi)))

(defn- apply-remove
  "Remove data from the OpenAPI schema at the given target path.

  Parameters:
  - openapi: The OpenAPI schema data structure (map)
  - target: JSONPath expression indicating where the remove applies
  - remove: Boolean; if true, excise data at the target path

  Returns the updated OpenAPI schema."
  [^DocumentContext openapi ^String target remove]
  (try
    (cond-> openapi
      remove
      (.delete (JsonPath/compile target preds)))
    (catch PathNotFoundException _e
      openapi)))

(defn apply
  "Apply overlay actions (update/remove) to the given OpenAPI schema string.

  Parameters:
  - openapi-string: String, JSON representation of the OpenAPI schema
  - overlay-string: String, JSON representation of the overlay instructions

  Returns the OpenAPI schema with all overlay actions applied as String"
  [^String openapi-string overlay-string]
  (let [overlay (json/read-value overlay-string)
        openapi (JsonPath/parse openapi-string)]
    (.jsonString ^DocumentContext
     (reduce (fn [openapi {:as _action :strs [target remove update]}]
               (cond-> openapi
                 update
                 (apply-update target update)
                 remove
                 (apply-remove target remove)))
             openapi
             (get overlay "actions")))))
