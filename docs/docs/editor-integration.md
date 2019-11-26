---
title: Editor Integration
layout: docs
category: docs
order: 20
published: true
---

# Editor Integration

<div class="lead-in"> Undoubtedly one of the major advantages of using
Clojure (and Lisps in general) is that <a
href="https://en.wikipedia.org/wiki/S-expression">S Expressions</a>
and REPLs make it trivial for editors to evaluate code that is present
in an editor buffer.</div>

Experiencing the interactive workflow that results from being able to
evaluate Clojure expressions that are present in your editor's buffer
frame is enlightening. When you have this ability to quickly verify
behavior without the ceremony of restarting an application, you will
start to move forward with more confidence. You have the ability to
check that a function works against glaring edge cases in seconds.
This allows you to think more clearly about what your code is actually
doing, and more clearly about what you actually need.

Many editors provide support for evaluating ClojureScript code via a
REPL connection, however the process for setting this up can seem
complex, mysterious and convoluted.

Why does this complexity exist? The main reason that there is
complexity in connecting a ClojureScript REPL to your editor is that
the environment is just well ... complex. 

The typical path of code that will ultimately evaluated in a Browser
and eventually returned to an editor:

```
Editor Buffer
|
nREPL server                    - Clojure remote REPL network interface
|
Piggieback nREPL Middleware     - re-directs nREPL evals to ClojureScript Compiler
|
ClojureScript REPL Code         - compiles ClojureScript to JavaScript
|
Figwheel REPL Websocket Server  - pushes JavaScript to Browser
|
Browser                         - Figwheel REPL Client code evaluates JavaScript
|
... and back through all the layers
```

The main problem here is that a lot is going on. When we evaluate
Clojure remotely the message just needs to travel to the Clojure
runtime and back. When we are using ClojureScript the code has to
travel to the compiler and then to the browser and back.

## The Major Players

Because of this complexity it is best if we start and understand the
pieces. In practice you will not often have to set these up yourself
but things can become very confusing very quickly if you don't
understand what they are.

### nREPL

[nREPL][nrepl] is server that implements a [protocol][nrepl-ops] that
supports evaluating Clojure code remotely.

It is common for Clojure developers to start an nREPL server inside an
application that is running on a remote server so they can
interactively debug particularly thorny problems.

nREPL provides the server endpoint that most editors integrations talk
to when they want to evaluate Clojure/ClojureScript code.

The simplest way to explain how nREPL works is that it understands a
set of messages. The most important one for our purposes is the
`:eval` message that includes a `:code` attribute. 

nREPL is extensible via middleware that can receive and handle these
messages.

### Piggieback

Piggieback is nREPL middleware that intercepts `:eval` messages and
re-directs them to a ClojureScript REPL for evaluation. So Piggieback
is an extension of nREPL and we have to extend nREPL because it knows
nothing about ClojureScript.

There are two main parts to using Piggieback. First, it has to be
inserted into the middleware of the nREPL server that you are going to
be using. Second, you have to start your ClojureScript REPL with a
call to `cider.piggieback/cljs-repl`. You have to start your REPL this
way so that it can do the bookkeeping necessary to re-direct `:eval`
messages to the ClojureScript REPL.

### ClojureScript REPL

The ClojureScript REPL is written in Clojure and is what allows us to
evaluate ClojureScript code in the first place.

You can run a ClojureScript REPL locally and this is what happens when
you run `clojure -m figwheel.main -b dev -r`. This is pretty simple to
accomplish. However, it is when you wan to run it remotely over nREPL
that things get more difficult.

The ClojureScript REPL is responsible for compiling ClojureScript code
and sending it to a JavaScript environment for evaluation. The
ClojureScript REPL's implementation is independent of the various
JavaScript environments that are available to evaluate JavaScript.
For this reason the ClojureScript REPL needs to be provided a
Connection to a JavaScript environment.

### Figwheel Websocket Server

The Figwheel provides a Websocket server that the Browser or Node can
connect back to. It is over this connection that the ClojureScript
REPL sends JavaScript code for evaluation. And it is also over this
connection that the ClojureScript REPL receives the response as a
result of the evaluation.

## Editors and nREPL

Almost all editor integrations in the ClojureScript ecosystem can
utilize nREPL for communication. This is why understanding it and its
role as a REPL server is important.

There are other ways for editors to connect to Clojure remotely but
currently nREPL integrations tend to provide the most editor
functionality (i.e. documentation, auto-complete, jump to
definition). You can opt to connect to a REPL by another means but the
nREPL integrations are by far the most used.

## Editors and Stream REPLs

A traditional way for editors to communicate with a REPL is to pipe
forms (Clojure expressions in this case), to the STDIN of the REPL and
capture the STDOUT as a response. This form of communication is much
simpler.

Currently both Emacs and Cursive support this type of
communication. Emacs has its
[inf-clojure](https://github.com/clojure-emacs/inf-clojure). Cursive
has particularly good support for this type of REPL communication when
you create a `clojure.main` REPL.

## Editors integrations

There are two major ClojureScript editor integrations, [CIDER][cider]
for [Emacs][emacs] and [Cursive][cursive] for [IntelliJ][intellij].

Don't worry if neither of those are your favorite editors as there are
many other integrations out there. But you should know that
[CIDER][cider] and [Cursive][cursive] account for a whopping 80% of
Clojure editor use and this is not by accident as both [CIDER][cider]
and [Cursive][cursive] are under intense active development.

10% of Clojurists use [Vim Fireplace][vim-fireplace]. The Vim support
for Clojure/Script is nowhere near as extensive as the previous two,
I'll chalk its high rate of adoption up to the sheer popularity of Vi
among the hacker crowd.

[Atom][atom], [VSCode][vscode] and [Sublime Text][sublime2] all have
support for evaluating ClojureScript code from your editor as well.

## Vi users

I know that Emacs is the *other* text editor but I highly recommend
that Vi users use Emacs with `evil-mode`, many Clojurists who are come
from a Vi background have switched and are happy about it.  The main
reason is that Emacs Clojure integration via CIDER is heads and
shoulders above the currently available Vi integration.

See [this blog post](https://www.martinklepsch.org/posts/emacs-and-vim.html) for an experience report on switching to Emacs. 


[nrepl]: https://nrepl.readthedocs.io/en/latest/
[nrepl-ops]: https://nrepl.readthedocs.io/en/latest/ops/
[cider]: https://github.com/clojure-emacs/cider
[emacs]: https://www.gnu.org/software/emacs
[cursive]: https://cursive-ide.com
[intellij]: https://www.jetbrains.com/idea/
[vim]: https://www.vim.org/
[vim-fireplace]: https://github.com/tpope/vim-fireplace
[atom]: https://atom.io/
[sublime2]: https://www.sublimetext.com/2
[vscode]: https://code.visualstudio.com/

















