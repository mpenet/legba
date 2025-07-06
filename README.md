# Legba 
<img src="https://github.com/user-attachments/assets/7b36b294-8ada-4ef6-bbcc-4e9be4b101f7" width="100" height="100" style="float:left;">

*Legba* is a library designed for building **fully OpenAPI 3.1 compliant**
services in Clojure.

Legba streamlines the creation of OpenAPI based servers by emphasizing the
**OpenAPI schema file as the foundation of your service definition**. Unlike other
libraries that generate the schema from routes & validation defined in a
separate DSL, Legba takes the opposite approach: **it uses your OpenAPI schema to
create the necessary routes and wraps your supplied handlers with OpenAPI
validation**.

Legba ensures that the final OpenAPI file exposed to your users remains
**unrestricted, reviewable, and editable**.

But feel free to leverage libraries like [pact](https://github.com/mpenet/pact)
to generate portions of your schemas, that's perfectly fine, and the resulting
file would certainly be usable with legba, but this is not a pre-requisite for
using *legba*.

## Legba goals

* Provide **rock solid**, **full coverage** of OpenAPI 3.1 spec

* Support all the bells & whistles such as `$refs`, conditionals, etc

* **[Great
  performance](https://www.creekservice.org/json-schema-validation-comparison/performance)**,
  being built on
  [networknt/json-schema-validator](https://github.com/networknt/json-schema-validator),
  which consistently ranks on the top of the java based json-schema validators.

* Be server adapter agnostic, it should be **usable with any RING compliant
  server adapter** (jetty, http-kit, aleph, hirundo, etc...)
  
* Provide building blocks to **build on top of your preferred routing libraries**
  or **plug onto any existing router**
  
* But **can also provide a default, easy to use single handler entry point**
  with routing for a given schema, built on the aforementioned primitives

* Provide **detailed**, informative and **customizable error messages**

## How does it work

You can either:

* Either use `s-exp.legba.handler/handlers`: taking an OpenAPI file and a map of
  `[method path]` -> `handler`, it will return a new map of `[method path]` ->
  `handler`, with all handlers wrapped with an OpenAPI validation
  middleware. From this map you can then plug the OpenAPI handlers in any
  routing solution and compose as you prefer.

* Or use `s-exp.legba/routing-handler`: taking an OpenAPI file and a map of
  `[method path]` -> `handler` that matches the routes of the OpenAPI schema,
  returns a single handler that will manage routing via reitit and perform
  validation and marshaling of the data according to the schema (via
  networknt/json-schema-validator). This handler can simply be plugged to a RING
  server adapter and you're good to go.
  
## Installation

[![Clojars Project](https://img.shields.io/clojars/v/com.s-exp/legba.svg)](https://clojars.org/com.s-exp/legba)

Or via git deps:

```clj 
com.s-exp/legba {:git/url "https://github.com/mpenet/legba.git" :git/sha "..."}
```

## Usage 

``` clj
(require '[s-exp.legba :as l])

(l/routing-handler {[:get "/item/{itemId}"]
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
                   "classpath://schema/oas/3.1/catalog.json"
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
