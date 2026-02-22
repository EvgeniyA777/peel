(ns peel.text
  (:require [clojure.string :as str]))

(defn normalize
  "Text normalization shared across all platform extractors.
  Strips emoji and non-text symbols, collapses runs of spaces,
  caps consecutive newlines at two, and trims."
  [s]
  (-> s
      (str/replace #"[^\p{L}\p{N}\p{P}\p{Sm}\p{Sc}\p{Z}\n]" "") ; strip emoji, keep +=/$
      (str/replace #" +" " ")
      (str/replace #"\n{3,}" "\n\n")
      str/trim))
