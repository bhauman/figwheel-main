---
title: Installation
layout: docs
category: docs
order: 3
---

# Installing Figwheel

<div class="lead-in"> Figwheel is just a Clojure library and can be
used with several different command line tools. This page will show
how to include Figwheel as a dependency with both <a href="https://leiningen.org/">Leiningen</a> and <a href="https://clojure.org/guides/deps_and_cli">Clojure CLI tools</a>.</div>

<div class="lead-in"> There are two parts to installing Figwheel.
First, you will <strong>install your Clojure tool of
choice</strong>. Second, you will <strong>create a project</strong>
that explicitly requires Figwheel.</div>

## Choosing Leiningen vs. CLI Tools

Let's address the question of whether one should choose
[Leiningen][lein] or [Clojure Tools][cli-tools] to work with
Figwheel. This is largely a matter of taste at this point as these
tools are quite different.

As of the writing of this document [Leiningen][lein] is still the
dominant Clojure dependency/task tool in the Clojure Ecosystem. Most
of the tutorials you find online will be using Leiningen. It has been
around for quite a while and there are innumerable plugins to assist
you with your workflow. It is **very stable**, it works **very well**,
and has a vast array of **plugins** to help you get your work
done. That being said it Leiningen takes a *batteries included* view
on tooling, it has a lot of features, and it runs through a bunch of
logic to prepare a running Clojure environment for you. This
complexity requires more investment to understand how and why
Leiningen is doing what it is doing.

[Clojure CLI Tools][cli-tools] takes the opposite approach. It is
minimal and it requires you to add functionality with different
libraries as you need it. This simplicity gives [CLI Tools][cli-tools]
the immediate advantage of a fast start up time. This simplicity
also means that you can be fairly certain how the Java Environment is
created when you run something with [CLI Tools][cli-tools], which
makes environmental problems easier to understand. The downside of
this, is that features that are built into [Leiningen][lein] need to
be added manually as libraries, if they are even available at all. As
it is still early for [CLI Tools][cli-tools] some of these libraries
are quite new and not as battle tested.

At the end of the day Figwheel will work absolutely fine with either
of these tools, and it is quite likely that you will use it with
both of these tools even in one project.

This document is going to prefer [CLI Tools][cli-tools] because:

* it starts quickly
* it has less configuration options
* it's very capable doing anything you need to do with `figwheel.main`
* does not obscure `figwheel.main`'s usage

**On Windows**

As of this writing [CLI Tools][cli-tools] on Windows is in an alpha state. For now, if you are on Windows you may want to use [Leiningen][lein].

## Install your tool of choice

You will need to install the latest version of [Leiningen][lein] or
[CLI Tools][cli-tools].

**Install Leiningen**

Make sure you have the latest version of [Leiningen][install-lein].

You can check that everything has been installed correctly by running
a Clojure REPL. In your terminal application at the shell prompt enter
the following:

```shell
$ lein repl
```

You should see a `user=>` prompt where you can enter Clojure code.
Type `Control-D` to quit out of the Clojure REPL.

**Install CLI Tools**

First we will want to [install][cli-tools] the `clj` and `clojure` [command line
tools][cli-tools].

If you are on Mac OSX and you can quickly install the Clojure tools
via [Homebrew][brew].

In the terminal at the shell prompt enter:

```shell
$ brew install clojure
```

If you've already installed Clojure, now is a great time to ensure
that you have the latest version installed with:

```shell
$ brew upgrade clojure
```

You can check that everything has been installed correctly by running
a Clojure REPL. In your terminal application at the shell prompt enter
the following:

```shell
$ clj
```

You should see a `user=>` prompt where you can enter Clojure code.
Type `Control-C` to quit out of the Clojure REPL.


## Adding Figwheel as a dependency

You can add `com.bhauman/figwheel-main` by itself as a dependency to
get started. However, you are better off adding Clojure and the
latest version of ClojureScript along with Rebel Readline.

Adding `org.clojure/clojure` as a dependency will help you ensure that
you are using the version of Clojure that you want to be
using. Figwheel requires Clojure 1.9 at least because it uses spec to
validate configuration. CLI Tools adds Clojure 1.9 by default.

Adding `org.clojure/clojurescript` as a dependency will allow to
ensure that you are getting the latest version of ClojureScript not
just the base version that Figwheel will work with.

Adding a `com.bhauman/rebel-readline-cljs` dependency will make the
terminal REPL Figwheel launches much more capable. It is optional but
highly recommended.

You will likely need to add other dependencies like
[Sablono][sablono], [Reagent][reagent] or
[Re-frame][re-frame]. However, these are not needed to work with
Figwheel.

> Many explanations in this document assume that you are currently in
> the root directory of a **project**, which is just a directory that
> contains all the code and other file assets for the program you are
> working on.

### Dependencies with Leiningen

