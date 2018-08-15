(ns figwheel-main-docs.main
  (:require [goog.dom :as dom]
            [goog.dom.classlist :as cl]
            [goog.string :as gstring]
            [goog.fx.css3 :as fxcss3]
            [goog.style :as style]))

(defn itemable->coll [itemable]
  (map #(.item itemable %) (range (.-length itemable))))

(defn log [x]
  (js/console.log x)
  x)

(defn slide-in! [el]
  (let [height (.-height (style/getSize el))]
    (style/setHeight el 0)
    (js/setTimeout (fn [e] (style/setHeight el height)) 0)))

(defn get-main []
  (.item (dom/getElementsByClass "main") 0))

(defn get-container []
  (.item (dom/getElementsByClass "container") 0))

(defn get-main-headers []
  (->> (itemable->coll (.-children (get-main)))
       (filter #(.startsWith (.-tagName %) "H"))
       (filter #(.-id %))
       (rest)))

(defn anchored-header! [header]
  (let [header-content (dom/getTextContent header)
        id (.-id header)
        anchor (dom/createDom "A" #js {:href (str "#" id) :class "page-anchor"} header-content)]
    ;; make sure we haven't already anchored this header
    (when (zero? (.-length (dom/getChildren header)))
      (dom/removeChildren header)
      (dom/append header anchor))))

(defn extract-heading [el]
  {:ref (.-id el)
   :content (dom/getTextContent el)
   :tag (.-tagName el)
   :type :link
   :el el})

(defn create-toc-link [{:keys [ref content]}]
  (dom/createDom "A" #js {:href (str "#" ref) :class "toc-link"} (dom/createTextNode content)))

(defmulti format-item :type)

(defmethod format-item :link [item]
  (create-toc-link item))

(defmethod format-item :category [{:keys [ref content]}]
  (dom/createDom "DIV" #js {:href (str "#" ref) :class "toc-category"} (dom/createTextNode content)))

(defn create-toc [links]
  (apply dom/createDom "div" #js {:id "toc" :class "toc slide-in"} links))

(defn get-current-nav-link []
  (let [path js/window.location.pathname]
    (some->> (.item (dom/getElementsByTagName "nav") 0)
             (.-children)
             (itemable->coll)
             (filter #(= (.-tagName %) "A"))
             (filter #(gstring/endsWith (.-href %) path))
             first)))

(defn nav-displayed? []
  ;; 650 aligns with the CSS value for the
  ;; nav element
  (> (.-width (dom/getViewportSize)) 650))

(defn ^:export anchored-headers []
  (doseq [header (get-main-headers)]
    (anchored-header! header)))

(defn ^:export insert-doc-toc []
  (when (nav-displayed?)
    (let [cur-nav (get-current-nav-link)
          toc-el (create-toc (->> (get-main-headers)
                                  (map extract-heading)
                                  (map format-item)))]
      (cl/add cur-nav "focused-nav-link")
      (dom/insertSiblingAfter toc-el cur-nav)
      (slide-in! toc-el)))
  (anchored-headers))

(defn ^:export insert-options-toc []
  (when (nav-displayed?)
    (when-let [nav (.item (dom/getElementsByTagName "nav") 0)]
      (dom/removeChildren nav)
      (let [toc-el (apply dom/createDom "DIV" #js {:style "opacity: 0;"}
                          (->> (get-main-headers)
                               (map extract-heading)
                               (map #(cond-> %
                                       (= (:tag %) "H1") (assoc :type :category)))
                               (map format-item)))]
        (dom/append nav toc-el)
        (.play (fxcss3/fadeIn toc-el 0.3)))))
  (anchored-headers))

#_(insert-options-toc)

#_(log    (create-toc (->> (get-main-headers)
                    (map extract-heading)

                    (map create-toc-link))))

