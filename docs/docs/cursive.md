---
title: Cursive
layout: docs
category: docs
order: 19
---

# Cursive

Once you have become comfortable with figwheel-main, you may want to put some effort into integrating it with your preferred editor. This document will discuss how to get your project working nicely with Cursive for a few different configurations.


# CLJS Only 

Support for deps.edn and the CLI is currently only available in the Early Access versions of Cursive and it is not yet able to run figwheel-main directly, so for now we will focus on Leiningen.
 
We're going to start with the same build that we used in the [create-a-build] documentation. Set up your project as described there and confirm that you can run it using
 
```shell
$ lein fig -- -b dev -r
```
> These instructions should work for any project that is able to run figwheel from the command line using a `lein run` command

Add a `dev` folder to your project, then add a `user.clj` file in that folder and copy this code into it 

```
(require '[figwheel.main.api :as fig])
(fig/start "dev")
```

Now we can get our REPL configured. In the Cursive menu, follow these steps

* Navigate to `Run -> Edit Configurations`
* Click the `+` icon and select `Clojure REPL ->  Local`
* Select `Use clojure.main in normal JVM process` 
* Enter `dev/user.clj` into the parameters field. 
* Click `OK` to save the config.

Run your REPL and enjoy figwheel-main in Cursive! Note that this will be a ClojureScript REPL - we will see how to get things working nicely with a combined Clojure + ClojureScript project next.

[create-a-build]: https://figwheel.org/docs/create_a_build.html
