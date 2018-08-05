---
title: Code Splitting
layout: docs
category: docs
order: 20
---

# Code Splitting

<div class="lead-in">ClojureScript let's you break down a larger
application into modules that can be dynamically loaded into your host
environment at runtime.</div>

This feature is covered in the
[Code Splitting Guide][code-splitting-guide]. I'm going to discuss how
to use Figwheel with a build where you are utilizing code splitting.

## Skip `:main` and `:output-to`

The first thing to note is that both the `:main` and the top-level
`:output-to` compile options have no meaning and are ignored when you
configure a code splitting build with `:modules`.

Previously, we have said that to use the `--build` Figwheel flag
requires declaring a `:main` at the very least. This doesn't apply
when you configure a code splitting build.

## Figwheel Code Splitting Guide

> This requires figwheel-main version `0.1.5` or greater. 

This is going to be a brief version of the official
[Code Splitting Guide][code-splitting-guide] with adjustments for
working with Figwheel. You should read and understand that document
first.

### Make a Simple Project

Create a project folder:

```shell
mkdir -p hello-modules
cd hello-modules
mkdir src
```

Create a `deps.edn` file to work with Figwheel.

```shell
touch deps.edn
```

Edit this script to look like the following:

```clojure
{:deps {com.bhauman/figwheel-main {:mvn/version "0.1.5"}}
 :paths ["src" "resources" "target"]}
```

### The Sources

Create the `foo.core` namespace:

```shell
mkdir -p src/foo
touch src/foo/core.cljs
```

Edit this file to look like the following:

```clojure
(ns ^:figwheel-load foo.core
  (:require [goog.dom :as gdom]
            [goog.events :as events]
            [cljs.loader :as loader])
  (:import [goog.events EventType]))

(println "I'm foo!")

(defn listen-to-button []
  (let [app (gdom/getElement "app")
        button (gdom/createDom
                "button"
                (gdom/createTextNode "Load Bar!"))]
    (gdom/removeChildren app)
    (gdom/append app button)
    (events/listen button EventType.CLICK
                   (fn [e]
                     (loader/load :bar
                                  (fn []
                                    ((resolve 'bar.core/woz))))))))

(defonce init (listen-to-button))

(loader/set-loaded! :foo)
```

> I altered the original example to make it
> [reloadable][reloadable-code] and so that it works with the DOM on
> the Figwheel default dev page. I also added [`^:figwheel-load`
> metatdata][figwheel-load] to the namespace so that I can easily load the file into
> my environment by saving it.

Create the `bar.core` namespace:


```shell
mkdir -p src/bar
touch src/bar/core.cljs
```

```clojure
(ns bar.core
  (:require [cljs.loader :as loader]))

(println "I'm bar!")

(defn woz []
  (println "WOZ!"))

(loader/set-loaded! :bar)
```

> Note I removed the `(enable-console-print!)` from the original
> examples. Figwheel already enables printing to the console by
> default. See the [`:client-print-to` option][client-print-to].


### Configure your build file

Let's make our Figwheel build file to support a `:modules` based build.

In a `dev.cljs.edn` file place the following:

```clojure
^{:watch-dirs ["src"]}
{:modules {:foo {:entries #{foo.core}}
           :bar {:entries #{bar.core}}}}
```

This is all the compile configuration that's needed for Figwheel to
start building and hot reloading a build that is using code
splitting. 

The `:watch-dirs` config option is required when working with
`:modules`.

Note that Figwheel will supply the `:output-to` config parameter for
each of your modules if one doesn't exist already.

If you do want to supply your own `:output-to` parameters to each
module you will want to heed the
[advice that is true for the top level `:output-to`][output-to]
option. It needs to be on a path that the client to load it and thus
should normally be on the classpath under a `public` directory.

If you want to choose your `:output-to` you will probably also want to
configure your top level `:output-dir` and `:asset-path` options as well.

You can learn more about the configuration params that Figwheel
provides by using the `-pc` option with the above minimal configuration.

For example if you run:

```shell
clojure -m figwheel.main -pc -b dev -r
```

It will show compile configuration to be this:

```clojure
---------------------- Compiler options ----------------------
{:modules
 {:foo
  {:entries #{foo.core},
   :output-to "target/public/cljs-out/dev-foo.js"},
  :bar
  {:entries #{bar.core},
   :output-to "target/public/cljs-out/dev-bar.js"}},
 :preloads
 [figwheel.core
  figwheel.main
  figwheel.repl.preload
  devtools.preload],
 :output-dir "target/public/cljs-out/dev",
 :asset-path "/cljs-out/dev",
 :aot-cache false,
 :closure-defines
 #:figwheel.repl{connect-url
                 "ws://localhost:9500/figwheel-connect?fwprocess=279383&fwbuild=dev"},
 :repl-requires
 ([figwheel.repl :refer-macros [conns focus]]
  [figwheel.main
   :refer-macros
   [stop-builds start-builds build-once reset clean status]]
  [cljs.pprint :refer [pprint] :refer-macros [pp]]
  [cljs.repl :refer-macros [source doc find-doc apropos dir pst]])}
```

The main difference here and an ordinary build is that there is neither a
`:main` option nor a top-level `:output-to` option.

### Build and start a REPL

Note that we haven't created a [host HTML page][host-page-doc] for our
build yet. The Figwheel default dev page work and host a REPL for your
`:modules` build.

To begin just start the build as usual:

```shell
clojure -m figwheel.main -b dev -r
```

You will see the default dev page open up and a REPL start.

It is important to note that neither of the modules has been loaded
yet. You can `require` (or use `:figwheel-load` metadata) to load them
in to the client environment and start working on them in the normal
hot reloaded workflow.

Since we already included [`^:figwheel-load` metadata][figwheel-load]
on the `foo.core` namespace you can make a small whitespace change to
the `src/foo/core.cljs` file and you will see a `Load Bar!` button
replace all the current content of the page. Open the development
console and press the button so you can see the dynamic loading at
work.

### Host page

We're going to make a Host page now. Almost every thing that we
covered on the [Host Page doc][host-page-doc] still applies here The
only difference is that we need to load a `cljs_base.js` file along
with any modules we want to initialize the page with.

Now make an `resources/public/index.html` file:

```html
<html>
    <body>
		 <div id="app"></div>
		 <!-- include the cljs_base.js target file -->
         <script src="target/cljs-out/dev/cljs_base.js" type="text/javascript"></script>
		 <!-- You will normally want to include at least one module otherwise nothing will happen -->		 
         <script src="target/cljs-out/dev-foo.js" type="text/javascript"></script>
    </body>
</html>
```

Note the name of the default `:output-to` file for the `:foo` module is
the `[build-id]-[module-name].js`.

> If you customize the `:output-to`'s for the modules you will need to
> alter the the paths on your host page accordingly.

### Build the Project

Now if we return to the shell and build the project again:

```shell
clojure -m figwheel.main -b dev -r
```

We will see the page with our button on it and get a working REPL. We
are now in a normal Figwheel hot reloaded workflow.

### Release Compile

The release compile is the same as other [advanced builds][advanced]:

```shell
clojure -m figwheel.main -O advanced -bo dev
```

The advanced compile still outputs a `cljs_base.js` file and a
compressed output file for each module so your HTML host page will
still work just fine.

[advanced]: advanced_compile
[client-print-to]: ../config-options#client-print-to
[host-page-doc]: your_own_page
[code-splitting-guide]: https://clojurescript.org/guides/code-splitting
[reloadable-code]: reloadable_code
[output-to]: compile_config#the-output-to-option
[figwheel-load]: hot_reloading#force-a-file-to-be-loaded
