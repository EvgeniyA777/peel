(ns peel.chatgpt-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.chatgpt :as chatgpt]))

(def ^:private fixture-path
  "test/peel/fixtures/Prime Number Checker (2_22_2026 12：19：52 PM).html")

(def ^:private sample-path "test/peel/fixtures/chatgpt-sample.html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :chatgpt)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (chatgpt/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))

(deftest no-ui-noise
  (h/check-no-noise (chatgpt/extract (h/load-doc sample-path))
                    ["Copy code"]))
