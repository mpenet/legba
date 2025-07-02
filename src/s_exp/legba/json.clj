(ns s-exp.legba.json
  "Simple utils to convert to and from jsonNode"
  (:require [jsonista.core :as jsonista])
  (:import
   (com.fasterxml.jackson.databind ObjectMapper
                                   JsonNode)
   (com.fasterxml.jackson.databind.node JsonNodeType
                                        ObjectNode
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
  ([node]
   (-json-node->clj node {:key-fn keyword}))
  ([node opts]
   (-json-node->clj node opts)))

(defn str->json-node
  [^String json-str]
  (when json-str
    (.readTree ^ObjectMapper jsonista/default-object-mapper json-str)))

(defn json-node->str
  [^JsonNode json-node]
  (.writeValueAsString ^ObjectMapper jsonista/default-object-mapper json-node))

;; (json-node->str (str->json-node "{\"a\": \"1\", \"b\":[1,2], \"c\": 3}"))
;; (json-node->str (str->json-node "{\"a\": \"1\", \"b\":[1,2], \"c\": 3}"))
;; (json-node->clj (clj->json-node nil))

(defn clj->json-node
  [x]
  (.valueToTree ^ObjectMapper jsonista/default-object-mapper x))

;; (clj->json-node {:a 1 :b 2})

(defn json-content-type?
  [content-type]
  (re-find #"(?i)application/json(:?[;]|$)"
           content-type))
