---
title: MD Reader sample
author: MD Reader
tags: [markdown, sample]
---

# Welcome to MD Reader

This sample shows how Markdown looks in the app. Open **Contents** from the top bar to jump
between sections, or try [a link to the tables section](#tables).

## Text

You can write **bold**, *italic*, ~~strikethrough~~, and `inline code`. Links open in your
browser, like [CommonMark](https://commonmark.org), and bare URLs work too:
https://github.github.com/gfm/

Here is a footnote reference.[^1] Long-press any text to select and copy it.

> Quotes stand out with an accent bar.
>
> They can span several paragraphs, and hold **formatting**.

## Lists

- Bullet item
- Another item
  - Nested item
    - Deeper item
- Back to the top

1. First step
2. Second step
3. Third step

### Task list

- [x] Render tables properly
- [x] Keep code from wrapping
- [ ] Read this whole sample

## Tables

Wide tables scroll sideways instead of squeezing their columns.

| Feature | Status | Notes on behavior | Applies to | Since |
|:--------|:------:|-------------------|------------|------:|
| Tables | Done | Columns fit their content and scroll as a unit | All themes | 1.0 |
| Code | Done | Long lines scroll, and never wrap | All themes | 1.0 |
| **Bold** in cell | `code` too | [Links](https://commonmark.org) work in cells | All themes | 1.0 |

## Code

```kotlin
// Parsing happens off the main thread
suspend fun load(uri: String): Document = withContext(Dispatchers.Default) {
    val text = source.read(uri)
    parser.parse(text) // This comment makes the line long enough to scroll sideways on a phone
}
```

```python
def greet(name):
    """Say hello."""
    return f"Hello, {name}!"
```

```bash
# Build the app
./gradlew assembleDebug
```

## Images

Remote images load when allowed in Appearance.

![Markdown badge](https://img.shields.io/badge/Markdown-supported-2A5B8C)

An image stored next to the file shows a placeholder:

![Local diagram](images/diagram.png)

---

That's everything. Close this sample and open your own file.

[^1]: Footnotes are collected at the end of the document.
