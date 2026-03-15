# ADR 001: Preserve Structure In HTML-to-Markdown Conversion

## Status

Proposed

## Date

2026-03-15

## Context

`peel` extracts chat messages from saved HTML pages and emits EDN, JSON, and Markdown.
The current Markdown path is implemented in `src/peel/text.clj` via `text/element->md`.

Today `element->md`:

1. clones the selected message DOM fragment
2. removes some UI elements such as `button`
3. extracts `<pre><code>` blocks into fenced Markdown placeholders
4. converts the remaining fragment to plain text with `(.text clone)`
5. runs `normalize` on that flattened text
6. reinserts fenced code blocks into the flattened output

This destroys structural HTML semantics before Markdown conversion happens.
As a result:

- paragraphs collapse
- `<br>` semantics are lost
- ordered and unordered lists lose numbering and nesting
- block quotes lose shape
- heading boundaries blur into plain text
- adjacent blocks are merged in ways that are hard to recover later

The current implementation is acceptable for plain text plus code blocks, but not for high-fidelity Markdown suitable for knowledge-base ingestion or archival.

## Problem Statement

`peel` needs Markdown output that preserves the meaningful structure of saved chat HTML:

- paragraph breaks
- line breaks where the source UI intended them
- ordered and unordered lists
- nested list indentation
- headings
- block quotes
- fenced code blocks
- basic emphasis when it exists in the source DOM

The system must do this without keeping UI chrome such as copy buttons, badges, toolbars, code headers, citation widgets, and similar noise.

## Decision Drivers

- Preserve structural semantics from the source DOM as long as possible
- Reuse a mature HTML-to-Markdown engine instead of growing an ad hoc renderer
- Stay JVM-native so the solution fits a Clojure/Babashka project cleanly
- Keep extractor-specific DOM cleanup logic in the platform files
- Avoid external binary dependencies for the canonical path
- Retain control over code-fence handling and cleanup rules where needed

## Options Considered

### 1. Keep current approach and add more regex/post-processing

Rejected.

Once `(.text clone)` is called, the block structure is already gone.
Regex repair after flattening cannot reliably reconstruct ordered lists, nested lists, paragraphs, or block quotes.

### 2. Use Pandoc as the canonical converter

Rejected as the canonical approach.

Pandoc is strong, but it introduces an external runtime dependency and operational variability.
It is still useful as a benchmark or fallback tool during evaluation.

### 3. Use `copy-down` (Java port of Turndown)

Rejected for now.

It is conceptually attractive, but currently carries more compatibility risk around Jsoup/library drift than is reasonable for `peel`.

### 4. Use `flexmark-java` with `flexmark-html2md-converter`

Accepted.

This keeps the conversion path on the JVM, provides a mature HTML-to-Markdown engine, and is the best fit for a Babashka/Clojure codebase that already uses Jsoup and Java interop.

## Decision

`peel` will replace the current text-flattening Markdown path with a two-stage structured pipeline:

1. platform extractors will continue to select the correct message DOM fragment and remove platform-specific UI noise
2. the cleaned DOM fragment will be handed to a real HTML-to-Markdown converter without first converting the fragment to plain text

The canonical converter will be `flexmark-java` plus `flexmark-html2md-converter`.

The key invariant is:

> `peel` must not flatten a message DOM fragment to plain text before Markdown conversion if Markdown fidelity is desired.

## Target Architecture

### Current

`Element -> clone -> remove some nodes -> .text -> normalize -> splice code fences`

### Target

`Element -> clone -> remove only UI noise -> preserve structural HTML -> HTML-to-Markdown converter -> light Markdown post-processing`

## Scope

In scope:

- replace the implementation behind `text/element->md`
- preserve paragraphs, lists, line breaks, block quotes, and headings
- keep or improve fenced code block rendering
- add regression tests for structural Markdown output
- allow extractor-specific DOM cleanup before conversion

Out of scope:

- perfect round-trip fidelity for every proprietary chat UI
- image extraction and image-to-Markdown rewriting
- table perfection in every platform on the first pass
- changing EDN/JSON output contracts

## Planned Changes

### 1. Introduce a structured HTML-to-Markdown adapter

Add a new adapter layer in `src/peel/text.clj` or a dedicated namespace such as `src/peel/markdown.clj` that:

- accepts a Jsoup `Element`
- clones it
- removes generic UI noise only
- serializes the cleaned fragment back to HTML
- runs the fragment through `flexmark-html2md-converter`
- returns Markdown instead of flattened plain text

