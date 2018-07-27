---
title: Getting Help
layout: docs
category: docs
order: 2
---

# Getting Help

<div class="lead-in">Here are some resources you can turn to when you
need help.</div>

The is an active community on the
[Clojurians Slack](http://clojurians.net). There are many Slack
channels devoted to ClojureScript and various ClojureScript libraries.

When looking for help with Figwheel and ClojureScript, you should
start with the **#figwheel-main** and **#clojurescript** channels.

There is also an active
[ClojureScript Google Group](https://groups.google.com/forum/#!forum/clojurescript).

[ClojureVerse](https://clojureverse.org/) is a very active forum.

Don't forget to use the
[ClojureScript website](https://clojurescript.org/) as it has many
guides and references.

The [ClojureScript cheatsheet](http://cljs.info/cheatsheet/) is very
helpful, as is this list of
[ClojureScript synonyms](https://kanaka.github.io/clojurescript/web/synonym.html)

## Learning ClojureScript

There is a **lot to learn** when you are first learning ClojureScript,
I recommend that you bite off very small pieces at first. Smaller
bites than you would take when learning other languages like
JavaScript and Ruby. 

There are a couple of common pitfalls that happen to folks when they
try to learn ClojureScript.

### Trying to learn to much

First, folks try to learn too many things in parallel. They try to
learn functional programming, persistent datastructures, ClojureScript
tooling, hot reloading, using a browser connected REPL, Reactjs, a
ClojureScript React wrapper like Reagent, Javascript, Ring(ie. Rack
for Clojure), setting up a Clojure webserver all at the same time.

This layering strategy may be an efficient way to learn when one is
learning an imperative programming language like Python, Ruby or
JavaScript. It becomes losing strategy when you start to work with
ClojureScript. The biggest reason for this is that the language itself
is significantly different than these imperative languages. There are
enough differences that you will find it difficult to associate these
new patterns with the programming patterns that you are accustomed
to. This unfamiliarity is easily compounded when you then add several
other paradigm breakers like Reactjs and hot reloading to the mix.

The solution is to keep things as simple as possible when you start
out. Choose finite challenges like learning enough of the
ClojureScript language to where you can
[express complex things](http://www.4clojure.com/) before you attempt
to manipulate a web page. From there attempt to simply manipuate the
DOM with the
[`goog.dom` API](https://google.github.io/closure-library/api/goog.dom.html). Once
you have a handle on that start exploring
[React](https://reactjs.org/) and how to use
[`sablono`](https://github.com/r0man/sablono) to create a dynamic web
site. Then start exploring Clojure and create a simple webserver with
[Ring](https://github.com/ring-clojure/ring).

### Trying to create a sweet dev environment

Another thing that folks do, is they try to duplicate their current
development environment. As developers, we become very accustomed to
having tools set up just the way we like them. I'm very sympathetic to
this.

I however strongly advise that you not invest too much time trying to
set up a sweet development environment, and rather invest that time
into learning the ClojureScript language with a simple set of tools. 

The Clojure landscape currently has a diverse set of tools that is
constantly in flux. As a result, it's very difficult to suss out which
ones will actually help you. If you spend a lot of time evaluating all
these options it can become very frustrating. If you wait a while, and
use simple tools you will have much more fun actually using the
language itself and this experience will provide a foundation to make
better tooling choices.

If you are new Clojure and ClojureScript I'd advise that you start
with a terminal REPL and a decent editor.

