(ns peel.meta-ai-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.meta-ai :as meta-ai]))

(def ^:private fixture-path
  "test/peel/fixtures/Meta AI (2_22_2026 12：27：58 PM).html")

(def ^:private sample-path "test/peel/fixtures/meta-ai-sample.html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :meta-ai)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (meta-ai/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))

(deftest no-ui-noise
  (h/check-no-noise (meta-ai/extract (h/load-doc sample-path))
                    ["loc ·"]))
