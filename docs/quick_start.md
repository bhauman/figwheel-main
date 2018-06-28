# Quick Start

This document is intended to get you aquainted with the features and
workflow of Figwheel.

This quick start is based on the Clojure CLI tools. If you are on
Windows Opersting System you will want to use the [Leiningen version of
this document](/quick_start_lein.html)

## Install the Clojure CLI tools

You will want to install the Clojure tools, they are very helpful in
working with Clojure in general.

Install the [Clojure CLI tools](CLI tools).

If you are on Mac OSX and you can quickly install the Clojure tools
via [homebrew](brew).

In the terminal at the shell prompt enter:

```shell
$ brew install clojure
```

If you've already installed Clojure now would be a good time to ensure
that you have updated to the latest version with:

```shell
$ brew upgrade clojure
```

You can check that everything installed OK by running a Clojure REPL

```shell
$ clj
```

You should a `user=>` prompt where you can enter Clojure code. 

Try entering `(+ 1 2 3)` you should get a response of `6` along with
the next prompt asking you for more code. Type `Control-C` to get out
of the Clojure REPL.

If everything worked well you have successfully installed Clojure!

## Make a new directory to work in

Make a directory for an example ClojureScript project.

For example in `/Users/[your username]/workspace` you could create a
directory called `hello-cljs`:

```
workspace$ mkdir hello-cljs
```

## Specifying that you want to use Figwheel

Figwheel is a library, or rather it is a Jar of Clojure code that you
will use. If you are familiar Ruby's `bundler` and `Gemfile`, Python's
`pip` and `requirements.txt`, or Javascript's `npm/yarn` and
`package.json` then the concept of specifying dependencies in a file
should be familiar to you.

Figwheel is just a dependency and with the Clojure Tools the way you
will specify that you want to have certain libraries available to you
is in the `deps.edn` file.

So in your `hello-cljs` directory create a file `deps.edn` with the
following contents.

```clojure
{:deps {com.bhauman/figwheel-main {:mvn/version "0.1.4"}}}
```

## Starting a ClojureScript REPL

Now that you have done that you can get a ClojureScript REPL running
straight away. Make sure you are still in the `hello-cljs` directory
and enter:

```clojure
clj -m figwheel.main
```

This should boot up a ClojureScript REPL and pop open a Browser
window with a page like this:

![Repl host page in browser](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/fm-repl-host-page.png)

If you see the green "Connected" animation it means that this page is
connected to the REPL that you just launched. This webpage is where all
the ClojureScript expressions that you type into the REPL will be
evaluated.

Speaking of the REPL, if you head back to the terminal window where you
launched Figwheel from, you should now see something like:

![figwheel repl prompt in terminal](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/figwheel-main-repl.png)

If this is the case, you have successfully started a ClojureScript REPL
and you can now type ClojureScript at the `cljs.user=>` prompt.

Let's try some ClojureScript. Type the following expressions at the
prompt as demonstrated in the example REPL session below:

```clojure
cljs.user=> (println "Hello World!")
Hello World!
nil
cljs.user=> (range 5)
(0 1 2 3 4)
cljs.user=> (map (fn [x] (+ 1 x)) (range 5))
(1 2 3 4 5)
cljs.user=> (filter odd? (map (fn [x] (+ 1 x)) (range 5)))
(1 3 5)
cljs.user=> (js/alert "ClojureScript!")
nil
```

That last expression should cause a JavaScript Alert to pop up in the
browser on our REPL host page.

## Amping up the REPL

The REPL we just launched will do fine for trying simple expressions
but if you are learning a new language it's nice to have a REPL that
can 

* syntax highlight the code as you type it
* allows for easy multi-line editing of expressions
* autocomplete the current function name that you are typing
* display the documentation for the function where your cursor is
* display the source code of the function where your cursor is
* allows you to query for functions that are similar to the word under your cursor

My library [Rebel Readline](rebel) provides these features for Clojure REPLs and it
will be very helpful for further explorations of using Figwheel and ClojureScript.

Add `com.bhauman/rebel-readline-cljs 0.1.4` as another dependency in your `deps.edn` file:

```clojure
{:deps {com.bhauman/figwheel-main {:mvn/version "0.1.4"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}}}
```

When you launch `figwheel.main` now it will detect the presence of
`com.bhauman/rebel-readline-cljs` and it will launch a REPL using this
fully functional line reader.

Let's launch `figwheel.main` again, from the `hello-cljs` directory:

```shell
$ clojure -m figwheel.main
```







[brew]: https://brew.sh/
[CLI Tools]: https://clojure.org/guides/getting_started#_installation_on_mac_via_code_brew_code 
[rebel]: https://github.com/bhauman/rebel-readline
