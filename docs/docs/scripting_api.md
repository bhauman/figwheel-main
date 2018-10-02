---
title: Scripting API
layout: docs
category: docs
order: 14
published: true
---

# Scripting API

<div class="lead-in">You can readily start and stop a Figwheel build
process from the REPL or a script.</div>

> This API is only available for `figwheel.main 0.1.6` and higher.

## Starting Figwheel from the REPL (or Script)

The scripting API isn't quite as general as the command line API. It focuses on
creating and managing **running** build processes.

To use the API you will need to ensure that you have
the [dependencies](installation) and the [classpaths](classpath)
sorted out to use `figwheel.main`. Then you will need to require the
`figwheel.main.api` namespace and call the `figwheel.main.api/start`
function.

Let's assume we have a `dev.cljs.edn` build file and we want to start
Figwheel from the Clojure REPL. You can start the `dev` build with a
REPL as follows:

```clojure
$ clj
Clojure 1.9.0
user=> (require 'figwheel.main.api)
nil
user=> (figwheel.main.api/start "dev")
;; ... Figwheel startup output ommitted ...
ClojureScript 1.10.238
cljs.user=>
```

As you can see this starts a Figwheel build process along with a
ClojureScript REPL.

If you want to start a Figwheel build without a REPL you will need to
ensure that the [`:mode` option][mode] is `:serve`. You can do this in
the metadata in the build file or you can supply a **replacement** for the
`figwheel-main.edn` config options like so:

```clojure
$ clj
user=> (figwheel.main.api/start {:mode :serve} "dev")
[Figwheel] Compiling build dev to "target/public/cljs-out/dev-main.js"
[Figwheel] Successfully compiled build dev to "target/public/cljs-out/dev-main.js" in 0.782 seconds.
[Figwheel] Watching and compiling paths: ("src" "devel") for build - dev
[Figwheel] Starting Server at http://localhost:9500
2018-08-06 17:53:26.155:INFO::main: Logging initialized @15707ms
Opening URL http://localhost:9500
nil
user=>
```

As you can see this starts the Figwheel build process, launches a
server and does not launch a ClojureScript REPL, instead it returns
you to the Clojure REPL so you can continue interacting with your
Clojure process.

Now that you have the `dev` Figwheel build running in the background,
you can now use the rest of the Scripting API. For example, you can
launch a ClojureScript REPL attached to the running `dev` build
process like so:

```clojure
;; in the Clojure REPL after you have started the "dev" build
user=> (figwheel.main.api/cljs-repl "dev")
```

This will start a REPL into the running `dev` build. You can quit the
REPL via `:cljs/quit` and then restart it by calling
`figwheel.main.api/cljs-repl` again.

## REPL switching example

For this example to work, one will need to set up the normal figwheel
dependencies along with `com.bhauman/rebel-readline-cljs`.

We'll start off by starting a Rebel Readline Clojure REPL.

```clojure
$ clojure -m rebel-readline.main
user=>
```

Assuming that we already have a build set up in `dev.cljs.edn` and a
[background build][background-builds] defined in `admin.cljs.edn`.

We can start both of these builds running with:

```clojure
user=> (require '[figwheel.main.api :as fig])
nil
user=> (fig/start {:mode :serve} "dev" "admin")
```

Now we'll start a REPL for the `dev` build:

```clojure
user=> (fig/cljs-repl "dev")
;; .. figwheel REPL startup output omitted ...
cljs.user=> (js/console.log "hey")
nil
```

There are a couple of things to notice at this point.  If you check
the console of the browser window that the `dev` application is
running in you will notice that the word `"hey"` is printed out. You
should also notice that your ClojureScript REPL is utilizing Rebel
Readline.

You can now quit the `dev` REPL and launch a REPL into `admin`.

```clojure
cljs.user=> :cljs/quit
nil
;; returns us to the Clojure REPL prompt
user=> (fig/cljs-repl "admin")
;; .. figwheel REPL startup output omitted ...
cljs.user=>
```

We've successfully switched between ClojureScript REPLs for our builds
all the while staying in a Rebel Readline environment.

# API Docs

{% include main-api-docs.md %}

[mode]: ../config-options#mode
[background-builds]: background_builds
