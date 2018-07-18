---
title: Create a Build
layout: docs
category: docs
order: 5
---

# Create a Build

<div class="lead-in">A Figwheel <strong>build</strong>
defines a compilation process, and is going to be your main unit of
configuration.</div>

`figwheel.main` has a CLI that is fairly expressive. However, most
folks who work with it are going want to define a watch/compile
process with a hot-reloading workflow to get the bulk of their work
done.

To faciliate this `figwheel.main` has a `--build` flag.

