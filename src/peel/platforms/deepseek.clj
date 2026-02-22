(ns peel.platforms.deepseek
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [peel.text :as text]))

;; "d162f7b9" is the hashed class for DeepSeek's "Read N web page(s)" search badge.
;; Like all hashed classes, it may change with frontend updates.
(def ^:private search-badge-class "d162f7b9")

;; DeepSeek renders citation superscripts as <span class="ds-markdown-cite"> containing
;; two inner spans: an invisible "-" spacer (opacity:0) and an absolutely-positioned digit.
;; jsoup's .text() concatenates both, producing artifacts like "-1", "-4", etc.
;; We remove the whole <a> wrapper (the citation link) from the DOM before extracting text.
(def ^:private cite-selector "a:has(span.ds-markdown-cite)")

(defn- detect-user-class
  "Infers the hashed CSS class that marks user bubbles by finding
  classes present on the first message but absent from all messages
  (i.e. not shared by assistant messages).
  Falls back to nil if detection is ambiguous."
  [elements]
  (when (seq elements)
    (let [all-classes (map #(set (.. % classNames)) elements)
          common      (reduce set/intersection all-classes)
          unique      (set/difference (first all-classes) common)]
      (when (= 1 (count unique))
        (first unique)))))

(defn- clean [s]
  (-> s
      (str/replace "\u2028" "\n")                                 ; line separator → newline
      (str/replace "\u2029" "\n")                                 ; paragraph separator → newline
      (str/replace #"[\uD800-\uDFFF]" "")                        ; remove surrogate pairs (emoji outside BMP)
      text/normalize))

(defn extract [^org.jsoup.nodes.Document doc]
  (let [elements  (seq (.select doc ".ds-message"))
        user-cls  (detect-user-class elements)]
    (when-not user-cls
      (binding [*out* *err*]
        (println "peel/deepseek: could not detect user bubble class, falling back to hardcoded value")))
    (let [user-cls (or user-cls "d29f3d7d")]
      (->> elements
           (map (fn [el]
                  (doseq [badge (.select el (str "." search-badge-class))]
                    (.remove badge))
                  (doseq [cite (.select el cite-selector)]
                    (.remove cite))
                  (doseq [banner (.select el "div.md-code-block-banner")]
                    (.remove banner))
                  {:role (if (.hasClass el user-cls) :user :assistant)
                   :text (clean (.text el))}))
           (remove (comp str/blank? :text))
           vec))))
