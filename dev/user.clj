(ns user
  (:require [figwheel.server.ring]
            [figwheel.main.schema.config]))

(defn build-option-docs []
  (doseq [dir ["doc/figwheel-main-options.md" "docs/config-options.md"]]
    (figwheel.main.schema.core/output-docs dir)))
