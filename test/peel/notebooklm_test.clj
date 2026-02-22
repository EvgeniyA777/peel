(ns peel.notebooklm-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.notebooklm :as notebooklm]))

(def ^:private fixture-path
  "test/peel/fixtures/Adapting to AI and a Changing World - NotebookLM (2_22_2026 2：51：25 PM).html")

(def ^:private sample-path
  "test/peel/fixtures/notebooklm-sample.html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :notebooklm)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (notebooklm/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))

(deftest no-ui-noise
  (h/check-no-noise (notebooklm/extract (h/load-doc sample-path))
                    ["Pin this message to a Note" "Save message to a note"]))
