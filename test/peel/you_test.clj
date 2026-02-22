(ns peel.you-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.you :as you]))

(def ^:private sample-path "test/peel/fixtures/you-sample.html")

(def ^:private fixture-path
  "test/peel/fixtures/Обзор Clojure - You.com ｜ AI for workplace productivity (2_22_2026 12：31：24 PM).html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :you)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (you/extract (h/load-doc fixture-path))
                       6 [:user :assistant :user :assistant])))

(deftest no-ui-noise
  (h/check-no-noise (you/extract (h/load-doc sample-path))
                    ["1" "2" " ."]))
