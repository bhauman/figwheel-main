# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Figwheel Main is a ClojureScript tooling library providing hot code reloading and live development. It is part of a multi-repo ecosystem alongside sibling projects in `../figwheel-core/` and `../figwheel-repl/`. All three repos are versioned in lockstep (currently 0.2.21-SNAPSHOT).

## Build System

Dual build system: **Leiningen** (`project.clj`) and **Clojure CLI** (`deps.edn`).

For local development with sibling projects, uncomment the `:local/root` entries in `deps.edn` and comment out the `:mvn/version` entries for `figwheel-core` and `figwheel-repl`.

## Common Commands

```bash
# Run tests
lein test                      # or: make testit (cleans first)

# Install all three projects locally (figwheel-core, figwheel-repl, figwheel-main)
make install

# Clean build artifacts
make clean

# Build the helper app (compiled to helper-resources/)
clojure -A:build-helper

# Build documentation
make docs                      # requires Ruby for kramdown markdown processing

# Format code
clojure -A:cljfmt check src/
clojure -A:cljfmt fix src/

# Deploy to Clojars (all three repos)
make deploy
```

## Architecture

### Entry Point and Core

- **`figwheel.main`** (`src/figwheel/main.cljc`, ~2600 lines) — The main namespace. Handles CLI parsing, build orchestration, REPL setup, file watching, and hot reloading coordination. This is a `.cljc` file with both Clojure (tooling) and ClojureScript (client-side reload) code.
- **`figwheel.main.api`** (`src/figwheel/main/api.clj`) — Public programmatic API (`start`, `start-join`, `stop`, `cljs-repl`, `repl-env`). Thin wrapper over `figwheel.main`.

### Configuration & Validation

- **`figwheel.main.schema.config`** — Spec definitions for figwheel-main.edn options (`:watch-dirs`, `:css-dirs`, `:ring-handler`, etc.)
- **`figwheel.main.schema.cljs_options`** — Spec definitions for .cljs.edn compiler options
- **`figwheel.main.schema.cli`** — CLI argument parsing and validation
- Uses `expound` for human-readable spec errors and `spell-spec` for typo detection in config keys

### Supporting Modules

- **`figwheel.main.helper`** — Ring web app served as the default landing page when no index.html exists. Pre-compiled to `helper-resources/` with advanced optimizations.
- **`figwheel.main.css_reload`** — CSS live reload (client-side, .cljc)
- **`figwheel.main.testing`** — ClojureScript test runner integration
- **`figwheel.main.npm`** — NPM/webpack bundling support
- **`figwheel.main.react_native`** — React Native development support
- **`figwheel.main.logging`** — Custom Java logging with ANSI formatting
- **`figwheel.main.watching`** — File system watching via beholder

### Configuration Files

Build configs are EDN files in the project root:
- `figwheel-main.edn` — Global figwheel settings
- `*.cljs.edn` — Per-build ClojureScript compiler options (e.g., `dev.cljs.edn`)
- `dev.cljs.edn` points at `devel/` which contains an example project for testing

### Key Dependencies

- `figwheel-core` — Core hot-reload engine (sibling repo)
- `figwheel-repl` — REPL implementation with websocket/long-polling transport (sibling repo)
- `ring` — HTTP server
- `beholder` — File watching
- `certifiable` — HTTPS certificate generation

## Development Notes

- `.cljc` files contain reader conditionals for both Clojure (server/tooling) and ClojureScript (client/browser) code
- The `devel/` directory contains example ClojureScript files used for local testing
- `helper-resources/` contains pre-compiled static assets for the helper app — rebuild with `clojure -A:build-helper` after changing `figwheel.main.helper`
- The `docs/` directory is a Jekyll site; `docs/docs/*.md` are the documentation pages
- `pom.xml` is auto-generated from `project.clj` — do not edit directly
- Integration tests live in a separate repo: `../figwheel-main-testing/` (run with `make itest`)
- Test projects in `../temp/` (e.g., `hello-world.core`, `testmain`) use `:local/root` deps to point at the local figwheel repos — use these to manually test builds and REPL workflows against local changes
