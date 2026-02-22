(ns peel.platforms.notebooklm
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; NotebookLM uses <chat-message> custom elements.
;; User messages contain div.from-user-container, assistant contain div.to-user-container.
;; Assistant messages include a mat-card-actions toolbar (Pin button) that must be removed.
;; Citation spans have aria-label="N: Source Title" and must be removed before text extraction.
;; Pages without a chat session have no <chat-message> elements and return [].
(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc "chat-message")
       (map (fn [el]
              (doseq [noise (.select el "mat-card-actions, span[aria-label]")]
                (.remove noise))
              {:role (if (.selectFirst el "div.from-user-container") :user :assistant)
               :text (-> (.selectFirst el ".message-text-content")
                         (.text)
                         (str/replace #" +([.!?,;:])" "$1")
                         text/normalize)}))
       (remove (comp str/blank? :text))
       vec))
