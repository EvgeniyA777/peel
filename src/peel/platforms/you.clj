(ns peel.platforms.you
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; User messages: [data-testid^=youchat-question-turn-N].
;; Assistant messages: [data-testid^=youchat-answer-turn-N].
;; Both are numbered from 0 and appear interleaved in DOM order.
;;
;; Noise to remove before text extraction:
;;   - [data-testid=youchat-citation-trigger] — superscript citation numbers ([1],[2]…).
;;   - h4 containing "Auto Agent" — upsell quota banner injected by You.com.
;;   - span containing "Answering your question with the Express Agent" — agent notice.
(def ^:private msg-selector
  "[data-testid^=youchat-question-turn], [data-testid^=youchat-answer-turn]")

(defn- clean-answer [el]
  (doseq [e (.select el "[data-testid=youchat-citation-trigger]")] (.remove e))
  (doseq [e (.select el "h4")]
    (when (.contains (.text e) "Auto Agent") (.remove e)))
  (doseq [e (.select el "span")]
    (when (.contains (.text e) "Answering your question") (.remove e))))

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc msg-selector)
       (map (fn [el]
              (let [tid (.attr el "data-testid")]
                (if (.contains tid "question")
                  {:role :user
                   :text (text/normalize (.text el))}
                  (do
                    (clean-answer el)
                    {:role :assistant
                     :text (text/normalize (.text el))})))))
       (remove (comp str/blank? :text))
       vec))
