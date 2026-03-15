(ns peel.text
  (:require [clojure.string :as str])
  (:import [org.jsoup Jsoup]
           [org.jsoup.nodes Element TextNode Document]))

(def ^:private mojibake-replacements
  [["â€”" "—"]
   ["â€“" "–"]
   ["â€¦" "…"]
   ["â€¢" "•"]
   ["â€˜" "‘"]
   ["â€™" "’"]
   ["â€œ" "“"]
   ["â€\u009d" "”"]
   ["â†’" "→"]
   ["Â\u00A0" " "]])

(defn- repair-mojibake
  "Repairs common UTF-8/CP1252 mojibake sequences seen in saved chat HTML.
  Keeps the fix table deliberately small to avoid mutating valid text."
  [s]
  (if (and s (re-find #"[Ââ]" s))
    (reduce (fn [acc [bad good]]
              (str/replace acc bad good))
            s
            mojibake-replacements)
    s))

(defn normalize
  "Text normalization shared across all platform extractors.
  Strips emoji and non-text symbols, collapses runs of spaces,
  caps consecutive newlines at two, and trims."
  [s]
  (-> s
      repair-mojibake
      (str/replace #"[^\p{L}\p{N}\p{P}\p{Sm}\p{Sc}\p{Z}\n]" "") ; strip emoji, keep +=/$
      (str/replace #" +" " ")
      (str/replace #"\n{3,}" "\n\n")
      str/trim))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

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

(defn- fence-ticks
  "Returns a backtick fence string long enough to safely wrap raw code.
  Uses at least 3 backticks; uses one more than the longest run in raw."
  [raw]
  (let [max-run (->> (re-seq #"`+" raw) (map count) (apply max 0))]
    (apply str (repeat (max 3 (inc max-run)) \`))))

(defn- block-tag?
  "Returns true if the tag name represents a block-level HTML element."
  [tag]
  (contains? #{"p" "div" "h1" "h2" "h3" "h4" "h5" "h6"
               "ul" "ol" "li" "blockquote" "pre" "hr"
               "table" "thead" "tbody" "tr" "th" "td"
               "dl" "dt" "dd" "figure" "figcaption" "section" "article"}
             tag))

;; ---------------------------------------------------------------------------
;; DOM → Markdown walker
;; ---------------------------------------------------------------------------

(declare walk)

(defn- walk-children
  "Walks all child nodes of an element and concatenates results."
  [^Element el]
  (->> (.childNodes el)
       (map walk)
       str/join))

(defn- heading-level [tag]
  (case tag
    "h1" 1 "h2" 2 "h3" 3 "h4" 4 "h5" 5 "h6" 6 nil))

(defn- query-text-line?
  [^Element el]
  (contains? (.classNames el) "query-text-line"))

(defn- markdown-table-line?
  [s]
  (boolean (re-matches #"\s*\|.+\|\s*" (or s ""))))

(defn- indent-block
  "Prepends prefix to every line of text."
  [prefix text]
  (->> (str/split-lines text)
       (map #(str prefix %))
       (str/join "\n")))

(defn- direct-children-by-tag
  [^Element el tags]
  (->> (.children el)
       (filter (fn [child]
                 (contains? tags (str/lower-case (.tagName ^Element child)))))))

(defn- walk-li
  "Renders a single <li> including nested lists."
  [^Element li bullet]
  (let [;; Separate nested lists from inline content
        nested-lists (.select li "> ul, > ol")
        ;; Remove nested lists temporarily to get inline content
        li-clone (.clone li)]
    (doseq [nl (.select li-clone "> ul, > ol")]
      (.remove nl))
    (let [inline (str/trim (walk-children li-clone))
          nested (when (seq nested-lists)
                   (->> nested-lists
                        (map (fn [nl] (walk nl)))
                        (str/join "\n")))]
      (str bullet inline
           (when nested
             (str "\n" (indent-block "  " nested)))))))

(defn- walk-list
  "Renders <ul> or <ol> list."
  [^Element el ordered?]
  (let [items (.select el "> li")
        rendered (->> items
                      (map-indexed
                       (fn [i li]
                         (let [bullet (if ordered?
                                        (str (inc i) ". ")
                                        "- ")]
                           (walk-li li bullet))))
                      (str/join "\n"))]
    (str "\n\n" rendered "\n\n")))

(defn- table-cell->md
  "Renders a table cell as a single-line GFM-safe string."
  [^Element cell]
  (-> (walk-children cell)
      str/trim
      (str/replace #"\n{2,}" "<br><br>")
      (str/replace #"\n" "<br>")
      (str/replace #" {2,}" " ")
      (str/replace "|" "\\|")))

(defn- table-row->md
  [^Element row]
  (let [cells (direct-children-by-tag row #{"th" "td"})]
    (when (seq cells)
      (str "| "
           (str/join " | " (map table-cell->md cells))
           " |"))))

(defn- table-rows
  "Returns direct table rows in document order, keeping thead rows first."
  [^Element table]
  (let [direct-trs (direct-children-by-tag table #{"tr"})
        section-rows (->> (direct-children-by-tag table #{"thead" "tbody" "tfoot"})
                          (mapcat #(direct-children-by-tag % #{"tr"})))]
    (concat direct-trs section-rows)))

(defn- walk-element
  "Converts a single Jsoup Element to Markdown."
  [^Element el]
  (let [tag (str/lower-case (.tagName el))]
    (cond
      ;; Code blocks
      (= tag "pre")
      (let [code (.selectFirst el "code")
            lang (code-lang el code)
            raw  (if code (.wholeText code) (.wholeText el))
            ticks (fence-ticks raw)]
        (str "\n\n" ticks (or lang "") "\n" raw "\n" ticks "\n\n"))

      ;; Inline code
      (= tag "code")
      (let [text (.wholeText el)]
        (if (str/includes? text "`")
          (str "`` " text " ``")
          (str "`" text "`")))

      ;; Headings
      (heading-level tag)
      (let [level (heading-level tag)
            prefix (apply str (repeat level "#"))]
        (str "\n\n" prefix " " (str/trim (walk-children el)) "\n\n"))

      ;; Paragraphs
      (= tag "p")
      (let [inner (str/trim (walk-children el))]
        (cond
          (str/blank? inner) "\n"
          (query-text-line? el) (str inner "\n")
          :else (str "\n\n" inner "\n\n")))

      ;; Line breaks
      (= tag "br")
      "\n"

      ;; Horizontal rule
      (= tag "hr")
      "\n\n---\n\n"

      ;; Lists
      (= tag "ul") (walk-list el false)
      (= tag "ol") (walk-list el true)
      ;; li handled by walk-list, but if encountered standalone:
      (= tag "li") (str (walk-children el) "\n")

      ;; Blockquote
      (= tag "blockquote")
      (let [inner (str/trim (walk-children el))]
        (str "\n\n" (indent-block "> " inner) "\n\n"))

      ;; Bold / strong
      (or (= tag "strong") (= tag "b"))
      (let [inner (walk-children el)]
        (if (str/blank? inner) "" (str "**" (str/trim inner) "**")))

      ;; Italic / emphasis
      (or (= tag "em") (= tag "i"))
      (let [inner (walk-children el)]
        (if (str/blank? inner) "" (str "*" (str/trim inner) "*")))

      ;; Strikethrough
      (or (= tag "del") (= tag "s"))
      (let [inner (walk-children el)]
        (if (str/blank? inner) "" (str "~~" (str/trim inner) "~~")))

      ;; Links
      (= tag "a")
      (let [inner (str/trim (walk-children el))
            href  (.attr el "href")]
        (if (or (str/blank? href) (= href "#"))
          inner
          (str "[" inner "](" href ")")))

      ;; Table support
      (= tag "table")
      (let [rows (table-rows el)
            header-row (first rows)
            header (when header-row (table-row->md header-row))
            separator (when header
                        (let [cols (count (direct-children-by-tag header-row #{"th" "td"}))]
                          (str "| " (str/join " | " (repeat cols "---")) " |")))
            body-rows (->> (rest rows)
                           (map table-row->md)
                           (remove nil?))]
        (str "\n\n"
             (when header (str header "\n" separator "\n"))
             (str/join "\n" body-rows)
             "\n\n"))

      ;; Divs and other block containers — just recurse, add block breaks
      (block-tag? tag)
      (str "\n\n" (str/trim (walk-children el)) "\n\n")

      ;; Inline / unknown — just recurse
      :else
      (walk-children el))))

(defn- walk
  "Walks a Jsoup Node (Element or TextNode) and returns Markdown string."
  [node]
  (cond
    (instance? TextNode node)
    (let [text (.getWholeText ^TextNode node)]
      ;; Normalize whitespace in text nodes (but preserve intentional newlines)
      (-> text
          repair-mojibake
          (str/replace #"[ \t]+" " ")))

    (instance? Element node)
    (walk-element node)

    :else ""))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn- collapse-table-gaps
  "Removes empty lines accidentally inserted between contiguous markdown table rows."
  [s]
  (let [lines (str/split-lines s)]
    (->> lines
         (reduce (fn [acc line]
                   (let [prev (peek acc)]
                     (if (and (str/blank? line)
                              (markdown-table-line? prev))
                       acc
                       (conj acc line))))
                 [])
         (reduce (fn [acc line]
                   (let [prev (peek acc)]
                     (if (and (str/blank? prev)
                              (markdown-table-line? line)
                              (>= (count acc) 2)
                              (markdown-table-line? (nth acc (- (count acc) 2))))
                       (conj (pop acc) line)
                       (conj acc line))))
                 [])
         (str/join "\n"))))

(defn element->html
  "Clones a Jsoup element, removes UI noise (buttons), returns clean outer HTML.
  Does not mutate the original element."
  [^Element el]
  (let [clone (.clone el)]
    (doseq [btn (.select clone "button")]
      (.remove btn))
    (.outerHtml clone)))

(defn html->md
  "Converts an HTML string to Markdown via DOM walking."
  [^String html]
  (let [doc  (Jsoup/parseBodyFragment html)
        body (.body doc)]
    (-> (walk-children body)
        collapse-table-gaps
        (str/replace #"[ \t]+\n" "\n")        ; trailing whitespace on lines
        (str/replace #"\n{3,}" "\n\n")
        str/trim)))

(defn html->text
  "Converts an HTML string to normalized plain text."
  [^String html]
  (-> (Jsoup/parseBodyFragment html)
      .body
      .text
      normalize))

(defn element->md
  "Extracts text from a Jsoup element, rendering <pre><code> blocks as Markdown
  fenced blocks. Non-code text is normalized. Works on a clone; does not mutate el.
  DEPRECATED: use element->html + html->md for structured Markdown."
  [^Element el]
  (-> (element->html el) html->md))
