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
- `src/peel/text.clj` — shared `normalize` fn (strips emoji, collapses whitespace)
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
               :text (text/normalize (.text el))}))
       (remove (comp str/blank? :text))
       vec))
```

- Use Jsoup CSS selectors: `.class`, `[attr]`, `[attr=value]`, `tag[attr*=substr]`
- Call `text/normalize` on all text (strips emoji, collapses spaces/newlines)
- Return empty strings filtered out via `(remove (comp str/blank? :text))`
- Remove UI noise (buttons, badges) with `(.remove el)` before calling `.text`

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

Перед запуском автоматически удаляет все `.md`, `.json`, `.edn` из `test/peel/fixtures/` — каждый прогон с чистого листа.

30 тестов, четыре вида:

| Тест | Файл | Фикстура | Работает без реального HTML |
|---|---|---|---|
| `platform-detection` | `<platform>_test.clj` | реальный HTML (gitignored) | пропускается если файла нет |
| `extract-messages` | `<platform>_test.clj` | реальный HTML (gitignored) | пропускается если файла нет |
| `no-ui-noise` | `<platform>_test.clj` | `*-sample.html` (в репо) | всегда |
| `json-is-pretty-printed` | `output_test.clj` | нет (данные в коде) | всегда |

**Структура тестов:**
- `test/peel/<platform>_test.clj` — по одному файлу на платформу
- `test/peel/output_test.clj` — тесты рендеринга (JSON pretty-print и др.)
- `test/peel/helpers.clj` — общие хелперы: `check-platform`, `check-messages`, `check-no-noise`, `with-fixture`
- `test/peel/fixtures/*-sample.html` — синтетические HTML с реальными UI-элементами (кнопки Copy, тулбары, бейджи)

**При добавлении новой платформы** создать `test/peel/<name>_test.clj`:

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

;; если экстрактор вырезает UI-шум:
(deftest no-ui-noise
  (h/check-no-noise (<name>/extract (h/load-doc sample-path))
                    ["Copy" "Download"]))  ; строки которых не должно быть в тексте
```

И добавить namespace в `:requires` задачи `test` в `bb.edn`.

**`no-ui-noise` тест:** обновить `*-sample.html` — добавить в HTML реальные элементы тулбара (кнопки Copy, бейджи статистики), которые экстрактор должен вырезать. Тест проверяет что их текст не попал в вывод.

## Notes on tricky platforms

**DeepSeek** — hashed CSS classes change with frontend updates. The extractor infers the user-bubble class dynamically by comparing class sets across messages. Has a hardcoded fallback.

**Claude** — assistant messages span multiple `.standard-markdown` blocks per turn; they are joined with `\n\n`.

**Google AI** — extracts from Google Search AI Mode (`google.com/search`), not Gemini. Detected separately from `gemini.google.com`.

## Jsoup quick reference

```clojure
(.select doc "css")          ; -> Elements (iterable)
(.selectFirst doc "css")     ; -> Element or nil
(.text el)                   ; visible text, all descendants
(.attr el "name")            ; attribute value
(.hasClass el "cls")         ; boolean
(.remove el)                 ; detach from DOM (mutates)
(.classNames el)             ; Set<String>
```
