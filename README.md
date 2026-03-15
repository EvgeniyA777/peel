# peel

Babashka CLI for extracting chat dialogues from saved AI chat web pages into EDN, JSON, and Markdown formats.

> **Web versions only.** peel works exclusively with HTML files saved from the browser.

> **Text-focused.** Extracts plain text and code blocks. Code blocks are rendered as fenced Markdown with language identifiers.

## How to get an HTML file

Open the chat in your browser and save the page as a single HTML file. The recommended way is the [SingleFile](https://github.com/gildas-lormeau/SingleFile) browser extension — it packs everything (styles, scripts, images) into one `.html` file. Other browser save methods work too, as long as the full chat DOM is present.

## External input folders

`peel` does not define or depend on any special corpus directory name.
Any external folder that contains saved chat `.html` files can be used as input:

- `~/Downloads/ai-chats/`
- `~/archive/chat-html/`
- a temporary evaluation folder
- a personal export archive

Directory names are local operator conventions only, not part of the `peel` system model or documentation canon.

## Supported platforms

| Platform | Web URL | Status |
|---|---|---|
| DeepSeek | chat.deepseek.com | Implemented |
| Claude | claude.ai | Implemented |
| ChatGPT | chatgpt.com | Implemented |
| Grok | grok.com | Implemented |
| Perplexity | perplexity.ai | Implemented |
| Google AI Mode | google.com/search | Implemented |
| Gemini | gemini.google.com | Implemented |
| Microsoft Copilot | copilot.microsoft.com | Implemented |
| Mistral (Le Chat) | chat.mistral.ai | Implemented |
| Meta AI | meta.ai | Implemented |
| HuggingChat | huggingface.co/chat | Implemented |
| You.com | you.com | Implemented |
| NotebookLM | notebooklm.google.com | Implemented (chat only) |

## Requirements

[babashka](https://github.com/babashka/babashka)

```bash
git clone <repo>
cd peel
```

## Usage

```bash
# Single file — output printed to terminal (run from project root)
bb peel test/peel/fixtures/claude-sample.html --md

# Single file — save to directory (creates claude-sample.md inside fixtures/)
bb peel test/peel/fixtures/claude-sample.html --md --out=test/peel/fixtures/

# All HTML files in a directory — save to the same directory
bb peel test/peel/fixtures/ --md --out=test/peel/fixtures/

# All HTML files in a directory — save to another directory (out/ created in project root)
mkdir -p out/ && bb peel test/peel/fixtures/ --md --out=out/

# JSON — printed to terminal
bb peel test/peel/fixtures/claude-sample.html --json

# JSON — saved to file (creates claude-sample.json inside fixtures/)
bb peel test/peel/fixtures/claude-sample.html --json --out=test/peel/fixtures/

# EDN — printed to terminal (default format)
bb peel test/peel/fixtures/claude-sample.html

# EDN — saved to file (creates claude-sample.edn inside fixtures/)
bb peel test/peel/fixtures/claude-sample.html --out=test/peel/fixtures/
```

## Output formats

- `--md` — Markdown, suitable for knowledge bases and chunking pipelines
- `--json` — JSON
- (default) — EDN

## Documentation canon

- `README.md` — user-facing CLI contract: purpose, usage, supported platforms
- `CLAUDE.md` — agent/developer guide: architecture, extractor patterns, testing workflow
- `PROJECT.md` — project metadata and documentation map
- `adr/` — architecture decisions
- `notes/` — working notes only, not canonical product documentation

## Documentation rules

- Canonical project docs are written in English
- Local folder names and operator conventions are not product concepts
- User-facing behavior belongs in `README.md`
- Implementation workflow belongs in `CLAUDE.md`
- Architectural decisions belong in `adr/`

## Contributing: adding a platform

Each platform is a single file. To add support for a new one, touch three places:

**1. `src/peel/platforms/<name>.clj`** — extract messages from the DOM:

```clojure
(ns peel.platforms.<name>
  (:require [clojure.string :as str]
            [peel.text :as text]))

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc "<css-selector>")
       (map (fn [el]
              {:role (if <user-condition?> :user :assistant)
               :text (text/element->md el)}))
       (remove (comp str/blank? :text))
       vec))
```

**2. `src/peel/detect.clj`** — detect the platform by a stable HTML marker:

```clojure
(.selectFirst doc "link[rel=canonical][href*=example.com]") :<name>
```

**3. `src/peel/core.clj`** — register the extractor:

```clojure
;; in ns :require
[peel.platforms.<name> :as <name>]

;; in extractors map
:<name> <name>/extract
```

`text/element->md` normalizes text and renders `<pre><code>` as fenced Markdown blocks (with language detection from `language-*`/`lang-*` classes). Buttons are removed automatically. Remove other UI noise (badges, toolbars) with `(.remove el)` before calling it.
