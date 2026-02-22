(ns peel.platforms.perplexity
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; User queries have class containing "group/query".
;; Assistant answers have class "prose dark:prose-invert".
;; Both are matched together to preserve DOM order.
(def ^:private msg-selector "[class*=group/query], .prose")

;; Perplexity renders inline citations as <span data-pplx-citation-url=...>domain</span>
;; followed by <span class="opacity-50">+N</span> for grouped citations.
;; Both are noise and should be removed before text extraction.

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc msg-selector)
       (map (fn [el]
              (doseq [cite (.select el "[data-pplx-citation-url]")]
                (.remove cite))
              (doseq [badge (.select el "span.opacity-50")]
                (.remove badge))
              (doseq [btn (.select el "button")]
                (.remove btn))
              {:role (if (.contains (.attr el "class") "group/query")
                       :user
                       :assistant)
               :text (text/normalize (.text el))}))
       (remove (comp str/blank? :text))
       vec))
