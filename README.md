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
- `notify` returns the ID (`String`) of the newly-created Honeybadger
  fault in case you want to log it, pass it to another system, or
  display it to your users as an incident identifier.

## Motivation

I got a lot of mileage out of [ring-honeybadger][] but ultimately
found that it wasn't quite flexible enough to meet my needs, and its
author isn't currently interested in extending it or reviewing PRs.  I
also dislike that it's bound to Ring when the underlying service is
not.

Features differentiating this library from (some of) the alternatives:

- A la carte Honeybadger reporting.
- Ability to report (informational) non-exceptions.
- No implicit assumptions about what to report (for instance because
  of environment or exception type).
- No deprecation warnings.
- Honeybadger fault IDs are returned.

## License

Copyright © 2015 Cameron Desautels

Distributed under the MIT License.

[honeybadger]: https://honeybadger.io
[throwable]: https://docs.oracle.com/javase/7/docs/api/java/lang/Throwable.html
[exception]: https://docs.oracle.com/javase/7/docs/api/java/lang/Exception.html
[ring-honeybadger]: https://github.com/weavejester/ring-honeybadger
