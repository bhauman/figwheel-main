# Quick Start

This document is intended to get you aquainted with the features and
workflow of Figwheel.

This quick start is based on the Clojure CLI tools if you are on
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

# Make a new directory to work in

Make a directory for an example ClojureScript project.

For example in `/Users/[your username]/workspace` you could create a
directory called:

```
workspace$ mkdir hello-cljs
```

# Specifying that you want to use Figwheel

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

Now that you have done that you can get a ClojureScript REPL running
straight away. Make sure you are still in the `hello-cljs` directory
and enter:

```clojure
clj -m figwheel.main
```

This should boot up a ClojureScript REPL and pop open a Browser
window.





[brew]: https://brew.sh/
[CLI Tools]: https://clojure.org/guides/getting_started#_installation_on_mac_via_code_brew_code 

