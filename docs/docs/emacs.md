---
title: Emacs
layout: docs
category: docs
order: 21
published: true
---

## Emacs

<div class="lead-in"> Regardless of how you feel about using <a
href="https://www.gnu.org/software/emacs">Emacs</a> as an editor, it
is one of the oldest actively developed text editors. Emacs is also
LISP centric and because of this its native features already support
the interactions we want when we are working on Clojure code.</div>

Learn more about why [Emacs is sexy][emacs-sexy].

Emacs can support LISP editing and REPL interaction natively, without
installing any extra packages or libraries, but the experience sub
optimal. The [CIDER][cider] library provides a comprehensive set of
features to help us edit Clojure code. I'm going to focus on setting
up CIDER from scratch in this guide.

## Installing Emacs

You can follow the
[instructions on the emacs site](https://www.gnu.org/software/emacs/download.html)
to download and install Emacs.

If you are on MacOS don't miss the
[Emacs for OSX](https://emacsformacosx.com) link at the bottom of the
page as downloading the pre-compiled binary is by far the easiest way
to install Emacs on OSX.

Emacs is universally available in most package managers and if you
can't find an expedient way to install it on your current OS, I would
be very very surprised.

## Learning to use Emacs

There is a lot to know about Emacs but you can get by with a
relatively small amount of information at the start.

When Emacs starts it will display a link to a tutorial. Click on that
link and learn enough so that you can get around in the buffer and
execute some commands. Also, make sure that you know how to open and
save files and how to switch from one editor buffer to another. With
this small bit of information you will be able to do a lot of
editing.

[Some good visual guides on how to learn Emacs](http://emacs.sexy/#learn).

[Emacs rocks][emacs-rocks] is a great resource of helpful videos once
you want to do more advanced editing.

Personally, I am not an Emacs expert, I tend to use a very small set
of commands when I'm using Emacs. I tend to use the commands that are
available universally in text buffers and shell line readers. I do
tend use more commands when I'm creating Emacs macros to automate an
otherwise repetitive operation to transform some text.

## Installing CIDER

Now we are going to start installing some Emacs packages. We will use
[`package.el`](https://www.emacswiki.org/emacs/InstallingPackages) to
install some Emacs lisp packages.

First we need to add a snippet to the top of our `~/.emacs.d/init.el`
file. If you don't have an `~/.emacs.d/init.el` you should create one
and make sure that it has the following code at the top.

```elisp
(require 'package)
(add-to-list 'package-archives
	     '("melpa-stable" . "https://stable.melpa.org/packages/") t)
(package-initialize)
```

This will initialize the package system and add the
[melpa-stable package repository](http://stable.melpa.org/#/getting-started)
to the list of repositories to fetch packages from.

If you are editing `init.el` in Emacs you can now either restart Emacs
or you can call the [`eval-last-sexp`][eval-lisp] with `Ctrl-x Ctrl-e`
at the end of each of these expressions one at a time. Either way will
work to execute the code.

Now that we have initialized the package system we can install and
setup some packages.

If you are using MacOS you will want to install a package to import
and use the PATH of your terminal environment in Emacs.

Type `M-x` (meta x) then `package-install` and hit ENTER. You will be
prompted for a package name. At the prompt type the name
`exec-path-from-shell` and hit ENTER.

This will quickly download and install the
[`exec-path-from-shell`](https://github.com/purcell/exec-path-from-shell)
library.

Almost every Emacs package you install will contain instructions that
provide some code to add to your `init.el` file. You can normally find
these instructions in the comments at the top of the `.el` file.

You can find the source code of the installed packages in the
`~/.emacs.d/elpa` directory. With my current system I can see the docs
for `exec-path-from-shell` in
`~/.emacs.d/elpa/exec-path-from-shell-1.11/exec-path-from-shell.el`

When I look there I learn that I should add the following snippet to
my `init.el`:

```elisp
(require 'package)
(add-to-list 'package-archives
	     '("melpa" . "https://melpa.org/packages/") t)
(package-initialize)

;; setup exec-path-from-shell here
(when (memq window-system '(mac ns))
  (exec-path-from-shell-initialize))
```

Now let's install [CIDER][cider] following a similar procedure.

Type `M-x` then `package-install` then ENTER then `cider` and finally
hit ENTER. This will install `cider` and `clojure-mode`.

## Using CIDER from ClojureScript

To prepare to use CIDER in Emacs for the first time you will want to
make sure all the libraries you need to start Figwheel are available
when you call `lein repl` or `clojure` to start a REPL.

If you are using Clojure CLI tools all the libraries you need to
compile and start your Figwheel build should be in the *top level*
`:deps` map, don't place your `com.bhauman/figwheel-main` and other
libraries in an alias.

In a simple `hello-world` example this means that your `deps.edn` file
would look like this:

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.9.0"}
        org.clojure/clojurescript {:mvn/version "1.10.339"}
        com.bhauman/figwheel-main {:mvn/version "0.2.1"}}
 :paths ["src" "resources" "target"]}
```

For Leiningen there is less of a problem because folks normally put
development time dependencies in the `:dev` profile which will be
available on the classpath when we run `lein repl`. Just make sure
that when you run `lein repl` that you can require `(require
'figwheel.main.api)` and run your build via `(figwheel.main.api/start
%build-name)`.

To start editing ClojureScript with CIDER integration, you should
navigate (in Emacs) to a ClojureScript source file in one of your
ClojureScript source directories (For example
`src/hello_world/core.cljs`). Now we will start our Figwheel build
from inside Emacs, with our cursor in the ClojureScript source code
buffer type `M-x cider-jack-in-cljs`. When you type this it will
prompt you for the type of tool you want to start a Clojure REPL
with. If you are using Leiningen type `lein` if you are using the
Clojure tools type `clojure`.

Cider will now ask you what type of ClojureScript REPL you want to
start. You should answer `figwheel-main`.

Next it will ask you the name of your build. At this point you should
simply type the name of your build, in many cases this is `dev`. DO
NOT ACCEPT THE DEFAULT. Also don't type a keyword version of your
build like `:dev`. Unfortunately at the time of this writing there is
a bug in the figwheel-main integration that will cause a failure if
you do not type in the name of your build correctly at the prompt.

At this point you will see Figwheel start up in a REPL buffer in Emacs.

Now you can use this REPL buffer like you would any other REPL. This
is handy in itself because you will be able to copy and paste code
from an editor buffer to the REPL quite easily.

The real magic happens when you experience evaluating ClojureScript
code from a buffer that holds a ClojureScript source file.

If you return to the buffer that contains the ClojureScript file
(where you launched Figwheel from). You can for example at the bottom
of the file you can add a line `(+ 1 2 3)` and then put your cursor at
the end of the line and hit `Ctrl-x Ctrl-e` to evaluate it. You should
see an inline evaluation of you code appear at the end of the line.

CIDER can provides many features to help you as a Clojure/Script
developer. You should take some time to read the
[documentation](https://cider.readthedocs.io) especially the parts
about [interactive
programming](https://cider.readthedocs.io/en/latest/interactive_programming/)
and [using the
REPL](https://cider.readthedocs.io/en/latest/using_the_repl/).

## Paredit

Structural editing makes editing LISP based languages a breeze and in
my opinion much better than editing languages that have an irregular
approach to delimiting expressions or that favor statements over
expressions.

Learning Paredit is an essential part of understanding the LISP
programming experience.

You will want to install `paredit` just like you installed `cider`
above.

Type `M-x` `package-install` hit enter then type `paredit`. Once it is
installed you want to make it start whenever you are editing a
Clojure/Script file. To do this add the following line to your
`.emacs.d/init.el` file after the `(package-initialize)` line:

```clojure
(add-hook 'clojure-mode-hook #'paredit-mode)
```

Checkout [this great animated guide to using Paredit][paredit-anim]

## Troubleshooting

CIDER is under active development and it's not uncommon to run into
troubles when you try to set this up.

You will find helpful folks on the `#cider` and `#emacs` channels on
the [Clojurians Slack](https://clojurians.slack.com/messages). You can
sign up for the Clojurians Slack [here](http://clojurians.net).

If you are just starting to use Emacs my best advice is to keep your
configuration simple at first. Trying to add a ton of functionality to
Emacs without understanding the ramifications of what your are doing
will most likely lead to thrashing about and not having anything work.

At first focus on getting things to work, not on getting them to work
perfectly.


[inf-clojure]: https://github.com/clojure-emacs/inf-clojure
[paredit-anim]:http://danmidwood.com/content/2014/11/21/animated-paredit.html
[emacs-sexy]: http://emacs.sexy
[emacs-rocks]: http://emacsrocks.com
[nrepl]: https://nrepl.readthedocs.io/en/latest/
[nrepl-ops]: https://nrepl.readthedocs.io/en/latest/ops/
[cider]: https://github.com/clojure-emacs/cider
[emacs]: https://www.gnu.org/software/emacs
[cursive]: https://cursive-ide.com
[intellij]: https://www.jetbrains.com/idea/
[vim]:https://www.vim.org/
[vim-fireplace]: https://github.com/tpope/vim-fireplace
[atom]: https://atom.io/ [sublime2]: https://www.sublimetext.com/2
[vscode]: https://code.visualstudio.com/
[eval-lisp]: https://www.gnu.org/software/emacs/manual/html_node/emacs/Lisp-Eval.html

