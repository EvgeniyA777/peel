(ns peel.output-test
  (:require [clojure.test :refer [deftest testing is]]
            [peel.output :as output]))

(def ^:private sample
  {:path     "chat.html"
   :title    "Test"
   :platform :claude
   :messages [{:role :user      :text "Hello"}
              {:role :assistant :text "Hi"}]})

(deftest json-is-pretty-printed
  (let [json (output/render sample :json)]
    (testing "output spans multiple lines"
      (is (> (count (clojure.string/split-lines json)) 1)))
    (testing "keys are indented"
      (is (re-find #"(?m)^\s+\"" json)))))
