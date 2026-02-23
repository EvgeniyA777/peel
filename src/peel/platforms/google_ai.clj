(ns peel.platforms.google-ai
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; Google AI Mode chat panel structure:
;; User queries are in div.sUKAcb (one per turn).
;; AI responses are in div.mZJni — contains the full answer text.
;; div[data-subtree=aimfl] is only a partial fragment inside mZJni, skip it.
;; Both appear interleaved in DOM order matching conversation flow.
(def ^:private msg-selector ".sUKAcb, .mZJni")

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc msg-selector)
       (map (fn [el]
              (doseq [btn (.select el "button")]
                (.remove btn))
              {:role (if (.hasClass el "sUKAcb") :user :assistant)
               :text (text/element->md el)}))
       (remove (comp str/blank? :text))
       vec))
