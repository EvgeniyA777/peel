(ns peel.platforms.gemini
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; Gemini wraps each turn in custom elements: <user-query> and <model-response>.
;; User text always starts with "You said " — a screen-reader label, not content.
;; Assistant full text lives in the <message-content> custom element inside model-response.
;; Both custom elements appear interleaved in DOM order matching conversation flow.
(def ^:private msg-selector "user-query, model-response")

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc msg-selector)
       (map (fn [el]
              (if (= "user-query" (.tagName el))
                {:role :user
                 :text (-> (.text el)
                           (str/replace-first #"^You said\s+" "")
                           text/normalize)}
                (let [mc (.selectFirst el "message-content")]
                  {:role :assistant
                   :text (text/normalize (.text (or mc el)))}))))
       (remove (comp str/blank? :text))
       vec))
