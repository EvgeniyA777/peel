(ns peel.perplexity-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.perplexity :as perplexity]))

(def ^:private sample-path "test/peel/fixtures/perplexity-sample.html")

(def ^:private fixture-path
  "test/peel/fixtures/What is the current Python version？ (2_22_2026 12：16：56 PM).html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :perplexity)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (perplexity/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))

(deftest no-ui-noise
  (h/check-no-noise (perplexity/extract (h/load-doc sample-path))
                    ["example.com" "wiki.org" "python"]))
