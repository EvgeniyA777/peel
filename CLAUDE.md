# peel — AI agent guide

Babashka CLI that extracts chat dialogues from saved AI chat web pages into EDN, JSON, and Markdown.

## Running

```bash
bb peel "chat.html" --md                        # stdout
bb peel "chat.html" --json                      # stdout, JSON
bb peel "chat.html"                             # stdout, EDN (default)
bb peel chats/ --md --out=out/                  # directory → directory
bb peel "chat.html" --md --out=/path/to/kb/     # file → directory
```

Requires [babashka](https://github.com/babashka/babashka).

## Documentation canon

- `README.md` — user-facing CLI contract and supported platforms
- `CLAUDE.md` — agent/developer operating guide
- `PROJECT.md` — project metadata and document map
- `adr/` — architecture decisions
- `notes/` — working notes only, not canonical product docs

## Documentation rules

- Canonical project docs are written in English
- Local operator folder names are not product concepts
- Keep user-facing behavior in `README.md`
- Keep implementation workflow in `CLAUDE.md`
- Keep architectural rationale in `adr/`

## External input folders

`peel` works on any external folder containing saved chat `.html` files.
Do not treat local directory names as product concepts.

Examples of valid inputs:

- `~/Downloads/ai-chats/`
- `~/archive/chat-html/`
- a temporary evaluation corpus
- a personal export folder

Local folder names are operator conventions only and should not appear in canonical `peel` documentation as if they were part of the system.

## Architecture

```
HTML file
  └─ core/parse-html        parse with Jsoup, strip <script>/<style>/<svg>
       ├─ detect/platform   identify platform via CSS selectors on <meta>/<link>
       ├─ extractors[platform]/extract   pull messages from DOM → [{:role :user/:assistant, :text "..."}]
       └─ output/render     serialize to :edn | :json | :md
```

Key files:
- `src/peel/core.clj` — entry point, `extractors` map, file/dir handling
- `src/peel/detect.clj` — platform detection via Jsoup CSS selectors
- `src/peel/output.clj` — EDN / JSON / Markdown rendering
- `src/peel/text.clj` — HTML/text normalization and DOM-to-Markdown conversion helpers
- `src/peel/platforms/<name>.clj` — one file per platform

## Data structures

Each extractor returns a vector of message maps:

```clojure
[{:role :user      :text "..."}
 {:role :assistant :text "..."}
 ...]
```

`parse-html` wraps this in:

```clojure
{:path     "path/to/file.html"
 :title    "Page title from <title>"
 :platform :claude        ; keyword, :unknown if undetected
 :messages [...]}
```

## Adding a new platform extractor

Three files to touch, always the same pattern:

### 1. `src/peel/platforms/<name>.clj`

```clojure
(ns peel.platforms.<name>
  (:require [clojure.string :as str]
            [peel.text :as text]))

(defn extract [^org.jsoup.nodes.Document doc]
  (->> (.select doc "<css-selector-for-messages>")
       (map (fn [el]
              {:role (if <user-condition?> :user :assistant)
               :text (text/element->md el)}))
       (remove (comp str/blank? :text))
       vec))
```

- Use Jsoup CSS selectors: `.class`, `[attr]`, `[attr=value]`, `tag[attr*=substr]`
- Call `text/element->md` on extracted message elements
- Return empty strings filtered out via `(remove (comp str/blank? :text))`
- Remove UI noise (badges, toolbars) with `(.remove el)` before calling `element->md`; buttons are removed automatically inside `element->md`

### 2. `src/peel/detect.clj`

Add a `cond` branch before the `:else`:

```clojure
(.selectFirst doc "link[rel=canonical][href*=example.com]") :<name>
```

Prefer `link[rel=canonical]` — it's stable. Fall back to `meta` attributes if no canonical link exists (see DeepSeek: uses `meta[name=commit-id]`).

### 3. `src/peel/core.clj`

Add to the `extractors` map and the `ns` `:require`:

```clojure
;; in ns :require
[peel.platforms.<name> :as <name>]

;; in extractors map
:<name> <name>/extract
```

## Tests

```bash
bb test
```

Automatically deletes all `.md`, `.json`, `.edn` from `test/peel/fixtures/` before running — each run starts clean.

The test suite has four kinds of checks:

| Test | File | Fixture | Runs without real HTML |
|---|---|---|---|
| `platform-detection` | `<platform>_test.clj` | real HTML (gitignored) | skipped if file missing |
| `extract-messages` | `<platform>_test.clj` | real HTML (gitignored) | skipped if file missing |
| `no-ui-noise` | `<platform>_test.clj` | `*-sample.html` (in repo) | always |
| `json-is-pretty-printed` | `output_test.clj` | none (inline data) | always |

**Test structure:**
- `test/peel/<platform>_test.clj` — one file per platform
- `test/peel/output_test.clj` — rendering tests (JSON pretty-print etc.)
- `test/peel/helpers.clj` — shared helpers: `check-platform`, `check-messages`, `check-no-noise`, `with-fixture`
- `test/peel/fixtures/*-sample.html` — synthetic HTML with real UI elements (Copy buttons, toolbars, badges)

**When adding a new platform** create `test/peel/<name>_test.clj`:

```clojure
(ns peel.<name>-test
  (:require [clojure.test :refer [deftest]]
            [peel.helpers :as h]
            [peel.platforms.<name> :as <name>]))

(def ^:private fixture-path "test/peel/fixtures/<name>-real.html")
(def ^:private sample-path  "test/peel/fixtures/<name>-sample.html")

(deftest platform-detection
  (h/with-fixture fixture-path
    #(h/check-platform (h/load-doc fixture-path) :<name>)))

(deftest extract-messages
  (h/with-fixture fixture-path
    #(h/check-messages (<name>/extract (h/load-doc fixture-path))
                       4 [:user :assistant :user :assistant])))

;; if the extractor removes UI noise:
(deftest no-ui-noise
  (h/check-no-noise (<name>/extract (h/load-doc sample-path))
                    ["Copy" "Download"]))  ; strings that must not appear in output
```

Also add the namespace to the `:requires` of the `test` task in `bb.edn`.

**`no-ui-noise` test:** update `*-sample.html` — add real toolbar elements (Copy buttons, stats badges) that the extractor should remove. The test verifies their text does not appear in the output.

## Notes on tricky platforms

**DeepSeek** — hashed CSS classes change with frontend updates. The extractor infers the user-bubble class dynamically by comparing class sets across messages. Has a hardcoded fallback.

**Claude** — assistant messages span multiple `.standard-markdown` blocks per turn; they are joined with `\n\n`.

**Google AI** — extracts from Google Search AI Mode (`google.com/search`), not Gemini. Detected separately from `gemini.google.com`.

**NotebookLM** — uses Angular custom elements (`<chat-message>`). Pages without a dialogue (summary-only) have no `<chat-message>` elements and return `[]` naturally. Citation spans (`<span aria-label="N: Source Title">`) must be removed before text extraction. After removal, space-before-punctuation artifacts are fixed with `str/replace #" +([.!?,;:])" "$1"`. The Pin/toolbar is inside `mat-card-actions`.

## MCP: semantic-code-indexing policy

When the `semantic-code-indexing` MCP server is available, always use it:

1. **Session start** — `create_index` → `repo_map` for project orientation. Do not manually read files to understand project structure.
2. **Before refactoring** a public function — `impact_analysis` to find all call sites and downstream effects.
3. **Understanding data flow** between modules — `resolve_context` on the function/var in question.
4. **Reviewing API surface** — `skeletons` instead of reading full source files.
5. **Deep exploration** — `expand_context` / `fetch_context_detail` instead of Agent(Explore).

Load all MCP tool schemas at session start (`ToolSearch` with `+semantic-code-indexing`). Do not fall back to manual Read/Grep/Agent(Explore) for tasks these tools cover.

## Jsoup quick reference

```clojure
(.select doc "css")          ; -> Elements (iterable)
(.selectFirst doc "css")     ; -> Element or nil
(.text el)                   ; visible text, all descendants (normalizes whitespace)
(.wholeText el)              ; visible text, whitespace preserved (use for code content)
(.attr el "name")            ; attribute value
(.hasClass el "cls")         ; boolean
(.remove el)                 ; detach from DOM (mutates)
(.classNames el)             ; Set<String>
```
