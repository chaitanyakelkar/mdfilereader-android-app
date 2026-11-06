# 1. Requirements

## Problem

Markdown files are hard to read on Android. They typically arrive as plain text with
visible `#` and `|` characters, tables wrap into nonsense, code blocks reflow, and many
file managers cannot open a `.md` file at all because Android has no reliable MIME type
for the extension.

## Goal

Open any Markdown file on the device from anywhere, and read it comfortably, offline,
without granting storage permissions.

## Functional requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1 | Open a file through the system file picker, with no file type greyed out | Must |
| FR-2 | Appear in "Open with" for `.md` and `.markdown` files from file managers and other apps, including files reported as `text/plain` or `application/octet-stream` | Must |
| FR-3 | Render CommonMark and GFM: headings, emphasis, links, lists, nested lists, quotes, code, rules, tables, task lists, strikethrough, autolinks | Must |
| FR-4 | Render footnotes, YAML front matter, and common inline HTML found in READMEs (`<br>`, `<img>`, comments) | Should |
| FR-5 | Decode UTF-8, UTF-16, and legacy Windows-1252 files correctly; normalize CRLF line endings | Must |
| FR-6 | Reject binary and oversized files with a clear message rather than showing garbage | Must |
| FR-7 | Wide tables and long code lines scroll horizontally and never reflow | Must |
| FR-8 | Table of contents, with jump to heading; in-document `#anchor` links scroll to the heading | Must |
| FR-9 | Find in document with match count and next/previous | Should |
| FR-10 | Toggle between rendered view and raw source | Should |
| FR-11 | Reader appearance: System, Light, Dark, Sepia themes; sans or serif font; text size 80% to 160%; persisted | Must |
| FR-12 | Recent files list that restores scroll position; individual items can be removed | Should |
| FR-13 | Copy a code block; select and copy text; share the file | Should |
| FR-14 | Syntax highlighting for common languages in fenced code blocks | Could |
| FR-15 | Load remote images (including SVG badges), with a setting to turn this off | Should |

## Non-functional requirements

| ID | Requirement |
|----|-------------|
| NFR-1 | **Privacy:** no storage permission, no analytics. The only network use is remote images, which the user can disable |
| NFR-2 | **Security:** untrusted documents cannot run code. No WebView; only `http`, `https` and `mailto` links open; images load only over `http(s)` |
| NFR-3 | **Robustness:** a malformed or adversarial file (deep nesting, huge size) must not crash the app |
| NFR-4 | **Performance:** parsing runs off the main thread; long documents render lazily |
| NFR-5 | **Accessibility:** WCAG AA contrast (4.5:1) in every theme; 48dp touch targets; headings exposed to TalkBack; text scales with system font size |
| NFR-6 | **Compatibility:** Android 8.0 (API 26) and up; phones and tablets; portrait and landscape |

## Out of scope for v1

- Editing files
- Relative images and links to sibling files. A single picked file grants access to that
  file only; supporting this needs an "open folder" feature (see backlog)
- Mermaid diagrams and LaTeX math (shown as code)
- PDF export

## Backlog

Open folder with file tree and relative links, Mermaid and math rendering, home screen
search across recents.
