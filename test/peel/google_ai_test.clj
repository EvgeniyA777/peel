(ns peel.google-ai-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.google-ai :as google-ai]))

(def ^:private fixture-path
  "test/peel/fixtures/Write a Python function that checks if a number is prime. - Google Search (2_22_2026 12：22：45 PM).html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :google-ai)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (google-ai/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))
