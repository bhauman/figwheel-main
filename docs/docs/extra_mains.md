---
title: Extra Mains
layout: docs
category: docs
order: 14
---

# Extra Mains

> This feature is only available in figwheel-main >= `0.1.8-SNAPSHOT`

<div class="lead-in"><a
href="../config-options#extra-main-files">Extra mains</a> allow you to
create extra entry points to your code base. This is helpful for
running things like tests, Devcards, and other top level applications
in the same codebase.</div>

Extra mains have the advantage of running in the build that declares
them. This makes them much lighter and simpler than running
[background builds][background-builds]. It also means they all share
the same broadcast updates when a file changes. In other words when a
source file changes all of the extra main apps get the change message,
but the namespace is only reloaded when it is in the app's dependency
tree.

The best part of the extra mains feature is that Figwheel provides
default web endpoints and you can easily use them without having to
create a host page.

Let's look at an example of using an extra main, so that we have a
better idea of what it is.

## Simple testing example

I'm going to assume that you already have a project set up. I'm going
to continue to use the `hello-world.core` project that I've been using
so far in this documentation.

We are going to add some basic tests to our project.

Go ahead and add the following `tests/hello-world/core_tests.cljs` file to
your project tree:

```clojure
(ns hello-world.core-tests
  (:require [cljs.test :refer-macros [deftest is run-tests]]))

(deftest should-not-pass
  (is (= 1 20)))

(run-tests)
```

Now we need to make sure we add the `tests` directory to your classpath.
For example in your `deps.edn` file:

```clojure
{...
 :paths ["resources" "target" "src" "tests"]
 ...}
```

We also need to add `tests` to your [`:watch-dirs` figwheel option][watch-dirs].
For example in your `dev.cljs.edn` file:

```clojure
^{:watch-dirs ["src" "tests"]}
{:main hello-world.core}
```

And now for the magic sauce you need to add an
[`:extra-main-files` entry][extra-main-files] in your Figwheel
options. For example:

```clojure
^{:watch-dirs ["src" "tests"]
  :extra-main-files {:tests {:main hello-world.core-tests}}}
{:main hello-world.core}
```

Now when you build the `dev` build:

```shell
$ clojure -m figwheel.main -b dev -r
```

Everything will start like usual. But if you check your target
directory you will have a new output file. In this example it would be
`target/public/cljs-out/dev-main-tests.js`. This output file will work
just like the `target/public/cljs-out/dev-main.js` output file except
that it will run the code in the `hello-world.core-tests` namespace.

At this point you would normally need to create a host page for this
new main file but you don't need to do that because you can visit the
`/figwheel-extra-main/tests` endpoint on your Figwheel server.

In the current example that would be
`localhost:9500/figwheel-extra-main/tests`

If everything went well, when you visit that URL you should see
something like this:

<img class="white-img-border" alt="extra-mains" src="https://user-images.githubusercontent.com/2624/44215467-9daade80-a140-11e8-81bc-90318c7bed2d.png">

If you open the console you will see the test report from `cljs.test`.

As you can see you now have a different application that is being
output from the `dev` build and it has its very own endpoint.

## Why?

Well, if you are working on an application and you want to have the
tests visible in another tab of your browser and you want them to
re-run after all source code changes, this is an ideal way to do that.

You could do that with a [background build][background-builds] but in
that case you have two autobuilding processes running in parallel on
the same codebase which is much more taxing on your CPU.

This is much lighter especially if you start to have a lot of extra
main entry points. For example, you could have all of the following
running at the same time with virtually no extra cost:

* cljs-test-display
* devcards
* backend admin
* tests that run on node (to keep a part of your app node compatible)
* node server

You don't have to use all of the main entry points at the same
time. But they are there, waiting to do your bidding.

Not only do extra mains lighten the compile load, but because they
are all within a single **build** this also allows for extremely simple
REPL focus switching.

## Behavior

The application loaded by the extra main entry point will behave the
same as the main in terms of its relationship to Figwheel. In other
words, the extra main will connect to the Figwheel REPL and get
reloads just like the main build.

This feature will only output the additional ClojureScript bootstrap
file that you will require on your host page, it will not cause any
additional files to be compiled.

> You will need to make sure that you have added all the needed source
> directories to your `:watch-dirs` and to your classpath.

If you add an extra main entry point but don't supply its source
files to the compiler this feature won't work.

## Default endpoint

You will be able to find the default endpoint for your extra main at
`/figwheel-extra-main/[extra-main-id]`. I.E for `:tests` the endpoint
would be `/figwheel-extra-main/tests`.

There is a div on the default host page where you can mount your
application. The app div on the page does not have an id `app` but
rather `app-[extra-main-id]` (for `:tests` that would be
`app-tests`). This is so that you can conditionally mount applications
by testing for the presence of a certain div id. I.E. you can belay
starting your main application if the `app` div isn't there. This
would allow you to require the namespace that mounts your main
application and test the functions in it even if it has top level
code that tries to display itself on the page.

## Usage

`:extra-main-files` will only work under `:optimizations` level
`:none`.

As you can see from the above example when you configure
`:extra-main-files` you pass it a map of name keys mapped to
ClojureScript options maps.

```clojure
:extra-main-files {:tests    {:main hello-world.core-tests}
                   :devcards {:main hello-world.devcards}}
```

These maps are merged with the options map that Figwheel has already
modified and may have injected various `:preloads` and
`:clojure-defines`. This of course creates a problem if you want to
add more `:preloads` or `:clojure-defines`. `:extra-main-files`
handles this by allowing you to add more `:preloads` by supplying an
`:extra-preloads` key. You can do the same with any ClojureScript key
that has a collection value.

```clojure
:extra-main-files {:tests    {:main hello-world.core-tests
                              :extra-preloads [hello-world.dev-methods]}}
```

The options that you supply do not affect how your code is compiled,
they only affect how the final main entry file is generated. The
following options are the only ones that affect how this file is
generated.

* `:main`
* `:output-to`
* `:target`
* `:asset-path`
* `:closure-defines`
* `:preloads`

You can supply an explicit `:output-to` path but Figwheel will supply
one based on the current `:output-to`. It will add an
`"-[extra-main-id]"` to the end of the file name before the
extension. For example, if there is already an `:output-to` file named
`target/public/cljs-out/dev-main.js`, Figwheel will rename that to
`target/public/cljs-out/dev-main-tests.js` if the name of the extra
mains config is `:tests`.

## Simple REPL focus switching

A major question when you have more than one environment is "How do a
get a REPL into my tests?"

The Figwheel REPL allows you to switch focus between environments
(think open tabs) as long as they are in the same build.

You can use the `figwheel.repl/conns` and `figwheel.repl/focus` macros
from the CLJS REPL prompt to do this.

There is a very simple technique for switching the focus of your REPL
though. Reload the browser tab if you want the focus of your REPL to
switch to that environment. The Figwheel REPL will focus on the last
environment to connect to the Websocket. So to switch focus just
reload the browser tab where you want to focus.

You verify this with a `(js/console.log "focus is here")`. You will
see that REPL focus follows that last tab to open **within** a build.















[background-builds]: background_builds
[host-page]: your_own_page
[watch-dirs]: ../config-options#watch-dirs
[extra-main-files]: ../config-options#extra-main-files
