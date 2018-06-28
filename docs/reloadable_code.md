# Writing reloadable code

Figwheel relies on having files that can be reloaded. 

Reloading works beautifully on referentially transparent code and
code that only defines behavior without bundling state with the
behavior. 

If you are using React or Om it's not hard to write reloadable code,
in fact you might be doing it already.

There are several coding patterns to look out for when writing
reloadable code. 

One problematic pattern is top level definitions that have local
state.

```clojure
(def state (atom {}))
```

The `state` definition above is holding an atom that has local state.
Every time the file that holds this definition gets reloaded the state
definition will be redefined and the state it holds will be reset back
to the original state. But with figwheel we are wanting to change our
programs while maintaining the state of the running program.

The way to fix this is to use `defonce`

```clojure
(defonce state (atom {}))
```

This will fix most situations where you have code that is relying on a
definition that has local state. Keep in mind though that if you
change the code that is wrapped in a `defonce` you won't see the
changes, because the identifier won't be redefined.

Complicated object networks wired together with callbacks (Backbone,
Ember, etc.) are also problematic. Instantiating these object callback
networks and then storing them in a global var is yet another version
of this problem.

Functions that maintain local state like counters and such are also
definitions with local state, and as such are problematic.

You also need to look out for common setup code that hooks into the browser.

Often you will see statements like this at the bottom of a file.

```clojure
(.click ($ "a.button") (fn [e] (print "clicked button")))
```

Every time this file gets loaded a new listener will get added to all
the anchor tags with a "button" class. This is obviously not what we
want to happen.

This code is very problematic and points to the why using the browser
APIs directly has always been really difficult. For instance if we make
it so that these hooks are only executed once, like so:

```clojure
(defonce setup-stuff 
  (do 
     (.click ($ "a.button") (fn [e] (print "clicked button")))))
```

When you are live editing code, this doesn't work very well. If you
alter your HTML template any new "a.button" elements aren't going to
have the listener bound to them.

You can fix this by using an event delegation strategy as so:

```clojure  
(defonce setup-stuff 
  (do 
     (.on ($ "div#app") "click" "a.button" (fn [e] (print "clicked button")))))
```

But even with the above strategy you won't be able to edit any of the
code in the setup up block and see your changes take effect.

If you are not using React and you want to build things this way and
have reloadable code we need to create `setup` and `teardown`
functions to be invoked on code reload.  

```clojure  
(defn setup []
   (.on ($ "div#app") "click" "a.button" (fn [e] (print "clicked button"))))

(defn teardown []
   (.off ($ "div#app") "click" "a.button"))

;; define a :on-jsload hook in your :cljsbuild options
(defn fig-reload-hook []
      (teardown)
      (setup))

```

Now you can edit the code in the setup and teardown functions and see
the resulting changes in your application.

In a way you can think of the previous definitions of `setup-stuff` as
functions that have local state of sorts. They are altering and storing
callbacks in the DOM directly and this is why it is so problematic.

This is one of the reasons React is so damn brilliant. You never end
up storing things directly in the DOM. State is mediated and managed
for you. You just describe what should be there and then React takes
care of making the appropriate changes. For this reason React is a
prime candidate for writing reloadable code. React components already
have a lifecycle protocol that embeds `setup` and `teardown` in each
component and invokes them when neccessary.

It is worth repeating that React components don't have local state, it
just looks like they do. You have to ask for the local state and React in
turn looks this state up in a larger state context and returns it,
very similar to a State Monad.

Reloadable code is easy to write if we are very conscious and careful
about the storage of state, state transitions and side effects. Since
a great deal of programming complexity stems from complex interactions
(side effecting events) between things that have local state, it is my
belief that reloadable code is often simply better code.
