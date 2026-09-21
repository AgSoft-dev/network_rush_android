---
name: terminus-design
description: Network Rush's "Terminus" visual identity (tram signage) for the Android Compose app. Use whenever you create or restyle a screen, dialog, button, tile, icon or any UI in this repo, or write UI copy, so new work matches the existing look (tokens, fonts, plates, line pills, sun-yellow CTA).
---

# Terminus design system (TriviaMap)

The app looks like **tram signage**: ink-blue station night, white plates, solid round line pills in the official CTS colours, one sun-yellow action colour. Tone: frank, cheerful, a little cheeky ("Belle course", "Terminus"), never kitschy. The Alsatian stork is a discreet mascot idea (level emblem / badge pictogram), not clip-art.

Source of truth: `app/src/main/java/com/triviamap/presentation/common/Theme.kt`. **Use its tokens and helpers; never hard-code hex colours or fonts in screens.** Reference screens: `HomeScreen.kt` (hero plate, ticket, dialogs), `SprintScreen.kt` (tiles), `ResultsScreen.kt` and `StatsScreen.kt` (plates, XP card), `SettingsScreen.kt` (panels).

## Tokens (Theme.kt)
| Token | Hex | Use |
|---|---|---|
| `Background` | #0F1A3C | Screen background ("station night", never pure black) |
| `Surface` / `SurfaceHigh` | #18275A / #1D2F6A | Dark panels |
| `Border` | #2B3D7E | Panel outlines (2 dp), inactive tracks |
| `Plate` | #FFFFFF | White signage plate: tiles, hero card, level/XP card, dialog options |
| `PlateEdge` | #B7C0DD | Plate "thickness" (4 dp shadow under a tile) |
| `Ticket` | #FFF6E0 | Cream ticket: daily validated, support card, share recap |
| `Ink` / `InkMed` | #0F1A3C / #3D4670 | Text on plates, tickets and sun buttons |
| `Sun` / `SunEdge` | #FFC83D / #C8901B | **Main action, combo, timer only.** (`Primary` and `Accent` alias `Sun`) |
| `OnSurface` / `OnSurfaceMed` | #FFFFFF / #B9C3E6 | Text on dark |
| `Success` / `Error` | #2ECC71 / #FF5A5F | Feedback only |
| `LineA`..`LineF` | official CTS | Line colours; real ones come from the data file (`line.color`) |

Line colours are kept as is for instant recognition (A red #E10D19, B cyan #009EE0, C orange #F29400, D green #009933, E violet #9085BA, F lime #97BF0D, G yellow #F6C900). Never show a line by colour alone: always with its letter.

## Typography
- `DisplayFont` (Bricolage Grotesque 500/700/800): headlines, scores, tile names, buttons, level numbers. Use `fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold`.
- Body: DM Sans (default via `MaterialTheme.typography`). Bundled in `res/font` (SIL OFL), no network needed.
- Small caps-style labels: `letterSpacing` 1-2 sp, `OnSurfaceMed`.

## Components and recipes
- **Line pill**: `LinePill(letter, Color(line.color), size)`. Round, letter in `onLineColor()` (ink or white, whichever contrasts best). Use it everywhere a line is named (headers, direction hints, mastery rows).
- **Plate** (tile / hero / card on dark): `RoundedCornerShape(18-26.dp)`, `Plate` background, `Ink` text, optional `PlateEdge` 4 dp offset behind (see `drawBehind` in `SprintScreen.kt`). Min height 64 dp for draggable tiles, handle = `Ink` at 45% alpha.
- **Dragging tile**: keep plate white, add a 3 dp `Sun` outline, rotate about -2 degrees, elevation. Checked tiles: 3 dp `Success`/`Error` border.
- **Primary CTA**: `Sun` background, `Ink` text, radius 18 dp, `DisplayFont` ExtraBold 18-24 sp, no elevation. One per screen region; secondary actions are `Surface` panels or outlined buttons in `OnSurface`.
- **Dark panel**: `Surface` + 2 dp `Border`, radius 18 dp, white text (Settings rows).
- **Ticket**: `Ticket` background, `Ink`/`InkMed` text; for a daily done/validated recap, support card and share card.
- **Dialogs**: `AlertDialog(shape = RoundedCornerShape(24.dp), backgroundColor = Surface)`, title in `DisplayFont`, options as `Plate` rows with a colour dot.
- **Progress bars**: bar `LineB`, track `PlateEdge` at 50% on plates (or `Surface`/`Border` on dark); timer bar `Sun`, turning `Error` under 10 s.
- **Switches**: checked thumb `Sun`, track `SunEdge`; unchecked thumb `OnSurfaceMed`, track `Border`.
- **Ads**: banner only at the bottom of Home (reserve its space, `navigationBarsPadding`); never on gameplay screens.

## Rules
1. Sun yellow is for **the** action, combo and timer. Do not use it as decoration, and do not put white text on it (ink only).
2. Text on white/cream is always `Ink`/`InkMed`; text on navy is `OnSurface`/`OnSurfaceMed`. Keep WCAG AA (4.5:1) for body text.
3. Big touch targets (tiles >= 64 dp, buttons >= 52 dp), one-handed and left-handed friendly (the drag handle side follows the left-handed setting).
4. Gameplay screens stay uncluttered: no ads, no decorative motion competing with the timer.
5. Copy is English in the app for now, short, imperative and lightly cheeky.
6. Do not add new colours or fonts without adding a token in `Theme.kt` (and updating this file).

## App icon
Adaptive icon: navy background (#101B3F with a lighter #16255A disc), foreground = an "S" drawn like a tram line in three segments (B cyan top, A red middle, D green bottom) with three white stations (dark 3-unit outline), all inside the 66 dp safe zone; monochrome variant = the "S" only. Files: `res/drawable/ic_launcher_{background,foreground,monochrome}.xml`, `res/mipmap-anydpi/ic_launcher*.xml`.

## Verifying UI work
- Build and run on the emulator: `./gradlew installDebug` (Android Studio JBR as `JAVA_HOME`), then `adb exec-out screencap -p` and look at the result (Home, Sprint tile list, Results, Progress, Settings, dialogs).
- Check both an easy state and the Classify 3-column state when touching tiles (Dev panel on Home in debug builds: "DEV · SPRINT VARIANTS").
- Run `./gradlew testDebugUnitTest lintDebug` (separately from `assembleRelease`, they can collide).

## Known gaps / ideas from the design exploration
Weekly "validation card" with punch holes for the streak, stork as level/badge emblem, line-filter chips on Home, signature sounds/haptics, Trace (dev) screen still on the old look, tinted Classify tiles vs light line colours (F, G) to check for contrast. See `TODO.md` ("Identité visuelle Terminus"). The full 3-identity HTML mockups (Terminus, Grès, Noctambule) live in `design-explorations/`, which is **not tracked by git**, so it may be absent; do not rely on it.
The app is named "Network Rush" (Play title "Network Rush: Tram Strasbourg"); the code base and `applicationId` keep the `triviamap` name (see `TODO.md`).
