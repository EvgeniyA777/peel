(ns peel.platforms.chatgpt
  (:require [clojure.string :as str]
            [peel.text :as text]))

;; Each conversational turn is wrapped in an <article data-turn=user|assistant>.
;; User text lives in div.whitespace-pre-wrap (plain text, no markdown).
;; Assistant text lives in div.markdown (rendered HTML — paragraphs, lists, code).
(def ^:private turn-selector "article[data-turn]")

(defn- extract-text [el role]
  (case role
    "user"      (let [div (.selectFirst el "div.whitespace-pre-wrap")]
                  (if div (text/element->md div) ""))
    "assistant" (let [div (.selectFirst el "div.markdown")]
                  (if div
                    (do (doseq [toolbar (.select div "div[class*=bg-token-sidebar-surface-primary]")]
                          (.remove toolbar))
                        (text/element->md div))
                    ""))
    ""))

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc turn-selector)
       (map (fn [el]
              (let [role (.attr el "data-turn")]
                {:role (keyword role)
                 :text (extract-text el role)})))
       (remove (comp str/blank? :text))
       vec))
