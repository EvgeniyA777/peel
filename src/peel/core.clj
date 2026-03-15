(ns peel.core
  (:require [peel.detect              :as detect]
            [peel.output              :as output]
            [peel.platforms.deepseek  :as deepseek]
            [peel.platforms.claude    :as claude]
            [peel.platforms.chatgpt   :as chatgpt]
            [peel.platforms.grok        :as grok]
            [peel.platforms.perplexity  :as perplexity]
            [peel.platforms.google-ai   :as google-ai]
            [peel.platforms.gemini      :as gemini]
            [peel.platforms.copilot     :as copilot]
            [peel.platforms.mistral     :as mistral]
            [peel.platforms.meta-ai     :as meta-ai]
            [peel.platforms.huggingchat :as huggingchat]
            [peel.platforms.you         :as you]
            [peel.platforms.notebooklm  :as notebooklm]
            [clojure.string           :as str])
  (:import [org.jsoup Jsoup]
           [java.io File]))

(def extractors
  {:deepseek deepseek/extract
   :claude   claude/extract
   :chatgpt  chatgpt/extract
   :grok       grok/extract
   :perplexity perplexity/extract
   :google-ai  google-ai/extract
   :gemini     gemini/extract
   :copilot    copilot/extract
   :mistral    mistral/extract
   :meta-ai    meta-ai/extract
   :huggingchat huggingchat/extract
   :you         you/extract
   :notebooklm  notebooklm/extract})

(defn- die [msg]
  (binding [*out* *err*] (println msg))
  (System/exit 1))

(defn parse-html [path]
  (when-not (.exists (File. path))
    (die (str "peel: file not found: " path)))
  (let [doc      (Jsoup/parse (File. path) "UTF-8")
        _        (doseq [el (.select doc "script,style,svg,noscript")]
                   (.remove el))
        platform (detect/platform doc)
        extract  (get extractors platform
                      (fn [_] [{:role :unknown
                                :text (.text (.body doc))}]))]
    {:path     path
     :title    (.title doc)
     :platform platform
     :messages (extract doc)}))

(defn- expand-home [path]
  (if (str/starts-with? path "~/")
    (str (System/getProperty "user.home") (subs path 1))
    path))

(defn- parse-opt [opts prefix]
  (some #(when (str/starts-with? % prefix)
           (expand-home (subs % (count prefix))))
        opts))

(defn- html-files [path]
  (let [f (File. path)]
    (cond
      (.isDirectory f) (->> (.listFiles f)
                            (filter #(str/ends-with? (.getName %) ".html"))
                            (map #(.getPath %)))
      (.isFile f)      [path]
      :else            (die (str "peel: not a file or directory: " path)))))

(defn- out-path [out-dir src-path fmt]
  (let [name (-> (File. src-path) .getName)
        ext  (case fmt :md ".md" :json ".json" ".edn")
        base (if (str/ends-with? name ".html")
               (subs name 0 (- (count name) 5))
               name)]
    (str out-dir File/separator base ext)))

(defn -main [& args]
  (let [[inputs opts] (split-with #(not (str/starts-with? % "--")) args)
        fmt           (cond (some #{"--json"} opts) :json
                            (some #{"--md"}   opts) :md
                            :else                   :edn)
        out-dir       (parse-opt opts "--out=")]
    (when (empty? inputs)
      (println "Usage: bb peel <file.html|dir/> [--md|--json] [--out=/path/]")
      (System/exit 1))
    (when out-dir
      (let [d (File. out-dir)]
        (when-not (.isDirectory d)
          (die (str "peel: output directory not found: " out-dir)))))
    (doseq [input inputs
            path  (html-files input)]
      (let [result   (parse-html path)
            rendered (output/render result fmt)]
        (if out-dir
          (spit (out-path out-dir path fmt) rendered)
          (println rendered))))))
