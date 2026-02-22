(ns peel.output
  (:require [cheshire.core :as json]
            [clojure.string :as str]))

(defn- render-md [{:keys [title messages]}]
  (str "# " title "\n\n"
       (str/join "\n\n"
                 (map (fn [{:keys [role text]}]
                        (str "**" (name role) ":** " text))
                      messages))))

(defn render [result fmt]
  (case fmt
    :json (json/generate-string result {:pretty true})
    :edn  (pr-str result)
    :md   (render-md result)))
