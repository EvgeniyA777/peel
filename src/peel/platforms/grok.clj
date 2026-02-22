(ns peel.platforms.grok
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; Each message (user or assistant) is a div.message-bubble.
;; User bubbles have bg-surface-l1 class (pill background).
;; Assistant bubbles have w-full class (full-width, no background pill).
;; Grok also renders a thinking section per assistant turn — it lives in a
;; separate div.response-content-markdown but is already excluded because
;; we read text directly from the message-bubble element.
(def ^:private msg-selector "div.message-bubble")

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc msg-selector)
       (map (fn [el]
              (doseq [btn (.select el "button")]
                (.remove btn))
              {:role (if (.contains (.attr el "class") "bg-surface-l1")
                       :user
                       :assistant)
               :text (text/normalize (.text el))}))
       (remove (comp str/blank? :text))
       vec))
