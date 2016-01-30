# Change Log

All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## Unreleased

- Fixed: errors compiling on Clojure 1.8 (due to Aleph version).

## 0.2.1 - 2016-01-29

- Fixed: non-string / keyword keys in metadata can cause exceptions.

## 0.2.0 - 2015-09-29

- Use protocol to define what `notify` can handle.
- Add filters for managing which exceptions are reported.

## 0.1.0 - 2015-09-24

- Initial public release.
