# Figwheel Main

[![Clojars Project](https://img.shields.io/clojars/v/com.bhauman/figwheel-main.svg)](https://clojars.org/com.bhauman/figwheel-main)

Figwheel Main builds your ClojureScript code and hot loads it as you are coding!

![Figwheel heads up example](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/figwheel-main-demo-image.png)

Get a quick idea of what figwheel does by watching the
6 minute [flappy bird demo of figwheel](https://www.youtube.com/watch?v=KZjFVdU8VLI).

Learn even more by watching a 45 minute [talk on Figwheel](https://www.youtube.com/watch?v=j-kj2qwJa_E) given at ClojureWest 2015.

Read the [introductory blog post](http://rigsomelight.com/2014/05/01/interactive-programming-flappy-bird-clojurescript.html).

## Support Work on Figwheel on many other Clojure tools

I contribute a significant amount of time writing tools and libraries
for Clojure and ClojureScript. If you enjoy using figwheel,
[rebel-readline](https://github.com/bhauman/rebel-readline),
[spell-spec](https://github.com/bhauman/spell-spec),
[cljs-test-display](https://github.com/bhauman/cljs-test-display) or
[piggieback](https://github.com/nrepl/piggieback) please consider
making a contribution.

<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=B8B3LKTXKV69C">
<img src="https://s3.amazonaws.com/bhauman-blog-images/Smaller%2BDonate%2BButton%402x.png" width="200">
</a>

## Features

#### Live code reloading

If you write [**reloadable
code**](https://github.com/bhauman/lein-figwheel#writing-reloadable-code),
figwheel can facilitate automated live interactive programming. Every
time you save your ClojureScript source file, the changes are sent to
the browser so that you can see the effects of modifying your code in
real time.

#### Supports Node.js

#### Static file server

The inclusion of a **static file server** allows you to get a decent
ClojureScript development environment up and running quickly. For
convenience there is a `:ring-handler` option so you can load a ring
handler into the figwheel server.

#### Live CSS reloading

Figwheel will reload your CSS live as well.

#### Live JavaScript reloading

Figwheel can live reload your JavaScript source files.

#### Heads up display

Figwheel has a non-intrusive heads up display that gives you feedback
on how well your project is compiling. By writing a shell script you
can click on files in the heads up display and they will open in your
editor!

#### Descriptive Errors with Code Context

Figwheel provides descriptive compiler errors that point to where
the error is in your code.  These errors appear in the REPL as well
as the heads up display.

#### First Class Configuration Error Reporting

It can be quite daunting, when you are configuring a tool for the
first time.  Figwheel currently offers excellent configuration
error reporting that will help you if you happen to misconfigure
something.

#### Built-in ClojureScript REPL

When you launch Figwheel it not only starts a live building/reloading
process but it also optionally launches a CLJS REPL into your running
application. This REPL shares compilation information with the
ClojureScript compiler, allowing the REPL is to be aware of the code
changes as well. The REPL also has some special built-in control functions
that allow you to control the auto-building process and execute
various build tasks without having to stop and rerun `figwheel.main`.

#### Robust connection

Figwheel's connection is fairly robust. I have experienced figwheel
sessions that have lasted for days through multiple OS sleeps. You can
also use figwheel like a REPL if you are OK with using `print` to output
the evaluation results to the browser console.

#### Message broadcast

Figwheel **broadcasts** changes to all connected clients. This means you
can see code and CSS changes take place in real time on your phone and
in your laptop browser simultaneously.

#### Respects dependencies

Figwheel will not load a file that has not been required. It will also
respond well to new requirements and dependency tree changes.

#### Won't load code that is generating warnings

If your ClojureScript code is generating compiler warnings Figwheel
won't load it. This, again, is very helpful in keeping the client
environment stable. This behavior is optional and can be turned off.






## Figwheel Innovations

When it was first released, Figwheel represented an relatively new
front end development experience. It introduced a hot code reloading
workflow that was very fast and worked well, and combined that with a
heads up display which presented compile time messages like exceptions
and warnings in the same browser window where you were working. By
eliminating the code and reload cycle, figwheel had greatly increased
the amount of time that I spent in a productive coding state.

It also introduced a more complete configuration validation that
attempted to go the extra mile and really help the person who made the
mistake. It offered visual feedback on where the mistake was make and
did its best to offer a solution along with relative documentation. I
feel like this was very helpful because the number of questions that I
recieved about how to use figwheel dropped significantly.
