(ns user
  (:require [figwheel.server.ring]
            [figwheel.main.schema.config]
            [figwheel.main.api :as api]
            [clojure.string :as string]))

(defn strip-leading-whitspace [s]
  (string/join "\n"
   (map
    #(string/replace % #"^\s\s(.*)" "$1")
    (string/split-lines s))))

(def doc-string #(-> % resolve meta :doc strip-leading-whitspace))
(def args #(-> % resolve meta :arglists))

(defn sym->markdown-doc [sym]
  (str
   "## `" sym "`\n\n"
   "Args: `"(pr-str (args sym)) "`\n\n"
   (doc-string sym) "\n\n"))

(defn api-docs []
  (let [syms ['figwheel.main.api/start
              'figwheel.main.api/cljs-repl
              'figwheel.main.api/repl-env              
              'figwheel.main.api/stop
              'figwheel.main.api/stop-all              
              'figwheel.main.api/start-join]]
    (string/join "\n" (map sym->markdown-doc syms))))

(defn build-api-docs []
  (spit "docs/_includes/main-api-docs.md" (api-docs)))

(defn build-option-docs []
  (doseq [dir ["doc/figwheel-main-options.md" "docs/config-options.md"]]
    (figwheel.main.schema.core/output-docs dir)))

(defn build-docs []
  (build-api-docs)
  (build-option-docs))

