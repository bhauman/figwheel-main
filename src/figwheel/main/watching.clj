(ns figwheel.main.watching
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [nextjournal.beholder :as beholder]))

(def ^:dynamic *watcher* (atom {:watcher nil :watches {}}))

(defn stop-watchers! [watchers]
  (doseq [watcher watchers]
    (beholder/stop watcher)))

(defn alter-watches [{:keys [watchers watches]} f]
  (stop-watchers! watchers)
  (let [watches (f watches)
        watchers (doall
                   (for [watch (vals watches)]
                     (let [{:keys [paths filter handler]} watch
                           ctx (atom {})]
                       (apply beholder/watch
                              (fn [e]
                                (let [file (.toFile (:path e))
                                      e (assoc e :file file)]
                                  (when (or (not filter)
                                            (filter ctx e))
                                    (swap! ctx handler e))))
                              paths))))]
    {:watchers watchers
     :watches  watches}))

(defn add-watch! [watch-key watch]
  (swap! *watcher* alter-watches #(assoc % watch-key watch)))

(defn remove-watch! [watch-key]
  (swap! *watcher* alter-watches #(dissoc % watch-key)))

(defn stop! []
  (stop-watchers! (:watchers @*watcher*)))

(defn reset-watch! []
  (stop!)
  (reset! *watcher* {}))

(defn running? []
  (some-> *watcher* deref :watcher :thread .isAlive))

(defn join []
  (some-> *watcher* deref :watcher :thread .join))

(defn throttle [millis f]
  (fn [{:keys [collector] :as ctx} e]
    (let [collector (or collector (atom {}))
          {:keys [collecting? events]} (deref collector)]
      (if collecting?
        (swap! collector update :events (fnil conj []) e)
        (let [events (volatile! nil)]
          (swap! collector assoc :collecting? true)
          (future
            (try
              (Thread/sleep millis) ;; is this needed now?
              (swap! collector update :events (fn [evts] (vreset! events evts) nil))
              (f (cons e @events))
              (finally
                (swap! collector assoc :collecting? false))))))
      (assoc ctx :collector collector))))

(defn file-suffix [file]
  (last (string/split (.getName (io/file file)) #"\.")))

(defn real-file? [file]
  (and file
       (.isFile file)
       (not (.isHidden file))
       (not (#{\. \#} (first (.getName file))))))

(defn suffix-filter [suffixes]
  (fn [_ {:keys [file]}]
    (and (real-file? file)
         (suffixes (file-suffix file)))))
