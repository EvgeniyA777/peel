(ns peel.helpers
  (:require [clojure.test :refer [is testing]]
            [peel.detect :as detect])
  (:import [org.jsoup Jsoup]))

(defn load-doc [path]
  (Jsoup/parse (java.io.File. path) "UTF-8"))

(defn with-fixture [path f]
  (if (.exists (java.io.File. path))
    (f)
    (println (str "  SKIP (fixture not found): " path))))

(defn check-platform [doc expected-platform]
  (testing (str "detects " (name expected-platform) " platform from fixture")
    (is (= expected-platform (detect/platform doc)))))

(defn check-no-noise [messages banned-strings]
  (testing "no UI toolbar noise in extracted text"
    (doseq [msg messages
            s    banned-strings]
      (is (not (clojure.string/includes? (:text msg) s))
          (str "found \"" s "\" in :" (name (:role msg)) " message")))))

(defn check-messages [messages expected-count expected-roles]
  (testing "returns a vector"
    (is (vector? messages)))
  (testing "each message has :role and :text"
    (doseq [msg messages]
      (is (contains? msg :role))
      (is (contains? msg :text))
      (is (not (clojure.string/blank? (:text msg))))))
  (testing "roles are :user or :assistant"
    (doseq [msg messages]
      (is (#{:user :assistant} (:role msg)))))
  (testing "fixture contains expected number of messages"
    (is (= expected-count (count messages))))
  (when expected-roles
    (testing "roles start correctly"
      (is (= expected-roles
             (vec (take (count expected-roles) (map :role messages))))))))
