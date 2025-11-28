(ns s-exp.legba.json
  "Simple utils to convert to and from jsonNode"
  (:require [jsonista.core :as jsonista])
  (:import
   (com.fasterxml.jackson.databind ObjectMapper
                                   JsonNode
                                   SerializationFeature
                                   MapperFeature)
   (com.fasterxml.jackson.databind.node ObjectNode
                                        ArrayNode
                                        NullNode
                                        MissingNode
                                        LongNode
                                        IntNode
                                        DoubleNode
                                        FloatNode
                                        BooleanNode
                                        TextNode)
   (java.util Iterator)))

(set! *warn-on-reflection* true)

(defn set-mapper-defaults!
  "Sets sane defaults on jsonista, without this jsonista will do mad stuff such as
  serializing POJO fields as json attributes"
  [object-mapper]
  (doto object-mapper
    (.configure SerializationFeature/FAIL_ON_EMPTY_BEANS false)
    (.configure MapperFeature/AUTO_DETECT_GETTERS false)
    (.configure MapperFeature/AUTO_DETECT_IS_GETTERS false)
    (.configure MapperFeature/AUTO_DETECT_SETTERS false)
    (.configure MapperFeature/AUTO_DETECT_FIELDS false)
    (.configure MapperFeature/DEFAULT_VIEW_INCLUSION false)))

(def json-mapper-default
  (-> (jsonista/object-mapper)
      set-mapper-defaults!))

(defn- iterator->reducible
  [^Iterator iter]
  (when iter
    (reify
      Iterable
      (iterator [_] iter)
      Iterator
      (hasNext [_] (.hasNext iter))
      (next [_] (.next iter))
      (remove [_] (.remove iter))
      clojure.lang.IReduceInit
      (reduce [_ f val]
        (loop [ret val]
          (if (.hasNext iter)
            (let [ret (f ret (.next iter))]
              (if (reduced? ret)
                @ret
                (recur ret)))
            ret)))
      clojure.lang.Sequential)))

(defprotocol JsonNodeToClj
  (-json-node->clj [node opts]))

(extend-protocol JsonNodeToClj
  nil
  (-json-node->clj [_node _opts]
    nil)

  NullNode
  (-json-node->clj [_node _opts]
    nil)

  MissingNode
  (-json-node->clj [_node _opts]
    nil)

  ObjectNode
  (-json-node->clj [node opts]
    (let [{:as _opts :keys [key-fn]
           :or {key-fn identity}} opts]
      (persistent!
       (reduce-kv (fn [m k v]
                    (assoc! m
                            (key-fn k)
                            (-json-node->clj v opts)))
                  (transient {})
                  (iterator->reducible (.fields node))))))

  ArrayNode
  (-json-node->clj [node opts]
    (into []
          (map #(-json-node->clj % opts))
          node))

  IntNode
  (-json-node->clj [node _opts]
    (.asInt node))

  LongNode
  (-json-node->clj [node _opts]
    (.asLong node))

  DoubleNode
  (-json-node->clj [node _opts]
    (.asDouble node))

  FloatNode
  (-json-node->clj [node _opts]
    (.floatValue node))

  BooleanNode
  (-json-node->clj [node _opts]
    (.asBoolean node))

  TextNode
  (-json-node->clj [node _opts]
    (.asText node))

  Object
  (-json-node->clj [node _opts]
    (throw (ex-info (str "Unsupported JsonNodeType: " (.getNodeType ^JsonNode node))
                    {:node node}))))

(defn json-node->clj
  "Takes a Jackson JsonNode returns an equivalent clj datastructure.
  `:key-fn` controls how map-entries keys are decoded, defaulting to `keyword`"
  ([node]
   (-json-node->clj node {:key-fn keyword}))
  ([node opts]
   (-json-node->clj node opts)))

(defn str->json-node
  "Takes a json-str String and returns a Jackson JsonNode"
  [^String json-str]
  (when json-str
    (.readTree ^ObjectMapper json-mapper-default json-str)))

(defn json-node->str
  "Takes a Jackson JsonNode and returns an equivalent String value"
  [^JsonNode json-node]
  (.writeValueAsString ^ObjectMapper json-mapper-default json-node))

(defn clj->json-node
  "Takes a clj value and converts it to a Jackson JsonNode"
  [x]
  (.valueToTree ^ObjectMapper json-mapper-default x))

(defn json-content-type?
  "Returns true if `content-type` is `application/json`"
  [content-type]
  (re-find #"(?i)application/json(:?[;]|$)"
           content-type))
