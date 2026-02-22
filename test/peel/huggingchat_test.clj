(ns peel.huggingchat-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.huggingchat :as huggingchat]))

(def ^:private fixture-path
  "test/peel/fixtures/Prime number check function (2_22_2026 12：30：13 PM).html")

(def ^:private sample-path "test/peel/fixtures/huggingchat-sample.html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :huggingchat)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (huggingchat/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))

(deftest no-ui-noise
  (h/check-no-noise (huggingchat/extract (h/load-doc sample-path))
                    ["Copied"]))
