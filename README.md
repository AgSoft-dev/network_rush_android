# TriviaMap — Strasbourg Tram Challenge

Android game (Kotlin, Jetpack Compose) to learn the Strasbourg tram network.

> **Scope:** Strasbourg is the pilot city for now. Support for other cities is planned (see TODO.md §5, multi-city abstraction) but not started: the data loader and the UI are still Strasbourg-specific.

**Station Sprint** is the main mode: reorder the stations of a line (or sort stations between two lines and their shared hub) against a clock. Correct answers earn time and points, wrong answers and skips cost time, combos multiply the score.

**Trace Network** (redraw the network from memory) is parked: its button is only shown in debug builds while its scoring is reworked. See [TODO.md](TODO.md).

## Architecture

```
app/src/main/java/com/triviamap/
├── data/
│   ├── local/        Room (GameResult, schema v2, migrations, exported to app/schemas/)
│   ├── model/        GeoJsonParser — parses assets/strasbourg_stations.json
│   └── repository/   TramLineRepositoryImpl (Loading/Loaded/Error), GameResultRepositoryImpl, UserPreferencesRepositoryImpl (DataStore)
├── di/               Hilt modules (DB, repositories, Random / Clock / TimeSource)
├── domain/
│   ├── model/        TramNetwork (GeoPoint, Station, TramLine), GameMode, GameResult, StreakCalculator
│   ├── repository/   Interfaces
│   └── sprint/       ChallengeGenerator, Challenge, SprintRules — pure Kotlin, unit-tested
├── presentation/     Compose screens + ViewModels (home, gameplay, results, stats)
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

## Scoring (Sprint)

- Points: `(500 + 50·level) × (2 if CLASSIFY) × speedFactor(1.0–1.5) × comboFactor(1.0–2.0)`
- Time: +8…12 s base by difficulty (decreasing with stage) + combo bonus (≤ 5 s) + type bonus; wrong answer −5…9 s; skip −4 s and combo reset.
- Clock: 60 / 45 / 30 s (easy / medium / hard), capped at its initial value.
- Details: `domain/sprint/SprintRules.kt`.

## Credits

Strasbourg tram network © Eurométropole de Strasbourg / CTS.
