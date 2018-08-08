---
title: Scripting API
layout: docs
category: docs
order: 20
published: false
---

# Scripting API

<div class="lead-in">You can readily start and stop a Figwheel build
process from the REPL or a script.</div>

There are times when you will want to launch Figwheel from a
script. We'll explore the scripting API here.

## Starting Figwheel

The API isn't quite as general as the command line API. It focuses on
creating and managing running build processes rather, and currently
doesn't facilitate.

First off you will need to ensure that you have
[the dependencies](installation) and the [classpaths](classpath)
sorted out to use `figwheel.main`. Then you will need to require the
`figwheel.main` namespace and call the `figwheel.main/start` function.

Let's assume we have a `dev.cljs.edn` build file and we want to start
`figwheel.main` from the Clojure REPL. You would start Figwheel as follows:

```clojure
$ clj
Clojure 1.9.0
user=> (require 'figwheel.main)
nil
user=> (figwheel.main/start "dev")
[Figwheel] Validating figwheel-main.edn
[Figwheel] figwheel-main.edn is valid!
[Figwheel] Compiling build dev to "target/public/cljs-out/dev-main.js"
[Figwheel] Successfully compiled build dev to "target/public/cljs-out/dev-main.js" in 0.808 seconds.
[Figwheel] Watching and compiling paths: ("src" "devel") for build - dev
[Figwheel] Starting Server at http://localhost:9550
[Figwheel] Starting REPL
Prompt will show when REPL connects to evaluation environment (i.e. a REPL hosting webpage)
Figwheel Main Controls:
          (figwheel.main/stop-builds id ...)  ;; stops Figwheel autobuilder for ids
          (figwheel.main/start-builds id ...) ;; starts autobuilder focused on ids
          (figwheel.main/reset)               ;; stops, cleans, reloads config, and starts autobuilder
          (figwheel.main/build-once id ...)   ;; builds source one time
          (figwheel.main/clean id ...)        ;; deletes compiled cljs target files
          (figwheel.main/status)              ;; displays current state of system
Figwheel REPL Controls:
          (figwheel.repl/conns)               ;; displays the current connections
          (figwheel.repl/focus session-name)  ;; choose which session name to focus on
In the cljs.user ns, controls can be called without ns ie. (conns) instead of (figwheel.repl/conns)
    Docs: (doc function-name-here)
    Exit: :cljs/quit
 Results: Stored in vars *1, *2, *3, *e holds last exception object
2018-08-06 17:47:51.991:INFO::main: Logging initialized @34931ms
Opening URL http://localhost:9550
ClojureScript 1.10.238
cljs.user=>
```

As you can see this starts the Figwheel build process along with a REPL.

If you want to start a Figwheel build without a REPL you will need to
ensure that the `:mode` option is `:serve`. You can do this in the
metadata in the build file or you can supply a replacement for the
`figwheel-main.edn` config options like so:

```clojure
$ clj
user=> (figwheel.main/start {:mode :serve} "dev")
[Figwheel] Compiling build dev to "target/public/cljs-out/dev-main.js"
[Figwheel] Successfully compiled build dev to "target/public/cljs-out/dev-main.js" in 0.782 seconds.
[Figwheel] Watching and compiling paths: ("src" "devel") for build - dev
[Figwheel] Starting Server at http://localhost:9500
2018-08-06 17:53:26.155:INFO::main: Logging initialized @15707ms
Opening URL http://localhost:9500
nil
user=>
```

As you can see this starts the Figwheel build process and launches a
server but does not start ClojureScript REPL, but rather returns you
to the Clojure REPL so you can continue interacting with your Clojure
process.


{% include main-api-docs.md %}
