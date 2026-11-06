<p align="center">
  <img src="docs/assets/app-icon.png" width="128" alt="MD Reader app icon">
</p>

# MD Reader

An Android app for reading Markdown files properly: tables that stay tables, code that
doesn't reflow, and files that open from anywhere. No storage permission, no analytics, and
no WebView.

## Features

- Opens `.md` and `.markdown` files from the system picker, and from "Open with" and Share in
  other apps
- CommonMark and GFM: tables, task lists, strikethrough, autolinks, footnotes, front matter
- Handles UTF-8, UTF-16 and Windows-1252 files, byte order marks, and CRLF line endings
- Table of contents, find in document, and a raw source view
- System, Light, Dark and Sepia themes, sans or serif font, text size 80% to 160%
- Recent files that reopen where you left off
- Syntax highlighting, code copy, text selection, and remote images (can be turned off)

## Build and test

Needs JDK 17 or newer and the Android SDK with API 37. Android Studio can open the project
directly.

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew connectedDebugAndroidTest
```

The last command needs a running emulator or a connected device.

## Project documents

1. [Requirements](docs/01-requirements.md)
2. [UI design](docs/02-ui-design.md) and [mockups](docs/design/ui-mockups.html)
3. [Architecture](docs/03-architecture.md)
4. [Test report](docs/04-test-report.md)

## Stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, DataStore,
[commonmark-java](https://github.com/commonmark/commonmark-java) and
[Coil](https://github.com/coil-kt/coil).
