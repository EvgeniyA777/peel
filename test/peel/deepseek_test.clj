(ns peel.deepseek-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.deepseek :as deepseek]))

(def ^:private fixture-path
  "test/peel/fixtures/Python Prime Number Check Function - DeepSeek (2_22_2026 12：14：57 PM).html")

(def ^:private sample-path "test/peel/fixtures/deepseek-sample.html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :deepseek)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (deepseek/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))

(deftest no-ui-noise
  (h/check-no-noise (deepseek/extract (h/load-doc sample-path))
                    ["Download"]))
