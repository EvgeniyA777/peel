# Project Notes

Working-note storage for the project.

## Canon

- Folder: `notes/`
- File name: `YYYY-MM-DD-HHmm-UUID.md`
- The timestamp in the file name must match `created_at` in frontmatter (`YYYY-MM-DDTHH:MM`)
- Notes are written in English unless a task explicitly requires another language
- Notes are working artifacts, not canonical product documentation

## Quick Start

```bash
./scripts/new-note.sh "short-topic" "en"
./scripts/validate-notes.sh
```
