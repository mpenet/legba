# Clojure library for building OpenAPI services

WIP: Here be dragons, it's not finished (yet)

legba is a library aimed at building OpenApi(3.1) services in Clojure. It
leverages
[networknt/json-schema-validator](https://github.com/networknt/json-schema-validator)
for the schema validation part, ensuring [full, up-to-date and extensive
coverage](https://www.creekservice.org/json-schema-validation-comparison/functional)
of the spec, not to mention [great
performance](https://www.creekservice.org/json-schema-validation-comparison/performance).

legba works by allowing you to construct a RING handler that will handle all
openapi requests and the associated routing that you can then plug to whatever
server adapter you are using.

This handler is created from an openapi json file that you pass to it and a map
keyed by `[method path]` to handler function that will be used to dispatch your
requests to their associated handler;. Requests **and** Responses will be
validated against the schema, covering all aspects of the spec such as body
contents, path parameters, query-string parameters, headers, status codes,
etc...


What it doesn't aim to do and will not do:

* Coercing payloads
* Supporting other means of validation, it's intentionally limited by what
  json-schema allows you to express

## License

Copyright © 2024-2025 Max Penet

Distributed under the Eclipse Public License version 1.0.
