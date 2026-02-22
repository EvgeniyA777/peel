(ns peel.grok-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.grok :as grok]))

(def ^:private fixture-path
  "test/peel/fixtures/Python Prime Number Check Function - Grok (2_22_2026 12：21：11 PM).html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :grok)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (grok/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))
