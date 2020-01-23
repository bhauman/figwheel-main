---
title: Hot Reloading
layout: docs
category: docs
order: 9
---

# Hot Reloading

<div class="lead-in">Figwheel's dominating feature is a very fast hot
reloaded workflow.</div>

When you start a build process with the `--build` option and your
`:optimizations` level is the default `:none`, Figwheel starts a
hot-reloaded workflow by default.

Working in a hot-reloaded environment can initially take some getting
used to, but once you get the hang of it you will experience a noted
increase in productivity and enjoyment in your coding.

Among the challenges of working in a hot reloaded environment the
biggest one is ensuring that you are aware of the
[load time side-effects][reloadable-code] of your code.

Here we are going to cover the tools available to you to tweak the
hot reloading behavior of Figwheel.

## Configuring which directories to watch

Of all the [Figwheel config options][config-options] the one you will
use the most often is the [`:watch-dirs` option][watch-dirs]. This
option determines which directories Figwheel will watch for file
changes. While Figwheel will guess which directory to watch based on
your `:main` namespace, it is better to configure this explicitly.
Please take a moment to read the
[`:watch-dirs` documentation][watch-dirs]. It is very important to
understand that watched directories need to also be in the list of
paths on the [classpath][classpath].

Note that you will normally configure this on a per build basis, for
example in our `dev.cljs.edn`:

```clojure
^{:watch-dirs ["src"]}
{:main hello-world.core}
```

## Reload a file on every save

Sometimes there is a file that you want to reload every time a file is
saved. You can accomplish this by adding `:figwheel-always` to the
namespace of the file.

For example:

```clojure
(ns ^:figwheel-always hello-world.core)

(println "hello")
```

There is rarely a need to use this because Figwheel will reload all
the files that are depending on the changed file that initiated the
reload. However, in some cases you may want to reload a file on every
save when it doesn't depend on your current file.

