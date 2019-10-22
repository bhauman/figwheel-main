(ns figwheel.main.css-reload
  (:require
   [clojure.string :as string]
   #?@(:cljs [[goog.Uri :as guri]
              [goog.log :as glog]
              [goog.object :as gobj]
              [goog.cssom :as gcss]
              [goog.events :as gevent]]
       :clj [[clojure.data.json :as json]
             [cljs.env]
             [cljs.repl]
             [clojure.java.io :as io]
             [figwheel.main.watching :as fww]
             [figwheel.main.util :as fw-util]]))
  (:import #?@(:cljs [[goog Promise]
                      goog.debug.Console])))

#?(:cljs

   (do

;; --------------------------------------------------
;; Logging
;; --------------------------------------------------
;;
;; Levels
;; goog.debug.Logger.Level.(SEVERE WARNING INFO CONFIG FINE FINER FINEST)
;;
;; set level (.setLevel logger goog.debug.Logger.Level.INFO)
;; disable   (.setCapturing log-console false)

     (defonce logger (glog/getLogger "Figwheel CSS Reload"))

     (defn ^:export console-logging []
       (when-not (gobj/get goog.debug.Console "instance")
         (let [c (goog.debug.Console.)]
      ;; don't display time
           (doto (.getFormatter c)
             (gobj/set "showAbsoluteTime" false)
             (gobj/set "showRelativeTime" false))
           (gobj/set goog.debug.Console "instance" c)
           c))
       (when-let [console-instance (gobj/get goog.debug.Console "instance")]
         (.setCapturing console-instance true)
         true))

     (defonce log-console (console-logging))

     (defn add-cache-buster [url]
       (.makeUnique (guri/parse url)))

     (defn truncate-url [url]
       (-> (first (string/split url #"\?"))
           (string/replace-first (str (.-protocol js/location) "//") "")
           (string/replace-first ".*://" "")
           (string/replace-first #"^//" "")
           (string/replace-first #"[^\/]*" "")))

     (defn matches-file?
       [file stylesheet]
       (when-let [href (.-href stylesheet)]
         (let [match (string/join "/"
                                  (take-while identity
                                              (map #(if (= %1 %2) %1 false)
                                                   (reverse (string/split file "/"))
                                                   (reverse (string/split (truncate-url href) "/")))))
               match-length (count match)
               file-name-length (count (last (string/split file "/")))]
           (when (>= match-length file-name-length) ;; has to match more than the file name length
             {:stylesheet stylesheet
              :link-href href
              :match-length match-length
              :current-url-length (count (truncate-url href))}))))

     (defn root-stylesheet [stylesheet]
       (if-let [parent-stylesheet (.-parentStyleSheet stylesheet)]
         (recur parent-stylesheet)
         stylesheet))

     (defn get-correct-link [file]
       (when-let [res (first
                       (sort-by
                        (fn [{:keys [match-length current-url-length]}]
                          (- current-url-length match-length))
                        (keep #(matches-file? file %)
                              (gcss/getAllCssStyleSheets))))]
         (-> res :stylesheet root-stylesheet .-ownerNode)))

     (defn clone-link [link url]
       (let [clone (.createElement js/document "link")]
         (set! (.-rel clone)      "stylesheet")
         (set! (.-media clone)    (.-media link))
         (set! (.-disabled clone) (.-disabled link))
         (set! (.-href clone)     (add-cache-buster url))
         clone))

     (defn add-link-to-document [orig-link klone finished-fn]
       (let [parent (.-parentNode orig-link)]
         ;; prevent css removal flash
         (gevent/listenOnce klone
                            "load"
                            (fn []
                              (.removeChild parent orig-link)
                              (finished-fn)))
         (if (= orig-link (.-lastChild parent))
           (.appendChild parent klone)
           (.insertBefore parent klone (.-nextSibling orig-link)))))

     (defonce reload-css-deferred-chain (atom (Promise. #(%1 []))))

     (defn reload-css-file [file fin]
       (if-let [link (get-correct-link file)]
         (add-link-to-document link (clone-link link (.-href link))
                               #(fin file))
         (fin nil)))

     (defn conj-reload-prom [prom file]
       (.then prom
              (fn [files]
                (Promise. (fn [succ fail]
                            (reload-css-file file
                                             (fn [f]
                                               (succ (if f
                                                       (conj files f)
                                                       files)))))))))

     (defn dispatch-on-css-load [files]
       (.dispatchEvent
        js/document.body
        (doto (js/Event. "figwheel.after-css-load" js/document.body)
          (gobj/add "data" {:css-files files}))))

     (defn reload-css-files* [files on-cssload]
       (doseq [file files]
         (swap! reload-css-deferred-chain conj-reload-prom file))
       (swap! reload-css-deferred-chain
              (fn [prom]
                (.then prom
                       (fn [loaded-files]
                         (when (not-empty loaded-files)
                           (glog/info logger (str "loaded " (pr-str loaded-files)))
                           (dispatch-on-css-load loaded-files))
                         (when-let [not-loaded (not-empty (remove (set loaded-files) (set files)))]
                           (glog/warning logger (str "Unable to reload " (pr-str not-loaded))))
                         ;; reset
                         [])))))

     (defn reload-css-files [{:keys [on-cssload]} files]
       (when (not (nil? goog/global.document))
         (when-let [files' (not-empty (distinct files))]
           (reload-css-files* files' on-cssload))))

     ;;takes an array of css files, relativized with forward slash path-separators
     (defn ^:export reload-css-files-remote [files-array]
       (reload-css-files {} files-array)
       true))

   :clj
   (do

     (defn client-eval [code]
       (when-not (string/blank? code)
         (cljs.repl/-evaluate
          cljs.repl/*repl-env*
          "<cljs repl>" 1
          code)))

     (defn reload-css-files [files]
       (when-not (empty? files)
         (client-eval
          (format "figwheel.main.css_reload.reload_css_files_remote(%s);"
                  (json/write-str files)))))

     (defn prep-css-file-path [file]
       (-> file
           .getCanonicalPath
           (string/replace java.io.File/separator "/")))

;; repl-env needs to be bound
     (defn start* [paths & [reload-config]]
       (binding
         [fww/*hawk-options* (:hawk-options reload-config nil)]
         (fww/add-watch!
           [::watcher paths]
           {:paths paths
            :filter (fww/suffix-filter #{"css"})
            :handler (fww/throttle
                       50
                       (bound-fn [evts]
                         (when-let [files (not-empty (mapv (comp prep-css-file-path :file) evts))]
                           (reload-css-files files))))})))

     (defn stop* [paths]
       (let [remove-watch! (resolve 'figwheel.main/remove-watch!)]
         (remove-watch! [::watcher paths])))

     (defmacro start [paths] (start* paths) nil)

     (defmacro stop [paths] (stop* paths) nil)))
