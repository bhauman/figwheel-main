---
title: Introduction
layout: docs
category: docs
order: 1
---

# Introduction

<div class="lead-in"> Figwheel is a tool that compiles your
ClojureScript with a focus on providing high quality feedback in a
timely manner. The ultimate goal of Figwheel is to <i>get out of your
way</i> so that you can just write code. </div>

Figwheel was a pioneer in hot-reloading and provides remarkably fast
workflow for a compile-to-JavaScript language, several of its
features are now common in other tools.

## Features

Figwheel is more than a simple wrapper around the ClojureScript
compiler. Figwheel compiles more than 4 years of thought, reflection
and user feedback into its codebase.

Figwheel provides a slew of features in the name of providing better
ClojureScript experience.

* fast intelligent hot-reloading
* heads up display with compiler feedback
* parsed error messages with code context
* informative configuration errors
* minimal configuration
* great REPL experience
* Nodejs support
* built-in server to get started quickly
* helper application to provide contextual help
* live CSS reloading
* simplicity of only being a library
* minimal dependencies
* grokable codebase
* works equally well with Leiningen and Clojure CLI Tools

## Figwheel Main vs. lein figwheel

You are currently looking at documentation for Figwheel Main which is
the latest iteration of Figwheel. The original
[`lein-figwheel`][lein-figwheel] is still relevant and widely used.

Figwheel Main is a complete re-write of `lein-figwheel` and the
following advantages:

**Starts faster**

The Figwheel Main codebase is much smaller. A lot of attention was
placed on minimizing dependencies and as a result it starts much
faster.

**REPL connection**

Figwheel Main's REPL evaluates on just one connected JavaScript
client whereas `lein-figwheel`'s REPL evaluated on all connected
clients with the same build-id.

Figwheel Main's REPL will let you select which client environment to
use when you have multiple connected clients.

**Minimal configuration**

Figwheel main makes intelligent choices for your ClojureScript
compiler options. This allows you to get started with very little
configuration.

**Provides a `cljs.main` CLI interface**

Utilizing the same CLI from `cljs.main` saves you from having to learn
yet another way to compile ClojureScript. It's a design goal to keep
the experience consistent between the two tools.

The CLI is also much more expressive than `lein-figwheel`, which
translates into less configuration. The CLI will allow you to create
test builds and deployment builds without needing a separate
configuration for each build.

**Better Nodejs experience**

With Figwheel Main you can go from zero to a fully hot reloading
Node workflow with very little configuration. Figwheel Main also
starts a Node process for you when you launch a build.

**Simpler codebase**

Figwheel Main has a much smaller codebase. It is more approachable and
forgiving when one wants to understand how it works.

**much more**

ClojureScript, Clojure and the wider ecosystem have all changed over
the last 4 years. Another thing that has changed is my understanding
of how to write a Clojure tool. Figwheel Main incorporates the
influences of all these changes and the result is a fundamentally
different codebase with too many improvements to list here.

> **Why not just incorporate these changes into `lein-figwheel`?**
>
> It would be impossible to incorporate all the changes presented in
> Figwheel Main while maintaining backwards compatibility with
> lein-figwheel.

## About this Document

This documentation is intended to be a complete guide to using
Figwheel Main. It lives in the
[Figwheel Main Github repository][fig-main-repo] under the
[`docs/docs` directory][fig-main-docs-repo]. If you notice any
mistakes or errors please submit a PR.

### Conventions

For simplicity this document will use **Figwheel** and **Figwheel
Main** interchangeably. Both will be referring to the subject of this
document: **Figwheel Main**.

[fig-main-repo]: https://github.com/bhauman/figwheel-main
[fig-main-docs-repo]: https://github.com/bhauman/figwheel-main/tree/master/docs/docs
[lein-figwheel]: https://github.com/bhauman/lein-figwheel




