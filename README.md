# Network Rush — Tram Strasbourg

Android game (Kotlin, Jetpack Compose) to learn the Strasbourg tram network.

> **Scope:** Strasbourg is the pilot city for now. Support for other cities is planned (see TODO.md §5, multi-city abstraction) but not started: the data loader and the UI are still Strasbourg-specific.

**Station Sprint** is the main mode: reorder the stations of a line (or sort stations between two lines and their shared hub) against a clock. Correct answers earn time and points, wrong answers and skips cost time, combos multiply the score.

**Trace Network** (redraw the network from memory) is parked: its button is only shown in debug builds while its scoring is reworked. See [TODO.md](TODO.md).

## Architecture

```
app/src/main/java/com/triviamap/
├── data/
│   ├── local/        Room (game results + per-station stats, schema v3, migrations, exported to app/schemas/)
│   ├── model/        GeoJsonParser — parses assets/strasbourg_stations.json
│   └── repository/   TramLineRepositoryImpl (Loading/Loaded/Error), GameResultRepositoryImpl, UserPreferencesRepositoryImpl (DataStore)
├── di/               Hilt modules (DB, repositories, Random / Clock / TimeSource)
├── domain/
│   ├── model/        TramNetwork (GeoPoint, Station, TramLine), GameMode, GameResult, StreakCalculator
│   ├── progress/     StationStat, Progression (XP/levels), Badges, ProgressStats, ShareText, RunSummary
│   ├── repository/   Interfaces
│   └── sprint/       ChallengeGenerator, Challenge, SprintRules, DailyChallenge — pure Kotlin, unit-tested
├── presentation/     Compose screens + ViewModels (home, gameplay, results, progress, settings)
└── util/             GeometryEngine, ScoringEngine (Trace), TimeSource
```

Rules and generation live in `domain/sprint` with injectable `Random` and `TimeSource`, so they can be tested deterministically. ViewModels only orchestrate state, timers and persistence.

## Data

`app/src/main/assets/strasbourg_stations.json`: per line, an ordered station list (`id`, `name`, `x`, `y`) and a schematic polyline. Coordinates are **diagram coordinates**, not lon/lat. A station id shared by several lines is an interchange hub (`Station.lines` is derived from that at load time). `DatasetTest` checks the file's invariants.

Data provenance and licence still need to be documented (see TODO.md §5).

## Build & test

Gradle 9.7 + AGP 9.4 + Kotlin 2.3, compileSdk 37 / targetSdk 35. `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to **JDK 21**; Gradle downloads it automatically (foojay resolver), whatever JDK your shell or IDE uses. In Android Studio, set *Settings → Build Tools → Gradle → Gradle JDK* to **Gradle Daemon JVM** (or any JDK 17–24).

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # debug APK (includes the Trace dev button)
./gradlew lintDebug
```

CI (`.github/workflows/ci.yml`) runs tests, lint and a debug build.

## Game design (Sprint)

- **Challenges:** reorder the stations of a line, sort stations into two lines + shared hub columns (then order each column), or rapid-fire 3 tiles. Direction is shown as "line → terminus".
- **Clock:** 60 / 45 / 30 s (easy / medium / hard). A correct answer gives time proportional to the puzzle size, decaying with the level down to a floor, so every run ends. A wrong answer or a skip (3 per run) costs time; almost-right answers cost less.
- **Score:** `(500 + 50·level) × (2 if sorting) × speed factor (1–2, relative to puzzle size) × combo factor (1–3)`.
- **Difficulty** also changes content: hard mixes reverse direction and non-contiguous station sets early.
- **Spaced repetition:** stations you miss (or never saw) are drawn more often; a station is *mastered* after 3 correct placements in a row.
- **Daily challenge:** same puzzles for everybody (seeded by day), one attempt per day, shareable Wordle-style recap.
- **Progression:** XP, levels, badges, per-line knowledge and most-missed stations on the Progress screen.
- Rules live in `domain/sprint/SprintRules.kt`; balance is checked by `SprintEconomyTest`.

## Visual identity

"Terminus" (tram signage): ink-blue station night, white plates for tiles and the main card, solid round line pills in the official CTS colours, one sun-yellow action colour. Bricolage Grotesque (headlines, plates, buttons) and DM Sans (body), bundled in `res/font` (SIL OFL). Tokens live in `presentation/common/Theme.kt`; the adaptive launcher icon is the three-colour "S" line (`res/drawable/ic_launcher_*`). The app is called **Network Rush** (Play title: "Network Rush: Tram Strasbourg"; `strings.xml`). `applicationId` is `com.agsoft.networkrush` (irreversible once published); the Kotlin package stays `com.triviamap`.

## Monetization

Non-intrusive: an AdMob banner on the home screen only (after GDPR consent) and optional tips through Google Play Billing (a tip also removes the banner). Setup and pre-publication checklist: [docs/MONETIZATION.md](docs/MONETIZATION.md). Debug and default builds use Google's test ad ids.

## Credits

Strasbourg tram network © Eurométropole de Strasbourg / CTS.
