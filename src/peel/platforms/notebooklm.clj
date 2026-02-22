(ns peel.platforms.notebooklm
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; NotebookLM uses <chat-message> custom elements.
;; User messages contain div.from-user-container, assistant contain div.to-user-container.
;; Assistant messages include a mat-card-actions toolbar (Pin button) that must be removed.
;; Pages without a chat session have no <chat-message> elements and return [].
(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc "chat-message")
       (map (fn [el]
              (doseq [noise (.select el "mat-card-actions")]
                (.remove noise))
              {:role (if (.selectFirst el "div.from-user-container") :user :assistant)
               :text (-> (.selectFirst el ".message-text-content")
                         (.text)
                         text/normalize)}))
       (remove (comp str/blank? :text))
       vec))
