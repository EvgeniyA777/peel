(ns peel.output
  (:require [cheshire.core :as json]
            [clojure.string :as str]))

(defn- quote-block [text]
  (->> (str/split-lines (or text ""))
       (map (fn [line]
              (if (str/blank? line)
                ">"
                (str "> " line))))
       (str/join "\n")))

(defn- render-message-md [{:keys [role text]}]
  (case role
    :user
    (str "## User prompt\n\n"
         (quote-block text))

    :assistant
    (str "## Assistant response\n\n" text)

    (str "## " (str/capitalize (name role)) "\n\n" text)))

(defn- render-md [{:keys [title messages]}]
  (str "# " title "\n\n"
       (str/join "\n\n---\n\n"
                 (map render-message-md messages))))

(defn render [result fmt]
  (case fmt
    :json (json/generate-string result {:pretty true})
    :edn  (pr-str result)
    :md   (render-md result)))
