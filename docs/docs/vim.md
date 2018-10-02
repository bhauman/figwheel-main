---
title: Vim
layout: docs
category: docs
order: 17
---

# Vim

<div class="lead-in"><a href="https://github.com/tpope/vim-fireplace">vim-fireplace</a> 
is the most popular Clojure plugin for Vim, providing good integration
with Clojure and ClojureScript REPLs via nREPL and Piggieback.
</div>

## Quick Setup

If you don't already have vim-fireplace setup head on over to the
[Github project][vim-fireplace] and follow
the installation guide.

To demonstrate how all the pieces fit together we'll create a simple,
minimal project with [Leiningen](https://leiningen.org/).

> Later on we'll describe how to use the
> [Clojure CLI](https://clojure.org/guides/deps_and_cli) and
> [rebel-readline](https://github.com/bhauman/rebel-readline).

We'll name our project *fullstack*. It will contain the following files
and folders:

```shell
├── dev.cljs.edn
├── project.clj
├── resources
└── src
    ├── cljs
    │   └── user.cljs
    └── fullstack
        ├── main.clj
        └── main.cljs
```

The following is a minimal figwheel dev config, `dev.cljs.edn`:

```clojure
^{:watch-dirs ["src"]}
{:main fullstack.main}
```

Create a `resources` folder. We'll leave it empty for now.

Place the following in `project.clj`:

```clojure
(defproject fullstack "0.1"
  :description "A minimal sample full-stack Clojure(Script) website"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]]
  :plugins [[cider/cider-nrepl "0.18.0"]]
  :profiles {:dev
             {:dependencies [[org.clojure/clojurescript "1.10.339"]
                             [com.bhauman/figwheel-main "0.1.9"]
                             [cider/piggieback "0.3.8"]]
              :resource-paths ["target"]
              :clean-targets ^{:protect false} ["target"]
              :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}})
```

`src/fullstack/main.clj` will be the entry-point into the server-side
(Clojure) application . We can start out with a minimal file:

```clojure
(ns fullstack.main)

(defn -main []
  (println "Hello from CLJ main"))
```

`src/fullstack/main.cljs` will be the entry-point into the client-side
(ClojureScript) application. We can just start with:

```clojure
(ns fullstack.main)

(println "Hello from CLJS main")
```

## Starting the REPLs

Now we can start the Clojure REPL with:

```shell
$ lein repl
```

An nREPL server will automatically start. Verify that fireplace is able
to communicate with it by `eval`ing something. E.g. you can place your
cursor over a symbol in Vim and press `K` to see its doc string. If
you get an immediate result, you have successfully connected to the
nREPL server.

Now we can start figwheel:

```clojure
user=> (require 'figwheel.main.api)

user=> (figwheel.main.api/start {:mode :serve} "dev")
```

Some log messages will appear including something like:

```shell
[Figwheel] Starting Server at http://localhost:9500
```

Open your browser to this URL. This is the default Figwheel dev host
page.  A green "Connected" image should appear at the top-left
indicating a successful connection between the browser and figwheel.

Back in the Clojure REPL, we're now ready to start a ClojureScript REPL
within.

> Be careful not to `eval` anything in a ClojureScript file from Vim
> yet. If you do this before connecting Piggieback, fireplace may start
> up its own ClojureScript REPL, disjoint from the browser!

Start the ClojureScript REPL:

```clojure
user=> (figwheel.main.api/cljs-repl "dev")
```

Test the connection to the browser with an alert:

```clojure
user=> (js/alert "Hello from the ClojureScript REPL")
```

If you see a pop-up dialog in your browser, it worked!

Now let's connect fireplace. Run this command in Vim while in a
ClojureScript buffer:

```vim
:Piggieback (figwheel.main.api/repl-env "dev")
```

This should return immediately if successful.

Let's ensure we're able to communicate with the browser from Vim. Add
the following in a ClojureScript file and eval it (`cpp`):

```clojure
(js/alert "Hello from Vim")
```

If that worked, we're all set!

## Rebel-Readline, Clojure CLI, Deps

> If you're happy with the workflow described above using Leiningen, you
> don't have to read any further.
>
> What's described here is optional and for those who prefer to use the
> Clojure CLI, deps.edn and rebel-readline over Leiningen.

For the most part, we'll be editing our `.clj`, `.cljs` and `.cljc`
files in Vim. vim-fireplace provides most of the functionality we need
for this purpose, however it's
[often necessary and even desirable](https://github.com/bhauman/rebel-readline/blob/master/rebel-readline/doc/intro.md)
to interact directly with a "true" REPL - not the spartan "Quasi-REPL"
provided by fireplace.

Leiningen provides a pretty good REPL experience with tab-complete,
etc. but rebel-readline takes things to the next level including
syntax highlighting, inline docs, and particularly useful to Vim
enthusiasts: Vim movements and custom key bindings!

Rebel-readline is able to
[work with Leiningen](https://github.com/bhauman/rebel-readline#leiningen),
but we'll describe how to use it with the simpler
[Clojure CLI](https://clojure.org/guides/deps_and_cli).

> Why not just use Leiningen? See here for
> [some rationale](https://clojure.org/reference/deps_and_cli) on why
> you might prefer using the native Clojure CLI and deps.edn.

So here's a minimal `deps.edn` (analogous to the previous `project.clj`):

```clojure
{:aliases
  {:rebel {:main-opts ["-m" "rebel-readline.main"]}}
 :paths ["src" "resources" "target"]
 :deps
 {org.clojure/clojure {:mvn/version "1.9.0"}
  org.clojure/clojurescript {:mvn/version "1.10.339"}
  com.bhauman/figwheel-main {:mvn/version "0.1.9"}
  com.bhauman/rebel-readline {:mvn/version "0.1.4"}
  com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}
  org.clojure/tools.nrepl {:mvn/version "0.2.13"}
  cider/cider-nrepl {:mvn/version "0.17.0"}
  cider/piggieback {:mvn/version "0.3.8"}}}
```

Let's create a helper namespace to make it easier to start and stop the
nREPL server. Place the following in `src/fullstack/helpers.clj`:

```clojure
(ns fullstack.helpers
  "Helpers around starting/stopping an nREPL server."
  (:require [clojure.tools.nrepl.server :as nrepl-server]
            [clojure.java.io :as io]))

(def nrepl-port 7888)
(defonce nrepl-server (atom nil))

(defn nrepl-handler []
  (require 'cider.nrepl)
  (ns-resolve 'cider.nrepl 'cider-nrepl-handler))

(defn start-nrepl-server! []
  (reset!
    nrepl-server
    (nrepl-server/start-server :port nrepl-port
                               :handler (nrepl-handler)))
  (println "Cider nREPL server started on port" nrepl-port)
  (spit ".nrepl-port" nrepl-port))

(defn stop-nrepl-server! []
  (when (not (nil? @nrepl-server))
    (nrepl-server/stop-server @nrepl-server)
    (println "Cider nREPL server on port" nrepl-port "stopped")
    (reset! nrepl-server nil)
    (io/delete-file ".nrepl-port" true)))
```

Now we can start the Clojure REPL with rebel-readline:

```shell
$ clojure -A:rebel
```

And start the nREPL server:

```clojure
user=> (require '[fullstack.helpers :refer :all])

user=> (start-nrepl-server!)
```

Now vim-fireplace can talk to the REPL. Verify this by `eval`ing
something from Vim.

The remaining steps are just like what we did earlier with our
Leiningen setup. See [this section](#starting-the-repls) (skipping the
`lein repl` step of course).

That's it. You now have a rebel-readline REPL (via the Clojure CLI and
deps.edn) with no Leiningen in sight.

## Dedicated Clojure REPL

With the two setups describe above, our REPL is left in a state suited
to working in a ClojureScript environment. The Clojure environment is
still there of course, but we can't easily work with our Clojure
code-base directly at the REPL.

But as we already have an nREPL server running we can just launch
another REPL and connect to our running environment. Simply run:

```shell
$ lein repl :connect
```

> Unfortunately rebel-readline doesn't currently support working within
> an nREPL session. So we have to use Leiningen (or Boot if you prefer)
> for this second REPL instance.

Now we have a dedicated REPL for working with Clojure.

[vim-fireplace]: https://github.com/tpope/vim-fireplace
