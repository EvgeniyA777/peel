(ns peel.gemini-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.gemini :as gemini]))

(def ^:private fixture-path
  "test/peel/fixtures/Google Gemini (2_22_2026 12：24：18 PM).html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :gemini)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (gemini/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))
