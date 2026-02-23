(ns peel.platforms.huggingchat
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; Each message is wrapped in [data-message-id].
;; Role is determined by inner element presence:
;;   user:      has p.whitespace-break-spaces (text bubble, no "Edit" button noise).
;;   assistant: has div.prose (rendered markdown; excludes model footer like
;;              "agentic with … via groq Copied").
(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc "[data-message-id]")
       (map (fn [el]
              (let [p (.selectFirst el "p.whitespace-break-spaces")]
                (if p
                  {:role :user
                   :text (text/element->md p)}
                  (let [prose (.selectFirst el ".prose")]
                    (doseq [overlay (.select prose "div.pointer-events-none.sticky")]
                      (.remove overlay))
                    {:role :assistant
                     :text (text/element->md prose)})))))
       (remove (comp str/blank? :text))
       vec))
