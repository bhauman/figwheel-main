---
title: Vim
layout: docs
category: docs
order: 10
---

# Vim

<div class="lead-in">
  vim-fireplace is the most popular Clojure plugin for Vim, providing
  good integration with Clojure and ClojureScript REPLs via nREPL and
  Piggieback.
</div>

## Quick Setup

If you don't already have vim-fireplace setup head on over to the
[Github project](https://github.com/tpope/vim-fireplace) and follow
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
    ├── fullstack
    │   ├── main.clj
    │   └── main.cljs
    └── user.clj
```

The following is a minimal figwheel dev config, `dev.cljs.edn`:

```clojure
^{:open-url false
  :watch-dirs ["src"]}
{:main fullstack.main}
```

> We're using `:open-url false` so that figwheel doesn't automatically
> open a browser tab when it starts figwheel, the ClojureScript REPL,
> etc.
>
> For the process we're describing here this would otherwise happen
> multiple times, so this just makes things a bit smoother.

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
                             [com.bhauman/figwheel-main "0.1.6-SNAPSHOT"]
                             [cider/piggieback "0.3.8"]]
              :resource-paths ["target"]
              :clean-targets ^{:protect false} ["target"]
              :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}})
```

When the Clojure REPL starts, it will start in the `user` namespace by
default. So let's create a starter file, `src/user.clj` with just:

```clojure
(ns user)
```

The `src/fullstack/main.clj` will be the entry-point into the
server-side (Clojure) application (if you choose to make use of it). We
can start out with a minimal file:

```clojure
(ns fullstack.main)

(defn -main []
  (println "Hello from CLJ main"))
```

The `src/fullstack/main.cljs` will be the entry-point into the
client-side (ClojureScript) application. We can just start with:

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
user=> (require 'figwheel.main)

user=> (figwheel.main/start {:mode :serve} "dev")
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
user=> (figwheel.main/cljs-repl "dev")
```

Test the connection to the browser with an alert:

```clojure
user=> (js/alert "Hello from the ClojureScript REPL")
```

If you see a pop-up dialog in your browser, it worked!

Now let's connect fireplace. Run this command in Vim while in a
ClojureScript buffer:

```vim
:Piggieback (figwheel.main/repl-env "dev")
```

This should return immediately if successful.

Let's ensure we're able to communicate with the browser from Vim. Add
the following in a ClojureScript file and eval it (`cpp`):

```clojure
(js/alert "Hello from Vim")
```

If that worked, we're all set!
