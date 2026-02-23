(ns peel.platforms.mistral
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; Each message (user and assistant) is wrapped in [data-message-id].
;; Role is distinguished by the presence of [data-testid=text-message-part]:
;;   assistant: has it — use it directly (clean rendered markdown text).
;;   user: no text-message-part — text lives in span.whitespace-pre-wrap.
;; Timestamps ("11:31pm") live in div.text-hint and are avoided by targeting
;; the specific inner elements rather than the whole message container.
(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc "[data-message-id]")
       (map (fn [el]
              (let [tmpl (.selectFirst el "[data-testid=text-message-part]")]
                (if tmpl
                  (do (doseq [toolbar (.select tmpl "div[data-exclude-copy=true]")]
                        (.remove toolbar))
                      {:role :assistant
                       :text (text/element->md tmpl)})
                  {:role :user
                   :text (text/element->md (.selectFirst el "span.whitespace-pre-wrap"))}))))
       (remove (comp str/blank? :text))
       vec))
