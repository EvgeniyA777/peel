(ns peel.text
  (:require [clojure.string :as str])
  (:import [org.jsoup.nodes Element TextNode]))

(defn normalize
  "Text normalization shared across all platform extractors.
  Strips emoji and non-text symbols, collapses runs of spaces,
  caps consecutive newlines at two, and trims."
  [s]
  (-> s
      (str/replace #"[^\p{L}\p{N}\p{P}\p{Sm}\p{Sc}\p{Z}\n]" "") ; strip emoji, keep +=/$
      (str/replace #" +" " ")
      (str/replace #"\n{3,}" "\n\n")
      str/trim))

(defn- lang-from-classes
  "Returns language string from a set of CSS class names, or nil.
  Handles both language-* and lang-* prefixes."
  [classes]
  (some (fn [cls]
          (cond
            (str/starts-with? cls "language-") (subs cls 9)
            (str/starts-with? cls "lang-")     (subs cls 5)))
        classes))

(defn- code-lang
  "Extracts language from a <pre><code> pair. Checks the <code> element first,
  then falls back to the <pre> element's classes."
  [pre-el code-el]
  (or (when code-el (lang-from-classes (.classNames code-el)))
      (lang-from-classes (.classNames pre-el))))

(defn element->md
  "Extracts text from a Jsoup element, rendering <pre><code> blocks as Markdown
  fenced blocks. Non-code text is normalized. Works on a clone; does not mutate el."
  [^Element el]
  (let [clone  (.clone el)
        fences (atom [])]
    (doseq [btn (.select clone "button")]
      (.remove btn))
    (doseq [[i pre] (map-indexed vector (.select clone "pre"))]
      (let [code  (.selectFirst pre "code")
            lang  (code-lang pre code)
            raw   (if code (.wholeText code) (.wholeText pre))
            fence (str "```" (or lang "") "\n" raw "\n```")]
        (swap! fences conj fence)
        (.replaceWith pre (TextNode. (str "PEELCODE" i "END")))))
    (let [normalized (normalize (.text clone))
          restored   (reduce-kv (fn [s i fence]
                                  (str/replace s (str "PEELCODE" i "END")
                                               (str "\n\n" fence "\n\n")))
                                normalized
                                (vec @fences))]
      (-> restored
          (str/replace #"\n{3,}" "\n\n")
          str/trim))))
