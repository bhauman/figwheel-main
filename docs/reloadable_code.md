# Writing reloadable code

Figwheel relies on having files that can be reloaded. 

Reloading works beautifully on referentially transparent code and
code that only defines behavior without bundling state with the
behavior.

Let's face it though, we are in JavaScript's domain and mutating state
is just the way stuff gets done.

## Load Time Side Effects

Load time side-effects are the side-effects that change the state of
our JavaScript environment when a file is loaded. As an illustration:
when a file with code in it is loaded, it will be changing the state
by importing that code into the environment. That is a side-effect of
loading the file but it is exactly the side effect we want when we
load a file.

If you are mutating the [DOM][DOM] or your program state at the
top-level of your file so that these state changes will occur when the
file is loaded, we are going to have to put some thought into how we
can safely make these things happen so that we can protect the state
of our running program.

Reloadable code is code who's **load time side-effects** will not
interact with our running program in a destructive way.

The good news: 

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
Every time this definition gets reloaded, the definition will be
redefined and reset back to the empty map `{}`.  This is normally not
desirable. 

The way to fix this is to use `defonce`

```clojure
(defonce state (atom {}))
```

This will fix most situations where you have a top level
definition. It is important to remember that if you change code that
in a `defonce` you won't see the changes after a reload because it
won't be evaluated after the first load.

#### Page initialization

It is not uncommon to initialize and launch your program from top
level of a file. However, you normally won't want to run this
initialization every time this file gets reloaded.

We can solve this problem by using `defonce` again. For example:

```clojure
(defonce initialize-block
  (do
     (initialize-dom)
     (add-body-listeners)
	 true))
```

Above we use the name `initialize-block` but you can use any name
because it will probably never be referenced in your program. We use a
`do` block because `defonce` can only take one form besides the
name. At the end of the `do` block we provide a value `true` so that
the `defonce` is only invoked the first time it is evaluated. If
`add-body-listeners` returns a truthy value we don't have to provide
the `true` at the end.

Running your initialization code the first time the a page loads can
also be accomplished by registering a `load` handler on window.

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

Create a couple of functions named `setup` and `teardown` to help
reset your application on every reload.

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
`^:figwheel-hooks` marker is required to let figwheel know that there
are reload hooks in the namespace.

## Use Reactjs

When you use Reactjs, state is mediated and managed for you. You just
describe what should be there and then React takes care of making the
appropriate changes. For this reason React is a prime candidate for
writing reloadable code. React components already have a lifecycle
protocol that embeds a `setup` and `teardown` pattern in each
component and invokes them when necessary.

It is worth repeating that React components don't have local state, it
just looks like they do. You have to ask for the local state and React in
turn looks this state up in a larger state context and returns it,
very similar to a State Monad.

## Conclusion

There are many ways to make your code reloadable. Once you start
paying attention to load time side-effects, the exercise of writing
reloadable code becomes fairly straight forward.

Since a great deal of programming complexity stems from complex
interactions (side effecting events) between things that have local
state, it is my belief that being more attentive to load time
side-effects, leads developers to reduced the amount of state and
stateful objects that are strewn throughout their programs and take a
more intentional approach to where state is stored and how it is
transformed.

Reloadable code is often simply better code.

[DOM]: https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model/Introduction