Leiningen requires `project.clj` file in the root of your project. In
your `project.clj` file you will need to add
`com.bhauman/figwheel-main` as a dependency.

As a concrete example, in the root directory of your project place a
`project.clj` that at least contains the following configuration:

```clojure
(defproject example-project "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles 
    {:dev 
      {:dependencies [[org.clojure/clojurescript "1.10.773"]
                      [com.bhauman/figwheel-main "0.2.12"]
                      ;; optional but recommended
                      [com.bhauman/rebel-readline-cljs "0.1.4"]]}})
```

We added all the dependencies to work with ClojureScript and Figwheel
into the `:dev` profile which is enabled by default when you are
working with Leiningen, these dependencies won't be included when you
create an artifact for deployment like a jar or an uberjar.

You can verify this worked by launching a generic `figwheel.main`
ClojureScript REPL.

```shell
$ lein trampoline run -m figwheel.main
```

> When using Leiningen with Rebel Readline you will have to use
> **trampoline**.

A browser window should pop open and back in the terminal you should
see a REPL with a `cljs.user=>` prompt waiting to evaluate
ClojureScript code.

You can quit the REPL by entering `:cljs/quit` or with `Control-D`.

### Dependencies with CLI Tools

In order to work with Clojure CLI tools you will need a `deps.edn`
file in the root directory of your project.

As an example, in the root directory place a `deps.edn` file with the
following contents:

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.9.0"}
        org.clojure/clojurescript {:mvn/version "1.10.773"}
        com.bhauman/figwheel-main {:mvn/version "0.2.12"}
        ;; optional but recommended
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}}}
```

You can verify this worked by launching a generic `figwheel.main`
ClojureScript REPL.

```shell
$ clojure -m figwheel.main
```

A browser window should pop open and back in the terminal you should
see a REPL with a `cljs.user=>` prompt waiting to evaluate
ClojureScript code.

You can quit the REPL by entering `:cljs/quit` or with `Control-D`.

## Aliases

Both Leiningen and Clojure CLI tools supply ways that you can provide
command line options to start your Clojure processes.

Aliases will play a helpful role when working with
`figwheel.main`. You will use them as abbreviations to start build
processes, to run tests, and to build a final minimized production
build.

### Aliases with Leiningen

As we noticed above to get a plain `figwheel.main` REPL we have to use
the following command in the shell:

```shell
$ lein trampoline run -m figwheel.main
```

That's a bit long considering how often we are going to want to use
`figwheel.main`.

You can create a shorter `fig` alias for these command line options by
configuring an `:aliases` key like so:

```clojure
(defproject example-project "0.1.0-SNAPSHOT"
  ...
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]}
  ...
  )
```

With that configuration you can now start a Figwheel REPL by invoking:

```shell
$ lein fig
```

One thing that is very important to remember is that if you want to
pass additional Figwheel CLI options to the `lein fig` command above
you will have to add a `--` before the additional arguments.

Let's display the Figwheel help documentation with the `-h` (or `--help`) option:

```shell
$ lein fig -- -h
```

That should display the help documentation while the `lein fig -h`
command will not.

> Why am I using using aliases here instead of creating a lein plugin?
> The first reason I'm not using a plugin here is that Leiningen boots
> much faster when it doesn't have to dynamically load/compile plugin
> code. Another reason is that `figwheel.main`'s command line options
> are much more expressive than `lein-figwheel`'s and lein aliases are
> better positioned to leverage that expressiveness.

### Aliases with CLI tools

Let's look at how to add aliases with Clojure CLI tools. As an example
let's use the `deps.edn` configuration we started above and add a way
to launch a REPL with `figwheel.main`.

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.9.0"}
        org.clojure/clojurescript {:mvn/version "1.10.773"}
        com.bhauman/figwheel-main {:mvn/version "0.2.12"}
        ;; optional but recommended		
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}}
 :aliases {:fig {:main-opts ["-m" "figwheel.main"]}}}
```

You can use this alias from the project root directory like so:

```shell
$ clj -A:fig
```

You can also add additional flags as you wish. Let's look at the help
documentation with the `-h` (or `--help`) option:

```shell
$ clj -A:fig -h
```

> These are brief instructions to help you to start being productive
> with these Clojure tools. You will benefit greatly by learning more
> about the tools you are using. Please take time to explore the
> documentation and features for [Leiningen][lein] and/or
> [CLI Tools][cli-tools]. It will pay off tremendously.

> It is also important to note that you can also use `Maven` and
> just plain `java` to work with Figwheel.


[clojurescript]: https://clojurescript.org
[cli-tools]: https://clojure.org/guides/deps_and_cli
[lein]: https://leiningen.org/
[install-lein]: https://github.com/technomancy/leiningen#installation
[re-frame]: https://github.com/Day8/re-frame
[reagent]: http://reagent-project.github.io
[sablono]: https://github.com/r0man/sablono
[brew]: https://brew.sh/
