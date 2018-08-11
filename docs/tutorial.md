---
layout: config-options
---

# Tutorial

This document is intended to get you aquainted with the features and
workflow of Figwheel.

This tutorial is based on the Clojure CLI tools. 

{::comment}
If you are using the
Windows Operating System you will want to use the [Leiningen version of
this document](/quick_start_lein.html)
{:/comment}

## Install the Clojure CLI tools

First we will want to install the `clj` and `clojure` [command line
tools][CLI tools].

If you are on Mac OSX and you can quickly install the Clojure tools
via [homebrew][brew].

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

Try entering `(+ 1 2 3)` you should get a response of `6` along with
the next prompt asking you for more code. Type `Control-C` to get out
of the Clojure REPL.

If you were able to start a REPL, you have successfully installed Clojure!

> We will use the acronym REPL frequently. It stands for [Read Eval
> Print Loop][REPL]

## Make a new directory to work in

Before we start working with ClojureScript, we'll make a new directory
to work in.

```
workspace$ mkdir hello-cljs
```

## Specifying that you want to use Figwheel

Figwheel is a Clojure library, or rather it is a Jar of Clojure code
that you will use. If you are familiar Ruby's `bundler` and `Gemfile`,
Python's `pip` and `requirements.txt`, or Javascript's `npm/yarn` and
`package.json` then the concept of specifying a projects dependencies
should be familiar to you.

When using the Clojure CLI Tools, the way that you specify that you
want to have certain libraries available to you is in a `deps.edn`
file.

So in our new `hello-cljs` directory, we'll create a `deps.edn` file with
the following contents:

```clojure
{:deps {com.bhauman/figwheel-main {:mvn/version "0.1.7"}}}
```

## Starting a ClojureScript REPL

Now let's use Figwheel to start a ClojureScript REPL. Make sure you
are still in the `hello-cljs` directory and enter:

```clojure
clj -m figwheel.main
```

This command should fetch all the dependencies we need, boot up a
ClojureScript REPL and finally pop open a Browser window with a page
like that looks like this:

![Repl host page in browser](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/fm-repl-host-page.png)

If you see the green **Connected** next to the CLJS logo, it means
that this page has successfully connected to the REPL that you just
launched. This webpage is the host JavaScript environment for the
REPL, and is where all the ClojureScript expressions that you type
into the REPL will be evaluated.

Speaking of the REPL, if you head back to the terminal window where
you launched `figwheel.main` from, you should now see something like
this:

![figwheel repl prompt in terminal](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/figwheel-main-repl.png)

If you see this, you have successfully started a ClojureScript REPL
and you can now type ClojureScript at the `cljs.user=>` prompt.

Let's try some ClojureScript. Type the following expressions at the
prompt as demonstrated in the example REPL session below:

```clojure
cljs.user=> (println "Hello World!")
Hello World!
nil
cljs.user=> (range 5)
(0 1 2 3 4)
cljs.user=> (map inc (range 5))
(1 2 3 4 5)
cljs.user=> (filter odd? (map inc (range 5)))
(1 3 5)
cljs.user=> (js/alert "ClojureScript!")
nil
```

That last expression should cause a JavaScript Alert to pop up in the
browser on our REPL host page.

## Amping up the REPL

The REPL we just launched has a simple terminal readline support,
meaning that it can handle editing a single line, and provide history
by hitting the up arrow. This will do fine for trying simple
expressions but we often to have a more fully featured terminal line
reader that can:

* syntax highlight Clojure code as you type it
* facilitate multi-line editing of expressions
* autocomplete the current function name that you are typing
* display function signatures as you type
* display the documentation for the function where your cursor is
* display the source code of the function where your cursor is
* allows you to query for functions that are similar to the word under your cursor

My library [Rebel Readline][rebel] provides these features for Clojure
REPLs. Let's install it now as it will be very helpful while exploring
how to work with Figwheel and ClojureScript.

