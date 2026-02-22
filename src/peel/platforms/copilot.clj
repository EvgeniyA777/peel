(ns peel.platforms.copilot
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; Copilot wraps each turn in divs with Tailwind group/ classes.
;; User messages: class contains "group/user-message"; text lives in .whitespace-pre-wrap.
;; AI messages: class contains "group/ai-message-item" (child of "group/ai-message" wrapper).
;; The outer "group/ai-message" also contains a "Copilot said" screen-reader label — skip it.
(def ^:private msg-selector "[class*=group/user-message], [class*=group/ai-message-item]")

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc msg-selector)
       (map (fn [el]
              (let [cls (.attr el "class")]
                (if (.contains cls "user")
                  {:role :user
                   :text (-> (or (.selectFirst el ".whitespace-pre-wrap") el)
                             .text
                             text/normalize)}
                  {:role :assistant
                   :text (text/normalize (.text el))}))))
       (remove (comp str/blank? :text))
       vec))
