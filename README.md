# cljson

Cljson is a Clojure/ClojureScript data to JSON converter. It supports all the
data types that ClojureScript supports. No support for tagged literals yet.

## Install

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

Cljson transforms Clojure data to and from the restricted set of types which can
be encoded as JSON. The resulting JSON can be passed back and forth between a
serverside Clojure application and the ClojureScript running in a browser without
needing to use `#'read-string` or `#'pr-str`, which are slow in the browser.

There are two functions exported by this library: `clj->cljson` and `cljson->clj`.
They convert Clojure data to and from JSON strings.

```clojure
tailrecursion.cljson=> (clj->cljson [1 2 3])
"[1,[1,2,3]]"

tailrecursion.cljson=> (clj->cljson {[1 2 3] :foo 'bar #{"bar"}})
"[2,[[[1,[1,2,3]],\"\\ufdd0'foo\"],[\"\\ufdd1'bar\",[3,[\"bar\"]]]]]"

tailrecursion.cljson=> (cljson->clj "[2,[[[1,[1,2,3]],\"\\ufdd0'foo\"],[\"\\ufdd1'bar\",[3,[\"bar\"]]]]]")
{[1 2 3] :foo, bar #{"bar"}}
```

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
