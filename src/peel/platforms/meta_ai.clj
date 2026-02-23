(ns peel.platforms.meta-ai
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; User messages are in div[class*=group/user-message].
;; The actual text bubble is div.bg-fill-secondary — avoids the timestamp sibling.
;; Assistant messages are in div[data-testid=assistant-message].
;; Both appear interleaved in DOM order matching conversation flow.
(def ^:private msg-selector "[class*=group/user-message], [data-testid=assistant-message]")

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc msg-selector)
       (map (fn [el]
              (if (.contains (.attr el "class") "user")
                {:role :user
                 :text (text/element->md (.selectFirst el ".bg-fill-secondary"))}
                (do
                  (doseq [cite (.select el "[data-testid=citation-pill],[data-testid=sources-pill]")]
                    (.remove cite))
                  (doseq [ui (.select el "div[data-streamdown=code-block-header],span.text-caption-1")]
                    (.remove ui))
                  {:role :assistant
                   :text (text/element->md el)}))))
       (remove (comp str/blank? :text))
       vec))
