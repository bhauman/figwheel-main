# Writing reloadable code

Figwheel relies on having files that can be reloaded. 

Reloading works beautifully on referentially transparent code and
code that only defines behavior without bundling state with the
behavior.

Let's face it though, we are in JavaScript's domain and state is going
to get changed.

## Load Time Side Effects

Load time side-effects are the side-effects that change the state of
our JavaScript when a file is loaded. As an example if we defined a
bunch of functions in a file, when the file is reloaded, it will be
setting changing the state by redefining all of the functions that are
already present in the same naemspace in the JavaScript
environment. That is a side-effect of loading the file but it is
benign, as this is a state change that we want.

But if you are mutating the [DOM][DOM], or your program state at the
top-level of your file so that these state changes will occur when the
file is loaded, we are going to have to put some thought into how to
safely make these things happen so that we can preserve the state of
our running program so that it can survive reloading our files.

Reloadable code is code who's **load time side-effects** will not
interact with our running program in a destructive way.

The good news: 

* ClojureScript namespaces are compile to JavaScript Object literals
  which make reloading semantics easy to think about
* it is idiomatic in ClojureScript, to write our functions so that they
  don't exhibit bad load time side-effects
* Reactjs makes it trivial to work with the DOM in a declarative way
  that is very reloadable

The upshot is: if you are using React or one of the ClojureScript
libraries that wraps it, it's not hard to write reloadable code, in
fact you might be doing it already.

## Potential Problems

There are several coding patterns to look out for when writing
reloadable code.

#### Top-level mutable state

One problematic pattern is top level definitions that have local
state.

```clojure
(def state (atom {}))
```

The `state` definition above is holding program state in an atom.
Every time the file that holds this definition gets reloaded, the
state definition will be redefined and the state it holds will be
reset back to the original state (the empty map `{}`). This is
normally not desirable. We are wanting to change our code, and
redefine its behavior while maintaining the state of the running
program.

The way to fix this is to use `defonce`

```clojure
(defonce state (atom {}))
```

This will fix most situations where you have code that is relying on a
definition that has local state. Keep in mind though that if you
change the code that is wrapped in a `defonce` you won't see the
changes, because the identifier won't be redefined.

#### Page initialization

Often you will want to initialize a several and kick off your program
when you load the the root file of your program. Any you don't want to
perform this initialization every time the initializtion code gets
reloaded.

This can be solved with a `defonce` as well.

```clojure
(defonce initialize-block
  (do
     (initialize-dom)
     (add-body-listeners)
	 true))
```

Above we use the name `initialize-block` but you can use any name
because it will probably never be used in your program. We use a `do`
block because a `defonce` can only take one form as a definition. At
the end of the `do` block we provide a value `true` so that the
`defonce` is only invoked the first time it is evaluated. If
`add-body-listeners` returns a value we don't have to provide the
`true` at the end, but it is good practice to keep it there as a
reminder, because as the block grows someone may add an initializer
that returns `nil` and the `defonce` will be invoked every time the
file loads.

Running your initialization code the first time the a page loads can
also be accomplished by registering an `load` handle on window.

```clojure
(ns example.core
  (:require
	[goog.events :as events]))

(events/listen js/window "load"
               (fn [_]
                 (initialize-dom)
                 (add-body-listeners)))
```

## Setup and teardown pattern

If you are working on a page where you are mainly altering the DOM
directly but you still want to use hot reloading, you can normally
manage almost any complex state with a **setup and teardown** pattern.

Create a comple of functions named `setup` and `teardown` to help
your reset your application state on every reload.

A trivial example is to add a listener in `setup` and remove it in
`teardown`.

```clojure
(ns ^:figwheel-hooks example.core
  (:require
    [goog.dom :as dom]
	[goog.events :as events]))

(defn ^:after-load setup []
  (events/listen 
   (dom/getElementByTagNameAndClass "a" "submit-btn")
   "click"
   (fn [e] (handle-submit)))))
   
(defn ^:before-load teardown []
  (events/removeAll
   (dom/getElementByTagNameAndClass "a" "submit-btn"))
   
;; and we'll want to call setup on the first initial load as well.
(defonce init-block
  (do (setup)
      true))
```

The above is taking advantage of Figwheel's metadata markers to
specify functions to call before and after a reload. The
`^:figwheel-hooks` marker is required to let figwheel no that there
are reload hooks in the namespace.

## Use Reactjs

When you use Reactjs, state is mediated and managed for you. You just
describe what should be there and then React takes care of making the
appropriate changes. For this reason React is a prime candidate for
writing reloadable code. React components already have a lifecycle
protocol that embeds `setup` and `teardown` in each component and
invokes them when neccessary.

It is worth repeating that React components don't have local state, it
just looks like they do. You have to ask for the local state and React in
turn looks this state up in a larger state context and returns it,
very similar to a State Monad.

## Conclusion

There are many ways to make your code reloadable. Once you start
paying attention to load time side-effects, the exercise of writing
reloadable becomes pretty straight forward.

Since a great deal of programming complexity stems from complex
interactions (side effecting events) between things that have local
state, it is my belief that being more attentive to load time
side-effects, leads developers to reduced the amount of state and
stateful objects, strewn throughout their programs and take a more
intentional approach to where state is stored and how it is
transformed.

Reloadable code is often simply better code.


[DOM]: https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model/Introduction
