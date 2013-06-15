# cljson

Use cljson to send data between Clojure and ClojureScript applications using JSON
as the data transfer format. The cljson library has implementations for Clojure and
ClojureScript and supports all the data types that ClojureScript supports, including
tagged literals.

## Why?

Parsing [edn](https://github.com/edn-format/edn) with `#'read-string` and
stringifying with `#'pr-str` is slow in the browser, but browsers have fast native
parsers and stringifiers for JSON.

## Install [![Build Status](https://travis-ci.org/tailrecursion/cljson.png?branch=master)](https://travis-ci.org/tailrecursion/cljson)

Artifacts are published on [Clojars](http://clojars.org/tailrecursion/cljson).

Leiningen:
```clojure
[tailrecursion/cljson "0.1.0-SNAPSHOT"]
```

Maven:
```xml
<dependency>
  <groupId>tailrecursion</groupId>
  <artifactId>cljson</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Usage

There are two functions exported by this library: `clj->cljson` and `cljson->clj`.
They convert Clojure data to and from JSON strings.

```clojure
user=> (use 'tailrecursion.cljson)
nil

user=> (clj->cljson [1 2 3])
"[1,2,3]"

user=> (cljson->clj "[1,2,3]")
[1 2 3]

user=> (clj->cljson '(1 2 3))
"{\"l\":[1,2,3]}"

user=> (cljson->clj "{\"l\":[1,2,3]}")
(1 2 3)

user=> (clj->cljson {[1 2 3] :foo 'bar #{"bar"}})
"{\"m\":[[1,2,3],{\"k\":\"foo\"},{\"y\":\"bar\"},{\"s\":[\"bar\"]}]}"

user=> (cljson->clj "{\"m\":[[1,2,3],{\"k\":\"foo\"},{\"y\":\"bar\"},{\"s\":[\"bar\"]}]}")
{[1 2 3] :foo, bar #{"bar"}}
```

## License

Copyright © 2013 Alan Dipert and Micha Niskin

Distributed under the Eclipse Public License, the same as Clojure.
