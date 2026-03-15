(ns peel.text-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [peel.text :as text]))

(deftest paragraphs-preserved
  (testing "separate paragraphs remain separated"
    (let [md (text/html->md "<p>First</p><p>Second</p><p>Third</p>")]
      (is (str/includes? md "First\n\nSecond"))
      (is (str/includes? md "Second\n\nThird")))))

(deftest headings-preserved
  (testing "headings render with # prefix"
    (let [md (text/html->md "<h1>Title</h1><h2>Sub</h2><h3>Deep</h3>")]
      (is (str/includes? md "# Title"))
      (is (str/includes? md "## Sub"))
      (is (str/includes? md "### Deep")))))

(deftest unordered-list-preserved
  (testing "unordered list renders as bullets"
    (let [md (text/html->md "<ul><li>alpha</li><li>beta</li><li>gamma</li></ul>")]
      (is (str/includes? md "- alpha"))
      (is (str/includes? md "- beta"))
      (is (str/includes? md "- gamma")))))

(deftest ordered-list-preserved
  (testing "ordered list renders with numbers"
    (let [md (text/html->md "<ol><li>first</li><li>second</li><li>third</li></ol>")]
      (is (str/includes? md "1. first"))
      (is (str/includes? md "2. second"))
      (is (str/includes? md "3. third")))))

(deftest nested-list-preserved
  (testing "nested list retains indentation"
    (let [md (text/html->md "<ul><li>top<ul><li>nested</li></ul></li></ul>")]
      (is (str/includes? md "- top"))
      (is (str/includes? md "  - nested")))))

(deftest blockquote-preserved
  (testing "blockquote renders with > prefix"
    (let [md (text/html->md "<blockquote><p>quoted text</p></blockquote>")]
      (is (str/includes? md "> quoted text")))))

(deftest code-fence-preserved
  (testing "pre/code renders as fenced code block"
    (let [md (text/html->md "<pre><code class=\"language-python\">x = 1</code></pre>")]
      (is (str/includes? md "```python"))
      (is (str/includes? md "x = 1"))
      (is (str/includes? md "```")))))

(deftest inline-formatting-preserved
  (testing "bold and italic render correctly"
    (let [md (text/html->md "<p><strong>bold</strong> and <em>italic</em></p>")]
      (is (str/includes? md "**bold**"))
      (is (str/includes? md "*italic*")))))

(deftest inline-code-preserved
  (testing "inline code renders with backticks"
    (let [md (text/html->md "<p>Use <code>foo()</code> here</p>")]
      (is (str/includes? md "`foo()`")))))

(deftest br-becomes-newline
  (testing "<br> maps to a line break"
    (let [md (text/html->md "<p>line one<br>line two</p>")]
      (is (str/includes? md "line one\nline two")))))

(deftest buttons-removed
  (testing "button elements are stripped from output"
    (let [md (text/element->md
               (.body (org.jsoup.Jsoup/parseBodyFragment
                        "<p>Hello</p><button>Copy</button><p>World</p>")))]
      (is (not (str/includes? md "Copy")))
      (is (str/includes? md "Hello"))
      (is (str/includes? md "World")))))

(deftest links-preserved
  (testing "links render as markdown links"
    (let [md (text/html->md "<p><a href=\"https://example.com\">click</a></p>")]
      (is (str/includes? md "[click](https://example.com)")))))

(deftest tables-render-as-contiguous-gfm
  (testing "table rows are emitted as one contiguous markdown table block"
    (let [md (text/html->md "<table><thead><tr><th><p>Field</p></th><th><p>Description</p></th></tr></thead><tbody><tr><td><p>title</p></td><td><p>name of the concept</p></td></tr><tr><td><p><strong>narrative implications</strong></p></td><td><p>story material</p></td></tr></tbody></table>")]
      (is (str/includes? md "| Field | Description |"))
      (is (str/includes? md "| --- | --- |"))
      (is (str/includes? md "| title | name of the concept |"))
      (is (str/includes? md "| **narrative implications** | story material |"))
      (is (not (str/includes? md "| Field | Description |\n\n| --- | --- |")))
      (is (not (str/includes? md "| --- | --- |\n\n| title | name of the concept |"))))))

(deftest query-text-lines-preserve-markdown-tables
  (testing "query-text-line table-style content stays a contiguous markdown block"
    (let [md (text/html->md "<p class=\"query-text-line\">### From Scientific Sources</p><p class=\"query-text-line\"><br></p><p class=\"query-text-line\">| Field | Description |</p><p class=\"query-text-line\">|---|---|</p><p class=\"query-text-line\">| title | name of the concept |</p><p class=\"query-text-line\">| domain | area of knowledge |</p>")]
      (is (str/includes? md "### From Scientific Sources\n\n| Field | Description |\n|---|---|\n| title | name of the concept |\n| domain | area of knowledge |"))
      (is (not (str/includes? md "| Field | Description |\n\n|---|---|")))
      (is (not (str/includes? md "|---|---|\n\n| title | name of the concept |"))))))

(deftest html-to-text-is-plain
  (testing "html->text returns plain text without formatting"
    (let [txt (text/html->text "<p><strong>bold</strong></p><ul><li>item</li></ul>")]
      (is (not (str/includes? txt "**")))
      (is (not (str/includes? txt "- ")))
      (is (str/includes? txt "bold"))
      (is (str/includes? txt "item")))))

(deftest mojibake-repaired-in-markdown
  (testing "html->md repairs common mojibake punctuation"
    (let [md (text/html->md "<p>MuseEngine â€” Unified Design Document â†’ v2</p>")]
      (is (str/includes? md "MuseEngine — Unified Design Document"))
      (is (str/includes? md "→ v2"))
      (is (not (str/includes? md "â€”")))
      (is (not (str/includes? md "â†’"))))))

(deftest mojibake-repaired-in-plain-text
  (testing "html->text repairs common mojibake punctuation"
    (let [txt (text/html->text "<p>Not AI for writing â€” preserve meaning â†’ structure</p>")]
      (is (str/includes? txt "Not AI for writing — preserve meaning → structure"))
      (is (not (str/includes? txt "â€”")))
      (is (not (str/includes? txt "â†’"))))))
