;; ------------------------------------------------------------------------
;; Code taken directly from Krell https://github.com/vouch-opensource/krell
;; ------------------------------------------------------------------------

(ns figwheel.main.krell.assets
  (:require [clojure.string :as string]
            [figwheel.main.krell.util :as util]))

(defn js? [f]
  (#{".js"} (util/file-ext f)))

(defn clojure? [f]
  (#{".clj" ".cljc" ".cljs"} (util/file-ext f)))

(defn asset-require [path]
  (str "\"" path "\": require('" path "')" ))

(defn assets-js [assets]
  (str
    "module.exports = {\n"
    "  assets: {\n"
    (string/join ",\n" (map (comp #(str "    " %) asset-require) assets))
    "  }\n"
    "};\n"))
