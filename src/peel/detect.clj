(ns peel.detect)

(defn platform [^org.jsoup.nodes.Document doc]
  (cond
    (.selectFirst doc "meta[name=commit-id]")               :deepseek
    (.selectFirst doc "meta[content*=claude.ai]")           :claude
    (.selectFirst doc "link[rel=canonical][href*=chatgpt.com]") :chatgpt
    (.selectFirst doc "link[rel=canonical][href*=grok.com]")          :grok
    (.selectFirst doc "link[rel=canonical][href*=perplexity.ai]")     :perplexity
    (.selectFirst doc "link[rel=canonical][href*=google.com/search]") :google-ai
    (.selectFirst doc "link[rel=canonical][href*=gemini.google.com]") :gemini
    (.selectFirst doc "link[rel=canonical][href*=copilot.microsoft.com]") :copilot
    (.selectFirst doc "link[rel=canonical][href*=chat.mistral.ai]")       :mistral
    (.selectFirst doc "link[rel=canonical][href*=meta.ai]")               :meta-ai
    (.selectFirst doc "link[rel=canonical][href*=huggingface.co/chat]")   :huggingchat
    (.selectFirst doc "link[rel=canonical][href*=you.com/search]")        :you
    (.selectFirst doc "link[rel=canonical][href*=notebooklm.google.com]") :notebooklm
    :else                                                    :unknown))
