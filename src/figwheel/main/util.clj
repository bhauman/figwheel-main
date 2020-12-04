(ns figwheel.main.util
  (:require
   [clojure.string :as string]
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [cljs.util]
   [figwheel.repl]
   [cljs.build.api :as bapi])
  (:import
   java.nio.file.Paths))

(def ^:dynamic *compile-collector* nil)

(defn command-exists? [& paths]
  (boolean
   (try
     (= 0 (:exit (apply sh (map str paths))))
     (catch Throwable e))))

(defn os? []
  (let [os-name
        (-> (System/getProperty "os.name" "generic")
            (.toLowerCase java.util.Locale/ENGLISH))
        has? #(>= (.indexOf %1 %2) 0)]
    (cond
      (or (has? os-name "mac")
          (has? os-name "darwin")) :macos
      (has? os-name "win") :windows
      (has? os-name "nux") :linux
      :else :other)))

(def system-os (os?))

;; this is a function so that we can actually make an effort to find the
;; executable
(def npx-executable
  (memoize
   (fn []
     (if (= :windows system-os)
       (if (command-exists? "npx" "-h") "npx" "npx.cmd")
       "npx"))))

(defn delete-file-or-directory [f]
  (let [f (io/file f)]
    (when (.exists f)
      (if (.isDirectory f)
        (doseq [sf (reverse (file-seq f))]
          (.delete sf))
        (.delete f)))))

(defn ->path [s & args]
  (java.nio.file.Paths/get ^String s (into-array String args)))

(defn path-parts [& args]
  (mapv str (apply ->path args)))

(defn relativized-path-parts [path]
  (let [local-dir-parts (path-parts (System/getProperty "user.dir"))
        parts (path-parts (.getCanonicalPath (io/file path)))]
    [local-dir-parts parts]
    (when (= local-dir-parts (take (count local-dir-parts) parts))
      (drop (count local-dir-parts) parts))))

#_(relativized-path-parts (.getCanonicalPath (io/file "src/figwheel/main.clj")))

(defn require? [symbol]
  (try
    (require symbol)
    true
    (catch Exception e
      #_(println (.getMessage e))
      #_(.printStackTrace e)
      false)))

(defn require-resolve-var [handler]
  (when handler
    (let [h (symbol handler)]
      (or (try (resolve h) (catch Throwable t nil))
          (when-let [ns (namespace h)]
            (when (require? (symbol ns))
              (when-let [handler-var (resolve h)]
                handler-var)))))))

(defn require-resolve-handler [handler]
  (when handler
    (if (fn? handler)
      handler
      (require-resolve-var handler))))

(let [symbol->parts (comp (partial map symbol) (juxt namespace name) symbol)
      require-it    #(do (require (first %)) %)
      resolve-it    #(resolve (apply symbol (map str %)))
      fn->error     {symbol->parts :bad-namespaced-symbol
                     require-it    :unable-to-load-handler-namespace
                     resolve-it    :unable-to-resolve-handler-fn}]
  (defn require-resolve-var-or-error [handler]
    (when handler
      (reduce
       (fn [v f]
         (try (f v)
              (catch Throwable t
                (reduced {:stage (fn->error f)
                          :value v
                          :exception t}))))
       handler
       [symbol->parts require-it resolve-it]))))

(defn require-resolve-handler-or-error [handler]
  (when handler
    (if (fn? handler)
      handler
      (require-resolve-var-or-error handler))))

(defn rebel-readline? []
  (require-resolve-var 'rebel-readline.core/read-line))

(defn static-classpath []
  (mapv
   #(.getCanonicalPath (io/file %))
   (string/split (System/getProperty "java.class.path")
                 (java.util.regex.Pattern/compile (System/getProperty "path.separator")))))

(defn dynamic-classpath []
  (mapv
   #(.getCanonicalPath (io/file (.getFile %)))
   (mapcat
    #(try (.getURLs %)
          (catch Throwable t
            nil))
    (take-while some? (iterate #(.getParent %) (.getContextClassLoader (Thread/currentThread)))))))

#_((set (dynamic-classpath)) (.getCanonicalPath (io/file "src")))
#_(add-classpath! (.toURL (io/file "src")))

(defn dir-on-classpath? [dir]
  (let [dir-canonical-path (.getCanonicalPath (io/file dir))]
    (some #(.startsWith dir-canonical-path %) (static-classpath))))

(defn dir-on-current-classpath? [dir]
  (let [dir-canonical-path (.getCanonicalPath (io/file dir))]
    (some #(.startsWith dir-canonical-path %)
          (distinct
            (concat
              (static-classpath)
              (dynamic-classpath))))))

(defn root-dynclass-loader []
  (last
   (take-while
    #(instance? clojure.lang.DynamicClassLoader %)
    (iterate #(.getParent ^java.lang.ClassLoader %) (.getContextClassLoader (Thread/currentThread))))))

(defn ensure-dynclass-loader! []
  (let [cl (.getContextClassLoader (Thread/currentThread))]
    (when-not (instance? clojure.lang.DynamicClassLoader cl)
      (.setContextClassLoader (Thread/currentThread) (clojure.lang.DynamicClassLoader. cl)))))

(defn add-classpath! [url]
  (assert (instance? java.net.URL url))
  (ensure-dynclass-loader!)
  (let [root-loader (root-dynclass-loader)]
    (.addURL ^clojure.lang.DynamicClassLoader root-loader url)))

;; this is a best guess for situations where the user doesn't
;; add the source directory to the classpath
(defn valid-source-path? [source-path]
  ;; TODO shouldn't contain preconfigured target directory
  (let [compiled-js (string/replace source-path #"\.clj[sc]$" ".js")]
    (and (not (.isFile (io/file compiled-js)))
         (not (string/starts-with? source-path "./out"))
         (not (string/starts-with? source-path "./target"))
         (not (string/starts-with? source-path "./resources"))
         (not (string/starts-with? source-path "./dev-resources"))
         (let [parts (path-parts source-path)
               fpart (second parts)]
           (and (not (#{"out" "resources" "target" "dev-resources"} fpart))
                (empty? (filter #{"public"} parts)))))))

(defn find-ns-source-in-local-dir [ns]
  (let [cljs-path (cljs.util/ns->relpath ns :cljs)
        cljc-path (cljs.util/ns->relpath ns :cljc)
        sep (System/getProperty "file.separator")]
    (->> (file-seq (io/file "."))
         (map str)
         (filter
          #(or (string/ends-with? % (str sep cljs-path))
               (string/ends-with? % (str sep cljc-path))))
         (filter valid-source-path?)
         (sort-by count)
         first)))

;; only called when ns isn't on classpath
(defn find-source-dir-for-cljs-ns [ns]
  (let [cljs-path (cljs.util/ns->relpath ns :cljs)
        cljc-path (cljs.util/ns->relpath ns :cljc)]
    (when-let [candidate (find-ns-source-in-local-dir ns)]
      (let [rel-source-path (if (string/ends-with? candidate "s")
                              cljs-path
                              cljc-path)
            candidate'
            (when candidate
              (let [path (string/replace candidate rel-source-path "")]
                (-> path
                    (subs 0 (dec (count path)))
                    (subs 2))))]
        (when (.isFile (io/file candidate' rel-source-path))
          candidate')))))

#_(find-source-dir-for-cljs-ns 'expro)

#_(find-source-dir-for-cljs-ns 'exproj.core)

(defn ns->location [ns]
  (try (bapi/ns->location ns)
       (catch java.lang.IllegalArgumentException e
         (throw (ex-info
                 (str "ClojureScript Namespace " ns " was not found on the classpath.")
                 {:figwheel.main/error true})))))

(defn safe-ns->location [ns]
  (try (bapi/ns->location ns)
       (catch Throwable t
         nil)))

(defn ns-available? [ns]
  (or (safe-ns->location ns)
      (find-ns-source-in-local-dir ns)))

(defn source-file-types-in-dir [dir]
  (into
   #{}
   (map
    #(last (string/split % #"\."))
    (keep
     #(last (path-parts (str %)))
     (filter
      #(.isFile %)
      (file-seq (io/file dir)))))))

(let [file-mod-atom (atom {})]
  (defn file-has-changed? [f scope]
    (let [f ^java.io.File (io/file f)
          canonical-path (str scope ":" (.getCanonicalPath f))]
      (when (.exists f)
        (let [chk-sum (.hashCode (slurp f))
              changed? (not= chk-sum (get @file-mod-atom canonical-path))]
          (swap! file-mod-atom assoc canonical-path chk-sum)
          changed?)))))

(let [localhost (promise)]
  ;; this call takes a very long time to complete so lets get in in parallel
  (doto (Thread. #(deliver localhost (try (java.net.InetAddress/getLocalHost)
                                          (catch Throwable e
                                            nil))))
    (.setDaemon true)
    (.start))
  (defn fill-connect-url-template [url host server-port]
    (cond-> url
      (.contains url "[[config-hostname]]")
      (string/replace "[[config-hostname]]" (or host "localhost"))

      (.contains url "[[server-hostname]]")
      (string/replace "[[server-hostname]]" (or (some-> @localhost
                                                        .getHostName)
                                                "localhost"))

      (.contains url "[[server-ip]]")
      (string/replace "[[server-ip]]"       (or (some-> @localhost
                                                        .getHostAddress)
                                                "127.0.0.1"))

      (.contains url "[[server-port]]")
      (string/replace "[[server-port]]"     (str server-port)))))

(defn setup-connect-url [{:keys [:figwheel.main/config repl-env-options] :as cfg}]
  (let [port (get-in config [:ring-server-options :port] figwheel.repl/default-port)
        host (get-in config [:ring-server-options :host] "localhost")]
    (fill-connect-url-template
     (:connect-url config "ws://[[config-hostname]]:[[server-port]]/figwheel-connect")
     host
     port)))

(defn dot-slash [dir]
  (let [path (.toPath (io/file dir))]
    (if (and (not (.getRoot path))
             (not= "." (str (first (iterator-seq (.iterator path))))))
      (io/file "." dir)
      (io/file dir))))
