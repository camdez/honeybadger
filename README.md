# honeybadger

[![Clojars Project][clojars-badge]][clojars-honeybadger]

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
  (`String`) of the newly-created Honeybadger fault--or `nil` if a
  filter (see below) caused the data not to be sent to
  Honeybadger. Because a deferred is used, the call returns
  immediately, not blocking your (e.g.) web server thread. This comes
  with
  [the typical Clojure caveats about exceptions thrown on background threads](background-exceptions),
  so I strongly recommend dereferencing these calls on the main thread
  unless / until you have an async error handling plan in place.
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

### Metadata

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
                      :session {"session-id" "d34dc0d3"}}})
```

All metadata is optional, so pick and choose what is useful for your
project. Keys and tags can be strings or keywords.  `:context` and
`:request` supported nested values.  If you're working with Ring, use
the corresponding [ring-honeybadger](camdez-rh) library and the
`:request` metadata will be populated for you.

### Filters

For more advanced behavior, the library allows us to provide a
sequence of functions which will be invoked with all key details (viz.
exception + configuration) prior to reporting to Honeybadger. These
functions can be used to transform the data in arbitrary ways, or they
can return `nil`, halting the function chain and indicating that
nothing should be reported to Honeybadger.

For maximum flexibility we can provide a custom function, but we can
handle many common cases with the preexisting filters / filter
combinators in `honeybadger.filter`:

```clj
(require '[honeybadger.core :as hb]
         '[honeybadger.filter :as hbf])

(def hb-config
  {:api-key "d34db33f"
   :env     "development"
   :filters [(hbf/only   (hbf/env? :production))
             (hbf/except (hbf/instance? ArithmeticException))
             (hbf/obscure-params [[:config :password]])]})

(hb/notify hb-config "dag, yo")
```

In this example, the first two filters are used to control which
errors get reported to Honeybadger, and the third is used to transform
the data we *do* send.

More precisely, the first two filter lines say *only report errors in
the production environment, and don't report errors of type
`ArithmeticException`*. The third filter uses the `obscure-params`
convenience function to replace parameters at the given keypaths with
a fixed string so that sensitive parameters are not sent to be stored
in Honeybadger. (Of course there isn't a param at
`[:config :password]` in this case as we haven't provided any request
metadata, so the filter won't change anything here).

To make filtering both possible and convenient, all details about the
error / config / metadata / etc. are bundled up in a consistent format
which filters are expected to consume and produce (with the sole
exception of filters which return `nil` to suppress reporting of an
error). You can see the details of that format at
`honeybadger.schemas/Event`, and if you use [Prismatic/schema][schema]
in your own project, then you can use the provided schemas to enforce
correctness. One detail worth calling out is that all map keys are
normalized to keywords so that filters don't have to handle
variations.

Here's an example of a fully-custom filter, applying a `logged-in` tag
to all exception reports where we have a `session-id`:

```clj
(defn tag-logged-in [e]
  (if (get-in e [:metadata :request :session :session-id])
    (update-in e [:metadata :tags] conj :logged-in)
    e))
```

Using that is as simple as adding `tag-logged-in` to the list of
filters.

Filters that suppress certain errors can typically be written with a
simple predicate function over `Event`s which is passed to `only?` or
`except?`:

```clj
(defn logged-in? [e]
  (get-in e [:metadata :request :session :session-id]))

(def hb-config
  {;; ...
   :filters [(hbf/only logged-in?)]})
```

Last but not least, note that the (deferred) value returned by
`notify` allows us to ascertain whether or not a given error was
reported because it will be `nil` iff the error reporting was filtered
out. We can use this to take conditional actions:

```clj
(if-let [hb-id @(hb/notify {:api-key "d34db33f"
                            :filters [(hbf/only (constantly nil))]}
                           "chunky bacon")]
  (str "Reported error with ID " hb-id)
  "Error reporting suppressed by filter")
```

### Ring

If you'd like to use this project with Ring, check out
[camdez/ring-honeybadger][camdez-rh].

## Motivation

I got a lot of mileage out of
[weavejester's ring-honeybadger][weavejester-rh] but ultimately found
that it wasn't quite flexible enough to meet my needs, and its author
doesn't currently have time to extend it or review PRs.  I also
dislike that it's bound to Ring when the underlying service is not.

Features differentiating this library from (some of) the alternatives:

- À la carte Honeybadger reporting.
- Ability to report (informational) non-exceptions.
- No implicit suppression by environment or exception type.
- No deprecation warnings.
- Honeybadger fault IDs are returned.
- Set of reportable objects open to extension (by protocol).
- Supports all known (to me) Honeybadger attributes.
- Asynchronous reporting (doesn't block the calling thread while the
  exception is being reported).
- Custom filtering of reported errors.
- Custom transformation of reported errors.
- Helpers for common filter / transformation operations
  (e.g. filtering by environment / exception class, redacting of
  sensitive parameters).

## License

Copyright © 2015 Cameron Desautels

Distributed under the MIT License.

[clojars-badge]: http://clojars.org/camdez/honeybadger/latest-version.svg
[clojars-honeybadger]: http://clojars.org/camdez/honeybadger
[honeybadger]: https://honeybadger.io
[throwable]: https://docs.oracle.com/javase/7/docs/api/java/lang/Throwable.html
[exception]: https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html
[weavejester-rh]: https://github.com/weavejester/ring-honeybadger
[camdez-rh]: https://github.com/camdez/ring-honeybadger
[manifold]: https://github.com/ztellman/manifold
[deref]: https://clojuredocs.org/clojure.core/deref
[schema]: https://github.com/Prismatic/schema
[background-exceptions]: http://stuartsierra.com/2015/05/27/clojure-uncaught-exceptions
