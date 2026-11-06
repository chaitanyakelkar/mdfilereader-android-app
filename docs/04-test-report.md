# 4. Test report

Test date: 2026-11-06. Device: Pixel 4 emulator, Android 13 (API 33).

## Summary

| Level | What it covers | Result |
|-------|----------------|--------|
| Unit (JVM) | Parser, decoder, search, highlighter, link policy, recents, contrast, ViewModel | 76 of 76 passed |
| Instrumented (emulator) | Screens and flows, intent matching, content provider loading | 18 of 18 passed |
| Manual end to end | Real system picker, restart, rotation, gestures, release build | Passed, after the fixes below |
| Android Lint | Whole project | 0 errors, 9 accepted warnings |

Run the automated tests with:

```bash
./gradlew testDebugUnitTest connectedDebugAndroidTest
```

## Requirements coverage

| Requirement | Verified by |
|-------------|-------------|
| FR-1 Open from picker | Manual, with a file in Downloads |
| FR-2 Open with | `ExternalDocumentTest` (intent matching, launch from a VIEW intent) |
| FR-3, FR-4 Rendering | `MarkdownParserTest`, `ReaderFlowTest`, visual review |
| FR-5 Encodings | `DocumentDecoderTest`, `ExternalDocumentTest`, manual with a BOM and CRLF file |
| FR-6 Bad files | `DocumentDecoderTest`, `ExternalDocumentTest` |
| FR-7 Sideways scrolling | Manual swipe on a table; code reviewed in screenshots |
| FR-8 Contents and anchors | `ReaderFlowTest`, manual tap on an anchor link |
| FR-9 Find | `ReaderViewModelTest`, `ReaderFlowTest` |
| FR-10 Source view | `ReaderViewModelTest`, `ReaderFlowTest` |
| FR-11 Appearance | `ReaderFlowTest`, `ReaderPaletteTest`, visual review of Dark |
| FR-12 Recents | `RecentsCodecTest`, `ReaderViewModelTest`, manual reopen after force stop |
| FR-13 Select and copy | Manual: long press shows Copy, and Share opens the system sheet. The copy code button ran without a crash, but **clipboard content wasn't checked** |
| FR-14 Highlighting | `CodeHighlighterTest`, visual review |
| FR-15 Remote images | Manual: an SVG badge loads, and with the switch off it shows a placeholder |
| NFR-2 Security | `LinkPolicyTest`. Blocked links weren't tapped on a device |
| NFR-3 Robustness | `MarkdownParserTest` (20,000 nested quotes), oversized and binary file tests |
| NFR-5 Accessibility | `ReaderPaletteTest` for contrast. **Not tested with TalkBack** |

## Defects found and fixed

| Severity | Defect | Found by | Fix |
|----------|--------|----------|-----|
| Critical | Every file from the real picker failed with "Access has expired". The URI was decoded twice, which corrupted it | Manual test | Removed the second decode, added a regression test |
| High | A document starting with a `---` rule lost its content, swallowed by the front matter parser | Unit test | Front matter parsing runs only on closed `key:` blocks |
| Medium | Empty state buttons were unreachable in landscape | Manual test | Message screens now scroll |
| Medium | Dark theme's current find color failed contrast at 3.06:1 | Unit test | Changed to `#9A5210` |
| Low | Small images and badges stretched to full width, then blurred | Visual review | Images show at natural size and reload at display size |
| Low | Reading progress bar was thicker than designed | Visual review | Set to 2dp |

The critical defect passed all automated tests, because the test URIs had no escaped
characters. The regression test `encodedUriReachesTheLoaderUnchanged` now covers it.

## Release build

R8 shrinking reduces the APK to 1.9 MB. A release build, signed with the debug key for this
test only, was smoke tested: sample rendering, an SVG badge over the network, and the real
picker all worked, with no crashes.

## Build alignment

The build now matches the project's sibling WhatsApp app: AGP 9.4.1, Kotlin 2.2.10, Compose BOM
2026.02.01, Java 11 and `minSdk` 24. Coil is held at 3.4.0, because later versions need Kotlin
2.3 or newer at compile time. All unit and instrumented tests were rerun after this change, on
API 33. The unsigned release APK can't be installed by design (`INSTALL_PARSE_FAILED_NO_CERTIFICATES`).

## Not covered

- Physical devices, and Android versions other than 13. The minimum supported version is
  Android 7.0
- Tablets, except for the landscape layout on a phone
- TalkBack
- Performance on long documents. The emulator ran with software rendering and under memory
  pressure, so frame timing there isn't meaningful
- Files from cloud providers such as Google Drive

## Accepted lint warnings

- `enableOnBackInvokedCallback` and `dataExtractionRules` are ignored on older Android, as intended
- Newer versions of AGP, Kotlin, the Compose BOM and Coil exist: versions match the sibling project on purpose
- A newer Gradle (9.8.2) is available: the wrapper uses 9.6.0, the same as the sibling project
