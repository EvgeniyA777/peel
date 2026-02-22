(ns peel.copilot-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.copilot :as copilot]))

(def ^:private fixture-path
  "test/peel/fixtures/Microsoft Copilot： Your AI companion (2_22_2026 12：25：43 PM).html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :copilot)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (copilot/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))
