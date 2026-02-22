(ns peel.platforms.perplexity
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; User queries have class containing "group/query".
;; Assistant answers have class "prose dark:prose-invert".
;; Both are matched together to preserve DOM order.
(def ^:private msg-selector "[class*=group/query], .prose")

;; Perplexity has two citation types:
;;   [data-pplx-citation] — linked citations with a URL attribute
;;   span[class*=rounded-badge] — standalone domain badges ("python", "devguide.python")
;;     without a data-pplx-citation attribute; they appear inline after sentences.
;; Code blocks are preceded by [data-testid=code-language-indicator] labels ("python" etc.).

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc msg-selector)
       (map (fn [el]
              (doseq [cite (.select el "[data-pplx-citation], span[class*=rounded-badge]")]
                (.remove cite))
              (doseq [badge (.select el "span.opacity-50")]
                (.remove badge))
              (doseq [btn (.select el "button")]
                (.remove btn))
              (doseq [lang (.select el "[data-testid=code-language-indicator]")]
                (.remove lang))
              {:role (if (.contains (.attr el "class") "group/query")
                       :user
                       :assistant)
               :text (text/normalize (.text el))}))
       (remove (comp str/blank? :text))
       vec))