Using `:figwheel-always` may make more sense when you
[disable the reloading of dependents](#disable-reloading-dependent-namespaces).

## Never reload a given file

There are files that once they've been loaded you don't want them to
reload again. This can be accomplished by adding `:figwheel-no-load`
to the namespace of the file that you don't want to be reloaded.

For example:

```clojure
(ns ^:figwheel-no-load hello-world.core)

(println "hello")
```

This will prevent `hello-world.core` from ever being **re-loaded**. It
will load the first time but will not be reloaded after that.

## Force a file to be loaded

Figwheel only reloads files that have been required by your
application. Sometimes you are working on a file that hasn't been
required by your application and you want to load the file into your
application. You could go to the REPL and require it, but that would
take your focus out of the file you are editing. You can force a file
to be loaded by adding `:figwheel-load` metadata to the namespace.

For example:

```clojure
(ns ^:figwheel-load hello-world.core)

(println "hello")
```

This will force `hello-world.core` to be loaded for the first
time. Once it's been loaded, you can safely remove the
`:figwheel-load` metadata because the namespace is now noted as a
dependency in your client environment and will be reloaded on change
like all the other required files.

`:figwheel-load` is intended to be a development time tool so you can
load files that help with your dev process without having to
explicitly require them.

## Reload hooks

It is common to want to provide callbacks to do some housekeeping
before or after a hot reload has occurred.

You can conveniently configure reload callbacks at runtime by
first adding `:figwheel-hooks` metadata to the namespace that
contains functions that you want called on reload.

Once the namespace is marked you will then need to add
`:before-load` and `:after-load` metadata to the functions that you
want called on every reload.

Here is an example of using reload hooks:

```clojure
;; first notify figwheel that this ns has callbacks defined in it
(ns ^:figwheel-hooks example.core)

(defn ^:before-load my-before-reload-callback []
    (println "BEFORE reload!!!"))

(defn ^:after-load my-after-reload-callback []
    (println "AFTER reload!!!"))
```

## Re-rendering UI after saving a file

Most ClojureScript UI libraries like Reagent, Rum, Re-frame or Om only
render when some managed state changes. Most of the time, that state
is defined using `defonce`, so while Figwheel will compile and install
new versions of the various components, the DOM will stay the same so
the UI library doesn't have any reason to re-render. This is where the
aforementioned reload hooks are useful:

First, don't forget to add the `^:figwheel-hooks` annotation to the namespace:

```clojure
(ns ^:figwheel-hooks example.core)
```

Then add an `^:after-load` marked function that will render the UI. In
most cases you can reuse the "mount" function that rendered the UI for
the first time. For example, to get Reagent to re-render you'd write:

```clojure
;; this is what you call for the first mount
(defn mount []
  (r/render [my-main-component]
            (js/document.getElementById "app")))

;; and this is what figwheel calls after each save
(defn ^:after-load re-render []
  (mount))

;; this only gets called once
(defonce start-up (do (mount) true))
```

## Reloading Clojure code

If Clojure code is on a watched path Figwheel will reload it when it
changes. It does this because ClojureScript macros are defined in
Clojure not ClojureScript and if we want to pick up changes to our
macros as we save them, we need to reload the changed Clojure code and
then recompile/reload the ClojureScript files that depend on it.

Reloading changed Clojure code can also be helpful while you work on
Clojure files that don't have macros because Figwheel will report any
load time syntax errors in the heads-up display, allowing you to
catch errors sooner.

You may want to disable the reloading of Clojure files if your Clojure
code has load time side-effects that you don't want to manage. Another
good reason to do this is when reloading Clojure code is causing a
mostly ClojureScript application to recompile/reload and thus
slowing down your workflow.

Use the [`:reload-clj-files` option][reload-clj-files] to disable the
reloading of watched Clojure code.

Setting `:reload-clj-files` to `false` will stop `.clj` files and
`.cljc` files from being reloaded.

If you only want to reload `.cljc` files set `:reload-clj-files` to the vector `[:cljc]`

If you only want to reload `.clj` files set `:reload-clj-files` to the vector `[:clj]`

> Figwheel does have limited support for recompiling ClojureScript
> files which depend on Clojure files. It will only recompile and
> reload ClojureScript files which are direct dependents of a changed
> Clojure file.


## Disabling hot reloading

There are plenty of situations where you will want to disable
ClojureScript hot reloading and go back to reloading the browser after
you make changes. This can be done with the
[`:hot-reload-cljs` option][hot-reload-cljs]. By setting
`:hot-reload-cljs` to `false` you will stop files from being reloaded
when you change them.

Setting `:hot-reload-cljs` to `false` does not stop files from being
recompiled on change and will still allow you to get feedback from the
compile process in the heads-up display.

## Disable reloading dependent namespaces

Figwheel by default finds all the files that depend on a changed file
and reloads them in correct dependency order after reloading the
changed file itself.

If you are working in a situation where you don't want these dependent
files reloaded set the
[`:reload-dependents` option][reload-dependents] to `false`

## Disable recompiling of dependent namespaces

So far we have been talking about Figwheel options but there is a
ClojureScript compiler option that you should be aware of in how it
affects recompiles.

By default the ClojureScript compiler recompiles all the dependents along with
a changed file. Depending on the size of your dependency tree and the
compile time of all the dependent files this can significantly slow
the compile time after each file save.

You can disable the recompiling of dependent namespaces by setting
`:recompile-dependents` in your compile options to `false`.

As we have been talking about Figwheel options so far in on this page,
I want to clarify that this is a ClojureScript compile option and you
would place it in your `[build-name].cljs.edn` file like so:

```clojure
{:main hello-world.core
 :recompile-dependents false}
```

The `:recompile-dependents` CLJS option is completely independent of
the `:reload-dependents` Figwheel option. It can still make sense to
reload your dependent files even if they are not recompiled. Load time
side effects are the reason. Reloading dependents is very fast
compared to recompiling dependents. I recommend that you continue to
reload dependent files even if you stop recompiling dependents.

## Slowing reloads down

Depending on your environment Figwheel may reload files too
quickly. If it does reload too soon it may be reloading the old file
before it has changed. This is not an ideal experience.

For example you may be working with a [Jekyll][jekyll] site where
Jekyll copies all the assets of the site to a `_site` directory and
serves the site from there. Jekyll currently doesn't copy the files
fast enough to beat Figwheel's reload time.

You can slow down this reload time with the
[`:wait-time-ms` option][wait-time-ms]. It defaults to 50 milliseconds.

You can make each reload wait a second after each compile by setting
`:wait-time-ms` to `1000`.

## Reloads are broadcast

There can be any number of clients (browser tabs) with valid
connections to the Figwheel websocket server. For sanity the Figwheel
REPL only communicates with one of these clients. However, reloads are
**broadcast** to all clients that are connected to the current build-id.

The reason it is done this way is to facilitate development across
multiple clients types. I.E. You want to work on your web app and view
the live reloaded changes on your phone, tablet and laptop at the same
time.

If you want to disable this you can set the
[`:broadcast-reloads` option][broadcast-reloads] to `false`.

> Figwheel is careful about which clients are able to receive reload
> and compile time messages. When you start Figwheel it assigns itself
> a unique identifier. When a client connects to the Figwheel
> websocket it can supply this process-id along with the
> build-id. Figwheel only sends messages to clients with the correct
> identifiers. This prevents stale clients and clients from with other
> build ids getting the wrong reload messages and REPL evals.


[broadcast-reloads]: ../config-options#broadcast-reloads
[wait-time-ms]: ../config-options#wait-time-ms
[reload-clj-files]: ../config-options#reload-clj-files
[reload-dependents]: ../config-options#reload-dependents
[hot-reload-cljs]: ../config-options#hot-reload-cljs
[watch-dirs]: ../config-options#watch-dirs
[reloadable-code]: reloadable_code
[jekyll]: https://jekyllrb.com
[config-options]: ../config-options
[classpath]: classpaths