To use Rebel Readline let's add `com.bhauman/rebel-readline-cljs
0.1.4` as another dependency in the `deps.edn` file:

```clojure
{:deps {com.bhauman/figwheel-main {:mvn/version "0.1.7"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}}}
```

Now when you launch `figwheel.main`, it will detect the presence of
`com.bhauman/rebel-readline-cljs` and use it when starting the
ClojureScript REPL.

To see it in action launch a REPL with `figwheel.main` again, from the
`hello-cljs` directory:

```shell
$ clojure -m figwheel.main
```

> Its better to use the `clojure` command when using
> rebel-readline because the `clj` command provides it's own terminal
> line reader

After entering the above command, a Browser will open and a REPL will
start just like before. However, you will now see the following line a
few lines before the `cljs.user=>` prompt.

```shell
[Rebel readline] Type :repl/help for online help info
```

This confirms that we are using Rebel Readline. 

If you type `:repl/help` command at the prompt, as you type you
immediately notice that `:repl/help` character are syntax
highlighted. Upon hitting enter, you will see a useful reference for
the REPL's capabilities displayed.

![rebel readline help](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/rebel-readline-help.png)

## Rebel Readline feature walkthrough

Let's quickly walk through how to use some of the **Clojure Key
Bindings** listed in the help above.

#### Autocomplete

Type `(ra` at the prompt and don't hit ENTER but hit the TAB key.

You will see a list of choices that you can TAB through and hit ENTER
on your selection. You can also keep typing to narrow the selection
down.

Select `rand-int`, you will now have `cljs.user=> (rand-int` on the
line. 

#### Inspecting functions

Let's find out how to use `rand-int`. Hit `Control-X Control-D` to
bring up the documentation for `rand-int`, upon doing so you will see 

![rebel redline displaying rand-int doc](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/demo-rebel-documentation.png)

One of the more helpful parts of this documentation is the line that
is displaying `([n])`. It's helpful but concise, and we should take a
moment to parse it.

`([n])` is a **list** of function signatures for the `rand-int`
function. It indicates that `rand-int` only has one function signature
`[n]`, which in turn indicates that `rand-int` takes a single
argument `n`. 

As for the type of `n` we can infer from the function name and
documentation that `n` is most likely an integer. But there is another
indication. The Clojure/Script core libraries, and many others, use
the following conventions when naming arguments.

* `f`, `g`, `h` - function
* `n` - integer, usually a size
* `index`, `i` - integer index
* `x`, `y` - numbers
* `xs` - sequence
* `m` - map
* `s` - string
* `re` - regular expression
* `coll` - a collection
* `pred` - a predicate closure
* `& more` - variable number of arguments

The argument name is `n` so we now know when we call `rand-int` we
should supply one integer argument.

If we want to understand `rand-int` further, we can directly examine
its source code.

Now, with your cursor anywhere on the `rand-int` function hit
`Control-X Control-S`. You will see the source code for `rand-int`
displayed.

![rand-int source in rebel](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/rand-int-source-in-terminal.png)

Now that we know that `rand-int` takes a single integer argument let's
call it. 

Let's see if `rand-int` works as we'd expect it to. Complete the REPL
line by typing `(rand-int 5)` and hit enter and you should indeed get
a random number from 0 to 4.

If you hit the up arrow you can get `(rand-int 5)` back at the REPL
prompt and you can hit enter again to get a different random result.

Let's inspect a function with a more complex argument signature.

Enter `(range` at the prompt (use TAB completion if you like). Now
look at it's documentation with `Control-X Control-D` you will notice
that the argument signature is described differently.

```clojure
([] [end] [start end] [start end step])
```

This is showing us that `range` can be called with between 0 and 3
arguments. You can call `range` with no arguments `[]` which will
return an infinite sequence (not recommended at the REPL), you can
call it with one argument `[end]` specifying where a range starting at
`0` should end, you can also call it with the other argument arities
`[start end]` and `[start end step]`.

Let's try this:

```clojure
cljs.user=> (range 4)
(0 1 2 3)
cljs.user=> (range 4 10)
(4 5 6 7 8 9)
cljs.user=> (range 4 10 2)
(4 6 8)
```

We intentionally didn't try to use `(range)` as it will cause the
JavaScript environment to go into a tight loop and prevent further
REPL use or interaction.

It is a good exercise to experience this, so if you're up for it, try
entering `(range)` at the prompt. It should freeze the REPL because
the browser is now stuck in a tight loop trying to iterate through all
the integers up the maximum integer possible.

We can recover from this.

Sometimes it's easy to forget that the REPL is backed by a browser
tab, which is a very simple thing to reset. If your REPL gets stuck
in a tight loop, you can return to the REPL host page, take note of
the URL (most likely `http://localhost:9500/` in our case) and then
explicitly close the tab to kill the tight loop (a page reload often
doesn't work in this case) and then open new tab at the same URL. 

At this point the REPL eval should have timed out and returned to
functioning normally.

> **TIP** If you are not in a tight loop and need to reset the state
> of the REPL at any point you can simply reload the REPL host page
> and that will give you a fresh slate to start from.

Okay, back from the brink? If not you can kill the REPL with
`Control-C Control-D` and restart it.

#### Inline eval

There is one more Rebel Readline feature that I'd like to go over
before moving on.

I'm assuming that you have a running REPL and that you are back at the
`cljs.user=>` prompt.

At the prompt, enter the expression `(+ 1 2 3 4)`, and after that when
your cursor is just after the last `)`, hit `Control-X Control-E`.

You should see that the expression was evaluated and the result `#_=>
10` displayed just under the line where your cursor is. Rebel Readline
allows you to evaluate any expression or sub expression before hitting
ENTER.

Let's try this again, hit ENTER to get back to an empty prompt and
type the expression `(+ (+ 1 2 3) (+ 4 5 6))` and now place the
cursor after the last paren of the sub-expression `(+ 1 2 3)`. If you
hit `Control-X Control-E` at this point you will see that you get the
value `6`. Experiment with evaluating in other parts of the expression
on this line.

What's the value of this feature? It allows you to work on larger
multi-line expressions while verifying that the sub expressions are
doing what you expect them to do.

> Having easy to parse expressions is part of the magic of LISP. It
> allows tools to understand delimited expressions without having to
> implement a complete parser for the language.

#### Multi-line editing

By now, you may have noticed that you can create multi-line expressions
at the REPL prompt in a fairly straight forward way.

Try entering the following expression at the prompt and make sure you
format it so that it spans multiple lines.

```clojure
cljs.user=> (+ 1
       #_=>    2
       #_=>    3)
```

You should notice that you were able to hit ENTER to create newlines
in your expression while the cursor was inside of an **open
expression** and that once you closed the expression (by adding the
last paren), when you hit ENTER it was submitted for evaluation.

This concludes our tour of the Rebel Readline REPL features.

You can now exit the Rebel Readline REPL by hitting `Control-C Control-D`.

## Break Time

Once you have made it this far you have learned how to add
dependencies to `deps.edn` and how to start a `figwheel.main` REPL
with Clojure's CLI tools. You have also, learned how to include
[Rebel Readline][rebel] and how to use it to introspect your
environment.

This is more than enough to justify a break, may I suggest a nice walk
or perhaps a chat with a co-worker nearby?

## Working at the REPL

The ClojureScript REPL is a fantastic tool. It will be very helpful
to understand a basic REPL driven workflow, as it is an important
skill that is complementary to the more automated hot-reloaded
workflow.

We are going to start up a generic `figwheel.main` REPL and then
start to compose, require and reload ClojureScript source files.

Again, we will begin in our `hello-cljs` directory and start a REPL
with `figwheel.main`.

```shell
$ clojure -m figwheel.main
```

Once the REPL has started we will turn our attention to creating a
file that contains some ClojureScript source code.

We'll have to create a file where it can be found, which means it will
have to be named properly and reside on the classpath. 

By default, the Clojure tool adds the local `src` directory to the
classpath.

Create a file `src/hello/cruel_world.cljs` with the following
contents:

```clojure
(ns hello.cruel-world)

(defn what-kind? []
  "Cruel")
  
(js/console.log (what-kind?))
```

The file layout of your project should now be:

```shell
hello-cljs
├── deps.edn
└── src
    └── hello
        └── cruel_world.cljs
```

Take note that in the ClojureScript file we are declaring a namespace
`hello.cruel-world` and that the path to our file mirrors this
namespace and it is rooted in the `src` directory, which is on the
classpath. This is what will allow the ClojureScript compiler to find
and compile our code.

> **TIP**: an extremely common mistake is to forget to replace hypens
> `-` with underscores `_` when creating a namespace file path for a
> source file.

Now that we have created the file with the `hello.cruel-world`
namespace let's require it into our running REPL.

```clojure
cljs.user=> (require 'hello.cruel-world)
nil
```

That `nil` response is exactly what we want. If if there was a problem
finding or compiling the file the REPL will let you know.

Now we can call our `what-kind?` function defined in `hello.cruel-world`:

```clojure
cljs.user=> (hello.cruel-world/what-kind?)
"Cruel"
```

As expected, when we call `(hello.cruel-world/what-kind?)` it returns
the string `"Cruel"`.

But it seems like a bit much to type out the entire namespace for each
call, so let's require `hello.cruel-world` again and create an alias
for it. Remeber that you can hit the up arrow to get back to the
original require statement and edit it.

```clojure
cljs.user=> (require '[hello.cruel-world :as world])
cljs.user=> (world/what-kind?)
"Cruel"
```

As you can see we created an alias `world` for our namespace and we
can now invoke functions from `hello.cruel-world` with the alias
`world`.

Now let's change the file and reload it.

Alter the `what-kind?` function in the `src/hello/cruel_world.cljs`
file so that it returns `"Brilliantly Cruel"`. When you are done the
file should look like this:

```clojure
(ns hello.cruel-world)

(defn what-kind? []
  "Brilliantly Cruel")
  
(js/console.log (what-kind?))
```

Now we can head back to the REPL prompt and reload the
`hello.cruel-world` namespace like so:

```clojure
cljs.user=> (require '[hello.cruel-world :as world] :reload)
nil
```

And then verify that our changes were loaded:

```clojure
cljs.user=> (world/what-kind?)
"Brilliantly Cruel"
```

There is one more quick trick that you will find helpful while working
at the REPL. You can change your current namespace (`cljs.user`) to
another loaded namespace.

For instance, we can change into the `hello.cruel-world` namespace
like so:

```clojure
cljs.user=> (in-ns 'hello.cruel-world)
nil
hello.cruel-world=>
```

You will see the prompt change and now that your current namespace is
`hello.cruel-world` you can call functions in that namespace without
needing to provide a namespace.

For example:

```clojure
hello.cruel-world=> (what-kind?)
"Brilliantly Cruel"
```

Remember that you are working in a Browser environment so you can
interact with the browser DOM as well.

```clojure
hello.cruel-world=> (js/document.getElementById "app")
#object[HTMLDivElement [object HTMLDivElement]]
```

There is an HTML element with an `id` of `"app"` available on the
REPL host page. This element contains all of the markup and style for
the helper application. Let's override the helper app content, with
the string returned by our `what-kind?` function.

Make sure the REPL host page is visible while you type the folowing.

```clojure
hello.cruel-world=> (def app-element (js/document.getElementById "app"))
#'hello.cruel-world/app-element
hello.cruel-world=> (set! (.-innerHTML app-element) (what-kind?))
"Brilliantly Cruel"
```

After that, on the REPL host page, you should see the helper app
dissappear and be replaced by the words `Brilliantly Cruel`.

Well that was just a brief tour of a REPL driven workflow. This was a
simple example but it starts to demonstrate that you could possibly
get quite far into an application just using this simple setup.

> **TIP**: There is a fantasic guide on
> [Programming at the REPL][program-at-repl] on the official Clojure
> website. Much of the guide is also directly applicable to
> ClojureScript, when you are ready to learn more it is an excellent
> resource.

> **TIP**: Much of what you learned above applies equally well to the
> Clojure language. So if you would like to try your hand at Clojure,
> as well, you can get a working Rebel Readline Clojure REPL by typing
> `clojure -m rebel-readline.main` in the `hello-cljs` directory. If
> you want to create and load Clojure files, everything is the same as
> above except Clojure files end with `.clj`

## Starting the REPL already intialized with your code

While it is perfectly workable to `require` all the code you need at
the REPL, most of the time you will want to initialize the REPL with
something specific already loaded.

You can accomplish this by using the following command:

```clojure
$ clojure -m figwheel.main --compile hello.cruel-world --repl
```

This will compile and load the `hello.cruel-world` namespace into the
REPL environment. If `hello.cruel-world` required other namespaces
they would get loaded as well.

You will notice some differences in the REPL startup output this time:

![compile terminal output](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/fm-cruel-world-compile.png)

The output looks similar to launching `figwheel.main` without any
arguments, but there are some important differences.

The first thing of note is this warning:

![target classpath warning](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/fm-cruel-world-compile-warning.png)

This is expected. When we started the `figwheel.main` REPL without any
arguments, the compiled ClojureScript files are output into a
temporary directory. This directory is automatically added to the
classpath so that the compiled assets can be found and served by the
built-in webserver.

Running `figwheel.main` without arguments indicates that you are
likely experimenting with some code, not working on a local project.

When one starts compiling namespaces it indicates that we are
commiting to a project, and thus we will want our compiled artifacts
to be local to the project (for later use). It is also time to start
being explicit about what is on our classpath. In order to provide a
smooth initial experience, `figwheel.main` will try to be helpful and
append the classpath with paths that should likely be there. When it
does add a classpath, Figwheel will print a warning because it is best
that you manage the classpath explicitly so that things work properly
when you are not using `figwheel.main`.

We can fix this classpath warning by adding both `"src"` and
`"target"` to the `:paths` key in our `deps.edn` file:

```clojure
{:deps {com.bhauman/figwheel-main {:mvn/version "0.1.7"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}}
 :paths ["src" "target"]}
```

We have to include `"src"` as well as `"target"` because when you add
a `:paths` key to `deps.edn` the `src` path is no longer implicitly
added.

Now we will return to run our REPL again from the `hello-cljs`
directory.

```clojure
$ clojure -m figwheel.main --compile hello.cruel-world --repl
```

You should see the familiar output when starting a `figwheel.main`
REPL minus the warning about `target`.

You should also see that your ClojureScript source code is now being
compiled to the local `target` directory and not to a temporary
directory.

You will also notice this line:

![watching line in output](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/watching-line.png)

This line indicates that Figwheel is now watching the ClojureScript
files in the `src` directory for changes. When a change is detected
Figwheel will compile and reload the changed code into the JavaScript
environment, without us needing to use `(require 'hello.cruel-world
:reload)`.

Let's try this out.

First let's verify that `hello.cruel-world` is loaded.

```clojure
cljs.user=> (hello.cruel-world/what-kind?)
"Brilliantly Cruel"
```

Looks good! We successfully started the repl with our namespace
already available so we don't have to explicitly require it.

Now let's change the namespace. Go to the `src/hello/cruel_world.cljs`
source file and change the `what-kind?` function so it returns `"Cruel
No More"` and then save the file. It should look like this:

```clojure
(ns hello.cruel-world)

(defn what-kind? []
  "Cruel No More")
  
(js/console.log (what-kind?))
```
 
When you save the file, you should see a green `Successfully compiled`
message appear in the REPL. You can now check the REPL to see that
your code has indeed been reloaded automatically. (Don't forget that
you can use TAB to use autocomplete to help you enter the following)

```clojure
cljs.user=> (hello.cruel-world/what-kind?)
"Cruel No More"
```

This verifies that our code has been loaded. Now, we don't have to
explicitly reload a namespace as we work, we can simply save the file
we are working on and it will be reloaded instantly.

Automatically reloading code on save makes a significant impact on
one's workflow.

## Feedback is King

We'll take a look at the ways in which Figwheel provides feedback
while you are working.

When we have a workflow where our files are being watched and compiled
as we work on them, we have an opportunity to detect syntax/compile
errors earlier than if we waited to reload by hand.

Figwheel provides feedback for compile time errors and warnings in the
REPL and in the browser.

####  REPL Feedback

To experience this, arrange your REPL terminal and editor windows so that
they are side by side.

![image of terminal next to editor](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/repl-editor-side-by-side.png)

Now in the editor edit the the `src/hello/cruel_world.cljs` file again
by adding some bad code on a line at the end of the file that looks
like this `defn hello`. When you are done the file should look like
this:

```clojure
(ns hello.cruel-world)

(defn what-kind? []
  "Cruel No More")
  
(js/console.log (what-kind?))

defn hello
```

Now when you save the file you should see some warnings that look like
the following feedback in the REPL.

![screen shot of defn hello warnings](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/terminal-defn-hello-warnings.png)

Getting feedback like this as you are coding is more timely than
waiting until a file is compiled and loaded by hand and allows you to
concentrate more on the problem with less interuptions from the
process.

Now let's cause a compile error by adding parenthesis around `defn
hello` so that it looks like this:

```clojure
(ns hello.cruel-world)

(defn what-kind? []
  "Cruel No More")

(js/console.log (what-kind?))

(defn hello)
```

Upon saving the file you will see a compile error in the REPL.

![image of compile error](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/def-hello-compile-error.png)

So, we've demonstrated a workflow where you can edit your code and
quickly get feedback from the REPL that informs you of any compile
errors.

#### Heads-up display feedback

You may have noticed this already but if you go back to the "Default
Figwheel Dev Page" in the Browser you will also see the same error
above displayed in Figwheel's heads-up display.

![image of error in heads up display](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/defn-hello-error-heads-up.png)

Displaying compile time errors and warnings in the browser is an
important feature of Figwheel as we can only keep our attention in so
many places as we work. It's far too easy to miss compile time error
messages if they are tucked away in a terminal somewhere while you are
focused on the front-end you are working on.

Now, keeping the browser window visible, change the file and remove
the parenthesis from around the `(defn hello)` and hit save. You will now
see the compiler error replaced with a warning like this:

![image of warning](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/defn-hello-warning-heads-up.png)

And finally delete the remaining `defn hello` line and save the file
once again.

You will notice that the warning goes away.

## The Build

The previous examples demonstrated how to start productively working
with ClojureScript with very little configuration. However, when we
are working on a larger project we will normally need to customize our
environment beyond what is possible with the `deps.edn` file and the
`figwheel.main` command line options.

There is also a need to **name** our *build* configurations so that we
can identify them and help Figwheel keep their compiled artifacts
separate.

To meet these needs `figwheel.main` relies on a **build file** to specify
ClojureScript compiler options, figwheel options, and a stable name
for the configuration.

To help explain what a build file is and how it works, we'll start
with an example.

First let's reset things. Make sure you quit the REPL and then
delete the `target/public` directory to get rid of our compiled
assets.

After that, in the `hello-cljs` directory, place a new build file
called `cruel.cljs.edn` with the following contents:

```clojure
{:main hello.cruel-world}
```

The contents of the `cruel.cljs.edn` build file are specifying the
[ClojureScript compiler options][compiler-options] for our cruel world
project. These options will be passed to the ClojureScript compiler
whenever source code needs compiling. There are quite a few
[compiler options][compiler-options] but the above is all we'll need
as `figwheel.main` provides enough default compiler options to allow
you to get started working.

The `hello-cljs` directory should now look like this:

```shell
hello-cljs/
├── cruel.cljs.edn
├── deps.edn
└── src
    └── hello
        └── cruel_world.cljs
```		

It is easy to use a build file. From the `hello-cljs` directory, start
`figwheel.main` again, using our new build file:

```shell
$ clojure -m figwheel.main --build cruel --repl
```

This will create a working environment that is practically identical
to when we previously called `clojure -m figwheel.main --compile
hello.cruel-world --repl`.

You will notice that if you edit the `src/hello/cruel_world.cljs` file
you will see the same hot reloading and feedback behavior as before.

In the startup output for the REPL, you will notice a difference in the
names of the compiled artifacts.

![image of compiled artifacts output lines](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/build-name-output-difference.png)

Before the compiled output was getting compiled to
`target/public/cljs-out/main.js` and now it's getting compiled to
`target/public/cljs-out/cruel-main.js`.

As your project grows it will more than likely have more than one
build, and having a name to identify and separate your build's
compiled artifacts is very helpful.

Also, as you progress with your project there is normally a need to
add more specific ClojureScript compiler options rather than rely the
default ones that `figwheel.main` supplies.  With a build file like
`cruel.cljs.edn` you will have a logical place to add these options as
needed.

You will also have a place to add [`figwheel.main` configuration
options][figwheel-options].

As an exercise, we'll add an option to modify `figwheel.main`'s
behavior. Let's say we'll be mainly relying on the heads up display
feedback and that we'd like to make figwheel print the compiler errors
more concisely in the REPL.

We have seen that compile errors normally print out with some code
context and a pointer to where the error was detected.

![image of verbose error](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/verbose-defn-error-message.png)

We can make errors print out without the code context with the
[`:log-syntax-error-style`][syntax-error-style] option.

We'll configure this in our `cruel.cljs.edn` file. Go ahead and edit
the build file to look like this:

```clojure
^{:log-syntax-error-style :concise}
{:main hello.cruel-world}
```

The caret `^` character is very important and signifies that we are
adding **metadata** to the map that follows it. 

> Learn more about Clojure Metadata [here][learning-metadata] and see the
> [official reference][metadata]

Now when we start up the `cruel` build with `figwheel.main` again:

```shell
$ clojure -m figwheel.main --build cruel --repl
```

If we type an error in our code we'll see a much more concise message
like this:

![concise message image](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/concise-defn-error.png)

In summary, a build file is a useful way to specify compiler and
Figwheel options and provide that configuration a useful name. I
consider this the most useful way of working with `figwheel.main`.

#### figwheel-main.edn

Sometimes you will have several builds and you will want to share some
or all of your `figwheel.main` configuration between them. You can do
this by creating a `figwheel-main.edn` in your project directory.

For our current example, we can move our `:log-syntax-error-style`
configuration out of our `cruel.cljs.edn` file and place it in a
`figwheel-main.edn` file. Like this:

Example `hello-cljs/figwheel-main.edn`:
```clojure
{:log-syntax-error-style :concise}
```

If you try this you will notice that the `:log-syntax-error-style
:concise` configuration still works. This configuration will now take
effect without out the need to specify it in any build files. 

It is important to note that the **metadata** configuration that you
add to your build files will override the configuration in
`figwheel-main.edn`

## Packaging up a single compiled artifact for production

The ClojureScript compiler has four `:optimizations` modes `:none`,
`:whitespace`, `:simple` and `:advanced`. These determine the type of
output that the complier produces. For example, the default mode that
we have been using so far is `:none`. This mode produces many
individual files when we compile our ClojureScript.

If you look at the `target/public/cljs-out/cruel` directory you will
see these files.

```shell
target
└── public
    └── cljs-out
        ├── cruel
        │   ├── cljs
        │   │   ├── core.cljs
        │   │   ├── core.js
        │   │   ├── core.js.map
        │   │   ├── pprint.cljs
        │   │   ├── pprint.cljs.cache.json
        │   │   ├── pprint.js
        │   │   ├── pprint.js.map
        │   │   ├── stacktrace.cljc
        │   │   ├── stacktrace.cljc.cache.json
        │   │   ├── stacktrace.js
        │   │   ├── stacktrace.js.map
        │   │   ├── test.cljs
        │   │   ├── test.cljs.cache.json
        │   │   ├── test.js
        │   │   └── test.js.map
        │   ├── cljs_deps.js
        │   ├── cljsc_opts.edn
        │   ├── clojure
        │   │   ├── data.cljs
        │   │   ├── data.cljs.cache.json
        │   │   ├── data.js
        │   │   ├── data.js.map
        │   │   ├── set.cljs
        │   │   ├── set.cljs.cache.json
        │   │   ├── set.js
        │   │   ├── set.js.map
        │   │   ├── string.cljs
        │   │   ├── string.cljs.cache.json
        │   │   ├── string.js
        │   │   ├── string.js.map
        │   │   ├── walk.cljs
        │   │   ├── walk.cljs.cache.json
        │   │   ├── walk.js
        │   │   └── walk.js.map
... and many more
```

When we want to deploy our final project we are normally going to want
to produce a single JavaScript file, to make load times more efficient.

All of the other `:optimizations` modes produce a single file as output.

* `:whitespace` only optimizes whitespace
* `:simple` only makes safe simple optimizations in addition to
  optimizing whitespace
* `:advanced` performs a state of the art JavaScript optimization

The `:advanced` optimization level is normally what you will want when
you are ready to deploy code.

Let's first output a `:whitespace` optimized file.

First lets delete the `target/public` directory.

```shell
$ rm -rf target/public
```

Because we just want to compile a file once and not start a watching
process we are going to use the `figwheel.main` `--build-once`
flag. We will also specify the `--optimizations` level as
`whitespace`.

Here's the long version of the command:

```shell
$ clojure -m figwheel.main --optimizations whitespace  --build-once cruel
```

and here's the command using abbreviated flags:

```shell
$ clojure -m figwheel.main -O whitespace -bo cruel
```

> **TIP**: you can use `clj -m figwheel.main --help` to learn all of
> the `figwheel.main` CLI flags and their abbriviations

If you execute the above command and then view
`target/public/cljs-out/cruel-main.js` you will see that it is now a
large file that bundles all the required code in it.

You may think that this is a lot of code considering the size of our
source file. Unfortunately we are getting all the code that is needed
by the core ClojureScript library even though we are not using it.

We can remedy this by using `advanced` compiliation which will perform
a static code analysis and perform DCE (Dead code elimination) and
remove any code that is not used.

```shell
$ clojure -m figwheel.main -O advanced -bo cruel
```

If you now view the contents of `target/public/cljs-out/cruel-main.js`
and see that it is now significantly smaller.

## index.html

In all of the examples above, we have used the host HTML page provided
by the `figwheel.main` helper application. You will very quickly get
to the point where you want to supply your HTML page to host your
application.

Let's look at how you can provide your own `index.html` to host your
application.

The webserver used by figwheel uses the classpath to find static
files. Anything that is on the classpath in a `public` directory will
be served as a static file.

Add a `resources` directory to the classpath by adding `"resources"`
to the `:paths` key in you `deps.edn` file. When you finish the file
should look like this:

```clojure
{:deps {com.bhauman/figwheel-main {:mvn/version "0.1.7"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}}
 :paths ["src" "target" "resources"]}
```

Now let's make a `resources/public` directory that we will use as a
web-root directory for our web assets.

```shell
$ mkdir -p resources/public
```

Now, we'll add a CSS file, because let's face it we're gonna need it.

Create a CSS file `resources/public/css/style.css` with the following
content:

```css
/* style */
body {
	color: red;
}
```

Then create an `resources/public/index.html` file with the following in it:

```html
<!DOCTYPE html>
<html>
  <head>
    <!-- this refers to resources/public/css/style.css -->
    <link href="css/style.css" rel="stylesheet" type="text/css">
  </head>
  <body>
    <div id="app"></div>
	<!-- this refers to target/public/cljs-out/cruel-main.js -->
    <script src="cljs-out/cruel-main.js"></script>
  </body>
</html>
```

Once this file is in place, when you start the `cruel` build with
`figwheel.main` you will now see your `index.html` file rather than
the default helper app host page.

#### Live Reload CSS

You can get `figwheel.main` to watch and reload the css file above by
adding `:css-dirs ["resources/public/css"]` configuration to
your `cruel.cljs.edn` file as follows:

```clojure
^{:css-dirs ["resources/public/css"]}
{:main hello.cruel-world}
```

## Conclusion

You made it! Thanks for taking the time to learn more about
`figwheel.main`. I've hopefully provided you enough information to get
started with your ClojureScript explorations.

[REPL]: https://en.wikipedia.org/wiki/Read%E2%80%93eval%E2%80%93print_loop
[brew]: https://brew.sh/
[CLI Tools]: https://clojure.org/guides/getting_started#_installation_on_mac_via_code_brew_code 
[rebel]: https://github.com/bhauman/rebel-readline
[program-at-repl]: https://clojure.org/guides/repl/introduction
[compiler-options]: https://clojurescript.org/reference/compiler-options
[figwheel-options]: https://github.com/bhauman/figwheel-main/blob/master/doc/figwheel-main-options.md
[syntax-error-style]: https://github.com/bhauman/figwheel-main/blob/master/doc/figwheel-main-options.md#log-syntax-error-style
[learning-metadata]: https://en.wikibooks.org/wiki/Learning_Clojure/Meta_Data
[metadata]: https://clojure.org/reference/metadata
