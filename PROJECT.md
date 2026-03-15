# Project Metadata

name: peel
status: active
readiness: usable
purpose: Babashka CLI for extracting dialogues from saved AI chat HTML pages into EDN, JSON, and Markdown
owner: TODO

## Run

- `cd peel && bb peel test/peel/fixtures/claude-sample.html --md`
- `cd peel && bb peel test/peel/fixtures/ --md --out=out/`

## Test

- `cd peel && bb test`

## Links

- Repo: TODO
- Docs: ./README.md
- Tasks: ./bb.edn

## Documentation Canon

- `README.md` — user-facing overview and CLI contract
- `CLAUDE.md` — agent/developer implementation guide
- `PROJECT.md` — project metadata and document map
- `adr/` — architectural decisions
- `notes/` — working notes only; not canonical documentation

## Documentation Rules

- Canonical project docs are written in English
- Local operator folder names are not part of the `peel` model
- `README.md` is the source of truth for CLI behavior and supported inputs
- `CLAUDE.md` is the source of truth for implementation workflow
- `notes/` stores exploratory working material only

## External Input Folders

`peel` accepts any external directory that contains saved chat `.html` files.
Folder names are not part of the system model.

Examples:

- `~/Downloads/ai-chats/`
- `~/archive/chat-html/`
- a temporary evaluation corpus
- a personal export folder

Local folder names are operator conventions only and must not appear in canonical project docs as if they were first-class `peel` concepts.

## Notes

- `bb.edn` defines the `peel` and `test` tasks.
