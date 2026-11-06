# 3. Architecture

## Layers

```
ui/        Compose screens and ReaderViewModel
markdown/  Plain Kotlin: parser, document model, search, highlighter, link policy
data/      File loading and decoding, recents, settings
```

`markdown/` and most of `data/` have no Android or Compose dependencies, so they are tested
on the JVM. Screens receive state and callbacks, and dependencies are wired by hand in
`AppContainer`, which is enough for an app of this size.

## Reading a file

1. A URI arrives from the picker, an "Open with" or Share intent, or the recents list.
2. `ContentDocumentSource` reads the bytes on the IO dispatcher, with a 10 MB limit.
3. `DocumentDecoder` detects the encoding, rejects binary data, and normalizes line endings.
4. `MarkdownParser` turns the text into an `MdDocument` on the Default dispatcher.
5. `ReaderScreen` renders top-level blocks in a `LazyColumn`.

## Decisions

| Decision | Reason |
|----------|--------|
| Native Compose rendering, not a WebView | Untrusted documents can't run script. Gives full control of tables, code and theming, and allows lazy rendering |
| commonmark-java for parsing | Spec-compliant and maintained, with extensions for GFM, footnotes and front matter |
| Own document model between parser and UI | Keeps the parser library out of the UI, and makes conversion testable without a device |
| Storage Access Framework, no storage permission | Least privilege. Picked files keep access through persistable URI permissions |
| Picker accepts `*/*` | Android has no dependable MIME type for `.md`, and a narrower filter greys out real files. The decoder rejects binary files on load |
| Nesting capped at 12 levels | Deeply nested input would otherwise overflow the stack. Deeper content is flattened to text |
| Front matter parser runs only on closed `key:` blocks | The extension otherwise swallows documents that start with a `---` rule |
| Fixed reader palette, no Material You colors | Keeps contrast verifiable, enforced by `ReaderPaletteTest` |

## Security

Documents are untrusted input, so:

- Links open only for `http`, `https` and `mailto`. `javascript:`, `file:`, `content:`,
  `intent:` and others are blocked (`LinkPolicy`).
- Images load only over `http(s)`, so a document can't read local files by path.
- Embedded HTML is never interpreted. It is reduced to its text and images.
- `TestDocsProvider` exists only in debug builds, and isn't exported.

Loading remote images reveals the device's IP address to the image host, as in any browser.
Users can turn this off under Appearance.

## Known limitations

- A single picked file grants access to that file only, so relative images and links to
  sibling files can't resolve. They show a placeholder or a message. See the backlog in
  [requirements](01-requirements.md).
- Files opened through "Open with" may lose access after a restart if the sending app's grant
  isn't persistable. The reader then shows an error that asks to open the file again.
- Find highlights all matches in the current block in the stronger color, rather than a
  single match.
- Selection can't be carried across blocks that have scrolled out of composition.
