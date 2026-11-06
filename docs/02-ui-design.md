# 2. UI design

Visual mockups: open [design/ui-mockups.html](design/ui-mockups.html) in a browser.

## Principles

1. **Content first.** The app is a reader. Chrome collapses on scroll, and the document
   takes the full screen.
2. **Never reflow what has structure.** Tables and code scroll sideways as a unit.
3. **Comfortable measure.** Text stays within 720dp on tablets and landscape, centred.
4. **Calm by default.** Reading surfaces use a small, low-saturation palette, and Material
   You wallpaper colors are deliberately not applied so that Sepia and code colors keep
   verified contrast.

## Industry guidelines applied

| Guideline | How it is applied |
|-----------|-------------------|
| Material Design 3 | Compose Material 3 components: top app bars, FAB, modal bottom sheets, snackbars |
| Android edge-to-edge | Content draws behind system bars; insets are handled by `Scaffold` |
| Touch targets | All interactive elements are at least 48dp |
| WCAG 2.1 AA | 4.5:1 text contrast in every theme, enforced by a unit test |
| TalkBack | Headings are marked as headings; icon buttons and checkboxes have labels |
| System font scale | All text sizes are in `sp` and multiply with the in-app text size |
| Adaptive layout | Content column capped at 720dp; works in portrait, landscape and tablet |
| Predictive back | Enabled in the manifest; back closes search first, then leaves the reader |
| Storage Access Framework | Uses the system picker rather than storage permissions |

## Information architecture

```
Home (recents, or empty state)
 ├─ Open file ───────► System file picker ──► Reader
 ├─ Recent item ─────────────────────────────► Reader
 ├─ Sample document (empty state) ───────────► Reader
 └─ Appearance sheet

External app "Open with" / Share ────────────► Reader

Reader
 ├─ Find bar
 ├─ Contents sheet ──► jump to heading
 ├─ Appearance sheet
 ├─ View source toggle
 └─ Share
```

## Screens

### Home

- **Top bar:** app name (large), Appearance action.
- **Recents:** document icon, file name, "opened 2 hours ago", and a remove button.
- **Primary action:** extended FAB, "Open file", in the thumb zone.
- **Empty state:** icon, one-line explanation, Open file and Try the sample buttons.

### Reader

- **Top bar:** back, file name (single line, ellipsized), Find, Contents, overflow menu
  (View source or View formatted, Appearance, Share). Hides on scroll down and returns on
  scroll up.
- **Reading progress:** 2dp bar under the top bar.
- **Loading and error states:** a spinner while parsing; errors state what happened and
  offer one action, such as Choose another file.

### Find bar

Replaces the top bar: text field, `3/12` counter, previous, next, close. All matches are
highlighted; matches in the current block use a stronger color.

### Contents sheet

Modal bottom sheet listing headings, indented by level. Tapping one closes the sheet and
jumps to the heading.

### Appearance sheet

Theme segmented control (System, Light, Dark, Sepia), font segmented control (Sans, Serif),
text size control with a live percentage, and a Load remote images switch. Changes apply
immediately behind the sheet.

## Design tokens

### Reader palette

| Token | Light | Dark | Sepia |
|-------|-------|------|-------|
| Background | `#FBFBF9` | `#141517` | `#F5ECD9` |
| Text | `#1C1C1A` | `#E6E5E1` | `#3B2F20` |
| Muted text | `#5C5C57` | `#A5A6A2` | `#6A5A44` |
| Accent / link | `#2A5B8C` | `#9CC3EA` | `#7A4B1E` |
| Code background | `#F0F0EC` | `#212327` | `#EBDFC6` |
| Divider | `#DDDDD8` | `#34363B` | `#D8CAAC` |
| Find highlight | `#FFE27A` | `#6B5A12` | `#F2CC6B` |
| Current find | `#FFB74D` | `#9A5210` | `#E8A33D` |

### Typography (at 100%)

| Element | Size / line height | Weight |
|---------|--------------------|--------|
| Body | 16 / 26 sp | Regular |
| H1 | 28 / 36 sp | Bold, with divider below |
| H2 | 24 / 32 sp | Bold, with divider below |
| H3 | 20 / 28 sp | SemiBold |
| H4 | 18 / 26 sp | SemiBold |
| H5 | 16 / 24 sp | SemiBold |
| H6 | 14 / 22 sp | SemiBold, muted |
| Code | 14 / 21 sp | Monospace |

### Spacing

Grid is 4dp. Horizontal page padding 20dp, space between blocks 12dp, space above a heading
24dp, list indent 24dp, corner radius 8dp for code and images.

## Markdown element treatment

| Element | Treatment |
|---------|-----------|
| Table | Bordered grid with a tinted header; columns size to content (up to 280dp); scrolls sideways |
| Code block | Tinted card with language label and copy button; scrolls sideways; never wraps |
| Inline code | Monospace on code background |
| Quote | 3dp accent bar on the left, muted text |
| Task list | Read-only checkbox icons with a state description for TalkBack |
| Image | Fits width, rounded; placeholder with alt text when it cannot load |
| Badges | Adjacent images flow in a wrapping row |
| Front matter | Collapsed "Properties" card that expands to key/value pairs |
| Footnotes | Collected at the end under a divider; references are tappable |
