---
title: Live CSS
layout: docs
category: docs
order: 7
---

# Live CSS reloading

<div class="lead-in">Figwheel will hot reload your CSS files as you edit them.</div>

You need to do three things for this to work:

* include a link to your CSS in your host page HTML
* ensure your CSS is in a `public` directory on the classpath
* configure the [`:css-dirs` config option][css-dirs]

## Including link to CSS file

Include a link tag to your CSS on your [HTML host page](your_host_page).

```html
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <!-- we are including the CSS here -->
    <link href="css/style.css" rel="stylesheet" type="text/css">
  </head>
  <body>
    <div id="app">
    </div>
    <script src="cljs-out/dev-main.js" type="text/javascript"></script>
  </body>
</html>
```

## Ensuring your CSS file can be served

The above example will work if you place your CSS file in a
`public/css` directory on the classpath. Since the `resources`
directory is normally on the classpath by convention we can place our
CSS files `resources/public/css`.

## Tell Figwheel to watch and reload CSS

You will use the [`:css-dirs` config key][css-dirs] to tell Figwheel
to which directories to watch for CSS file changes.

You can do this one of two ways: in the **build file** or in the
`figwheel-main.edn` file.

As and example let's assuming a `dev.cljs.edn` build file. You can add
`:css-dirs` config to the metadata in the build file like so:

```clojure
^{:css-dirs ["resources/public/css"]}
{:main example.core}
```

Or you can set it for all builds and compiles in the `figwheel-main.edn`:

```clojure
{:css-dirs ["resources/public/css"]
 ;; rest of the config
 }
```

Then you restart your build:

```shell
clojure -m figwheel.main -b dev -r
```

Now you should be able to edit and save the
`resources/public/css/style.css` file and see the changes rendered
live in your application.

[css-dirs]: ../config-options#css-dirs
