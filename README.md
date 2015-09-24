# honeybadger

A Clojure library for reporting errors to [Honeybadger][].

## Usage

The library only has one public endpoint: `notify`.  You can pass
`notify` a `String`, or anything which inherits from
[`Throwable`][throwable] (e.g. [`Exception`][exception]):

```clj
(require '[honeybadger.core :as hb])

(def hb-config
  {:api-key "d34db33f"
   :env     "development"})

(hb/notify hb-config "Something happened")
(hb/notify hb-config (Exception. "Things ain't good"))
```

- `api-key` is the only required entry in the configuration map.
- `notify` returns a [Manifold][] *deferred* wrapping the ID
  (`String`) of the newly-created Honeybadger fault. This means that
  the call returns immediately, not blocking your (e.g.) web server
  thread.
- Honeybadger fault IDs can be handy--log them, pass them to other
  systems, or display them to your users as incident identifiers they
  can send to your support team. Manifold offers ways of receiving
  this data asynchronously, but for a simple (synchronous) approach,
  simply [`deref`][deref] the return value:

```clj
(try
  ("kaboom") ; Strings aren't functions
  (catch Exception e
    (let [hb-id @(hb/notify hb-config e)]
      (println (str "Exception! Learn more here:\n"
                    "https://www.honeybadger.io/notice/" hb-id)))))
;; (Output)
;; Exception! Learn more here:
;; https://www.honeybadger.io/notice/12345678-669c-4178-b456-be3d0feb1551
```

The optional third parameter to `notify` can be used to pass all
manner of additional Honeybadger metadata. The following example shows
all possible metadata values:

```clj
(hb/notify hb-config
           (Exception. "Vapor Lock")
           {:tags [:serious :business]
            :component "robot-brain" ; ~= a Rails controller
            :action "think"          ; ~= a Rails action
            :context {:name "Winston"
                      :power 42
                      :grease 12}
            :request {:method :get
                      :url "http://camdez.com"
                      :params {"robot" "true"}
                      :session {"session_id" "d34dc0d3"}}})
```

All metadata is optional, so pick and choose what is useful for your
project. Keys and tags can be strings or keywords.  `:context` and
`:request` supported nested values.

## Motivation

I got a lot of mileage out of [ring-honeybadger][] but ultimately
found that it wasn't quite flexible enough to meet my needs, and its
author isn't currently interested in extending it or reviewing PRs.  I
also dislike that it's bound to Ring when the underlying service is
not.

Features differentiating this library from (some of) the alternatives:

- A la carte Honeybadger reporting.
- Ability to report (informational) non-exceptions.
- No implicit suppression by environment or exception type.
- No deprecation warnings.
- Honeybadger fault IDs are returned.
- Asynchronous reporting (doesn't block the calling thread while the
  exception is being reported).

## License

Copyright © 2015 Cameron Desautels

Distributed under the MIT License.

[honeybadger]: https://honeybadger.io
[throwable]: https://docs.oracle.com/javase/7/docs/api/java/lang/Throwable.html
[exception]: https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html
[ring-honeybadger]: https://github.com/weavejester/ring-honeybadger
[manifold]: https://github.com/ztellman/manifold
[deref]: https://clojuredocs.org/clojure.core/deref
