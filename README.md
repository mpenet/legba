# Legba 

<img src="https://github.com/user-attachments/assets/7b36b294-8ada-4ef6-bbcc-4e9be4b101f7" width="100" height="100" style="float:left;">

*Legba* is a library aimed at building **fully OpenApi 3.1 compliant** services
in Clojure.

## Its goals:

* Provide **rock solid**, **full coverage** of OpenAPI 3.1 spec

* Support all the bells & whistles like `$refs`, conditionals, etc

* **[Great
  performance](https://www.creekservice.org/json-schema-validation-comparison/performance)**,
  being built on
  [networknt/json-schema-validator](https://github.com/networknt/json-schema-validator),
  which consistently ranks on the top of the java based json-schema validator.

* Be server adapter agnostic, it should be **usable with any RING compliant
  server adapter**
  
* Provide building blocks to **build your own routing schemes** or **plug onto
  any existing router**
  
* But **also provide a default, easy to use single handler entry point** with
  routing for a given schema, built on the aformentioned primitives

* Provide detailed, informative and customizable **error messages**

## How does it work

Legba provides simple paths to run an OpenAPI based server. 

You can either:

* Rely on `s-exp.legba/openapi-handler`: from an OpenAPI file and a map of
  `[method path]` -> `handler` matching the routes of the schema returns a
  single handler that will manage routing and perform validation and marshalling
  of the data according to the schema. Routing is performed via reitit in this
  case. This handler can simply be plugged to a RING server adapter and you're
  good to go.
  
* Or use `s-exp.legba.handler/openapi-routes`: from a map of `[method path]` ->
  `handler` will return a new map of `[method path]` -> `openapi-handler`. From
  this map you can then plug the openapi handlers in any routing solution.
  
## Installation 

For now it's tools.deps only until an alpha is out.

```clj 
com.s-exp/legba {:git/url "https://github.com/mpenet/legba.git" :git/sha "..."}
```

## Usage 

``` clj
(require '[s-exp.legba :as l])

(l/openapi-handler {[:get "/item/{itemId}"]
                    (fn [_request]
                      {...})
                    [:get "/items"]
                    (fn [_request]
                      {...})
                    [:get "/search"]
                    (fn [_request]
                      {...})
                    [:post "/items"]
                    (fn [_request]
                      {..})}
                   ;; path to a resource file
                   :schema "schema/oas/3.1/catalog.json"
                   ;; {...} ; options
                   )
```

There's also an extra argument with options:

* `:not-found-response` - defaults to `{:status 404 :body "Not found"}`

* `:key-fn` - Control map keys decoding when turning jackson JsonNodes to clj
  data for the handler - default to `keyword`
  
* `:query-string-params-key` - where to find the decoded query-string
  parameters - defaults to `:params`
  
* `:validation-result` - function that controls how to turn
  `com.networknt.schema.ValidationResult` into a clj -> json response. Defaults
  to `s-exp.legba.schema/validation-result`
  
* `:extra-routes` - extra routes to be passed to the underlying reitit router
  (using `{:syntax :bracket}`)
  
  
### Notes

You don't have to do any JSON marshaling, if the content-type is of
application/json type we will read data as such, same goes for writing. Given
the validation library needs the data to be parsed, we preserve this work and
re-use the parsed content.

### Documentation

[API docs](API.md)

## License

Copyright © 2024-2025 Max Penet

Distributed under the Eclipse Public License version 1.0.
