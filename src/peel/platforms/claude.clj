(ns peel.platforms.claude
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; User messages are marked with a stable test id.
;; Assistant messages carry data-is-streaming on their root div.
;; Both selectors are matched in a single query to preserve DOM order.
(def ^:private msg-selector "[data-testid=user-message], [data-is-streaming=false]")

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc msg-selector)
       (map (fn [el]
              ;; Remove copy buttons and other toolbar UI from code blocks
              ;; before extracting text to avoid noise like "Copy" in output.
              (doseq [btn (.select el "button")]
                (.remove btn))
              (if (= "user-message" (.attr el "data-testid"))
                {:role :user
                 :text (text/element->md el)}
                (let [blocks (.select el ".standard-markdown")]
                  {:role :assistant
                   :text (->> (if (seq blocks) blocks [el])
                              (map text/element->md)
                              (str/join "\n\n"))}))))
       (remove (comp str/blank? :text))
       vec))
