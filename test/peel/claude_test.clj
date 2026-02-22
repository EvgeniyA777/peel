(ns peel.claude-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.claude :as claude]))

(def ^:private fixture-path
  "test/peel/fixtures/Prime number checker in Python - Claude (2_22_2026 12：18：18 PM).html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :claude)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (claude/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))
