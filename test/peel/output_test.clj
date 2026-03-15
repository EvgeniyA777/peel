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

(deftest markdown-distinguishes-user-and-assistant
  (let [md (output/render {:title "Test"
                           :messages [{:role :user
                                       :text "Question line 1\nQuestion line 2"}
                                      {:role :assistant
                                       :text "Answer body"}]}
                          :md)]
    (testing "user prompt renders with a prominent heading and quote block"
      (is (clojure.string/includes? md "## User prompt"))
      (is (clojure.string/includes? md "> Question line 1"))
      (is (clojure.string/includes? md "> Question line 2")))
    (testing "assistant answer renders with a prominent heading"
      (is (clojure.string/includes? md "## Assistant response"))
      (is (clojure.string/includes? md "Answer body")))))