This adapter becomes the canonical implementation for Markdown extraction.

### 2. Separate generic cleanup from platform cleanup

Keep two classes of cleanup rules:

- generic cleanup in shared code
  - `button`
  - obvious copy/download controls
  - duplicated toolbar wrappers that are safe across platforms
- platform cleanup in `src/peel/platforms/*.clj`
  - provider-specific badges
  - code block headers
  - citation widgets
  - pinned banners
  - footer action rows

The rule is:

Remove UI chrome, not semantic content containers.

### 3. Preserve code block behavior deliberately

Before migration, verify whether `flexmark-html2md-converter` renders code blocks correctly on the saved chat HTML samples.

If its default output is good enough, use it directly.
If not, keep a small preprocessing layer that normalizes `<pre><code>` blocks before conversion rather than converting the whole message to plain text.

Code blocks remain a first-class acceptance criterion.

### 4. Keep `normalize` out of the structural path

`normalize` must no longer run on the whole pre-conversion fragment.

If normalization remains useful, it should be applied only in narrowly scoped places such as:

- trimming converter edge artifacts
- collapsing pathological blank-run output after conversion
- platform-specific punctuation spacing cleanup

It must not erase structural boundaries.

### 5. Add structural regression tests

Expand tests with fixtures and assertions for:

- paragraphs remain separated
- ordered list numbering survives
- unordered lists remain bullets
- nested lists retain depth
- `<br>` maps to intended line breaks
- block quotes remain block quotes
- code fences still render correctly
- UI noise is still absent

Prefer fixture-driven tests over synthetic string comparisons where possible.

### 6. Evaluate on a real external HTML corpus before finalizing

Use a real saved-HTML corpus in an external evaluation folder as the practical acceptance set.

Do not finalize the migration based only on synthetic fixtures.
The real export corpus should drive final cleanup rules and converter settings.

## Implementation Plan

### Phase 1. Dependency and spike

- add `flexmark-java` and `flexmark-html2md-converter`
- create a spike adapter that converts a cleaned Jsoup fragment to Markdown
- run it on several real saved HTML samples from different providers
- compare output with current `peel`

Exit criteria:

- lists, paragraphs, and line breaks are visibly better than current output on at least Claude, ChatGPT, Gemini, and DeepSeek samples

### Phase 2. Replace the shared Markdown path

- swap `text/element->md` to the structured converter path
- keep platform extractor signatures unchanged
- preserve code fence behavior or explicitly replace it with tested converter behavior

Exit criteria:

- existing tests pass
- Markdown output improves on representative fixtures

### Phase 3. Platform cleanup refinement

- audit each platform extractor
- move only platform-specific UI removals into extractor code
- avoid deleting semantic wrappers used by the converter

Exit criteria:

- no major UI-noise regressions
- no newly collapsed list/paragraph structures from over-aggressive cleanup

### Phase 4. Regression suite hardening

- add fixtures for broken cases seen in the real corpus
- add assertions for lists, block quotes, blank lines, and code blocks
- document known converter limitations if any remain

Exit criteria:

- test suite covers the structural failures that motivated the migration

## Consequences

### Positive

- Markdown becomes structurally meaningful instead of mostly flattened text
- less custom rendering logic to maintain in `peel`
- easier extension as more platforms are added
- better downstream ingestion quality for notes, KBs, and chunking pipelines

### Negative

- new dependency surface
- converter tuning may be needed for some providers
- some platform extractors may need cleanup adjustments to avoid removing useful structure

## Risks

- some provider DOMs may use deeply nested `div` structures where semantic intent is not explicit
- converter defaults may still need configuration for line-break policy
- citation widgets and inline controls may leak into Markdown if cleanup is incomplete
- code-block headers may be duplicated unless removed before conversion

## Acceptance Criteria

The migration is complete when:

- `peel --md` preserves paragraphs, lists, and code blocks on representative saved HTML chats
- Markdown from real samples is clearly better than the current implementation
- UI chrome remains excluded
- no extractor API redesign is required
- EDN and JSON output remain unchanged

## Follow-Up Work

- decide whether the adapter lives in `peel.text` or a new `peel.markdown` namespace
- benchmark `flexmark` output against `pandoc` and `Turndown` on the same sample set
- document any converter options chosen as project defaults
- add a small corpus of “known bad before / good after” fixtures

## References

- `src/peel/text.clj`
- `src/peel/platforms/*.clj`
- external saved HTML corpora used for evaluation
- `flexmark-java` and `flexmark-html2md-converter`
