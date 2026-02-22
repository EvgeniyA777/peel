(ns peel.mistral-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.mistral :as mistral]))

(def ^:private fixture-path
  "test/peel/fixtures/Le Chat (2_22_2026 12：26：46 PM).html")

(def ^:private sample-path "test/peel/fixtures/mistral-sample.html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :mistral)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (mistral/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))

(deftest no-ui-noise
  (h/check-no-noise (mistral/extract (h/load-doc sample-path))
                    ["Copy"]))
