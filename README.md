# TriviaMap — Strasbourg Tram Challenge

A minimalist Android game where players redraw tram lines from memory on a live canvas map.

---

## Architecture

```
app/
└── src/main/java/com/triviamap/
    ├── data/
    │   ├── local/          Room database, DAOs, entities
    │   ├── model/          GeoJSON parser
    │   └── repository/     Repository implementations
    ├── di/                 Hilt modules
    ├── domain/
    │   ├── model/          Pure Kotlin data models (TramLine, Station, GameResult…)
    │   ├── repository/     Repository interfaces
    │   └── usecase/        Single-responsibility use cases
    ├── presentation/
    │   ├── common/         Theme, NavGraph, shared composables
    │   ├── home/           HomeScreen
    │   ├── lineselection/  LineSelectionScreen + ViewModel
    │   ├── gameplay/       GameplayScreen + GameplayViewModel + TramMapCanvas
    │   ├── results/        ResultsScreen
    │   └── stats/          StatsScreen + StatsViewModel
    └── util/
        ├── GeometryEngine.kt   Projection, RDP simplification, Fréchet, Bézier
        └── ScoringEngine.kt    Score breakdown computation
```

### Key design decisions

| Decision | Rationale |
|---|---|
| **No Google Maps** | Custom Canvas renderer gives full control over transit-diagram aesthetic |
| **Material 2** | Stable, predictable dark theming without M3 dynamic color |
| **Hilt** | Ergonomic DI without reflection overhead |
| **Room** | Offline-first persistence; no network needed after first install |
| **Discrete Fréchet distance** | Better than Hausdorff for ordered paths; penalises mis-routing |
| **RDP simplification** | Reduces 400+ pt GeoJSON polylines to ~30 pts before scoring |
| **Bézier smoothing** | Catmull-Rom-style cubic segments make drawn lines feel fluid |

---

## Coordinate pipeline

```
GeoJSON (lon, lat)
    ↓  GeoBounds.from()            — compute WLTP bounding box + 5% padding
    ↓  GeoPoint.toNormalized()     — project to [0,1] × [0,1] canvas space
    ↓  scale(size.width, height)   — pixel coordinates for Canvas
    ↓  withTransform(pan, zoom)    — interactive navigation
```

The inverse pipeline is used for touch input: pixel → normalised → geo.

---

## Scoring (0 – 1000 pts)

| Criterion | Weight (Medium) | Notes |
|---|---|---|
| Station order | 30% | Longest Common Subsequence vs reference |
| Path accuracy | 35% | Discrete Fréchet distance; 0 m = 100%, ≥800 m = 0% |
| Completion | 25% | Fraction of stations touched (within 150 m snap radius) |
| Speed bonus | 10% | Linear decay from 0 → time limit |

Weights shift by difficulty (Easy favours order; Hard favours accuracy).

---

## Getting started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Build

```bash
git clone <repo>
cd TriviaMap
./gradlew assembleDebug
```

### Data refresh

The GeoJSON data is bundled in `app/src/main/assets/strasbourg_tram.geojson`.

To update from the open-data portal:
```bash
curl "https://data.strasbourg.eu/api/explore/v2.1/catalog/datasets/lignes_tram/exports/geojson" \
  -o app/src/main/assets/strasbourg_tram.geojson
```

Data licence: **Licence Ouverte v2.0 (Etalab)** — free reuse with attribution.

---

## Roadmap

### v0.2 — Polish
- [ ] Station label collision avoidance
- [ ] Haptic feedback on station snap (VibrationEffect)
- [ ] Animated line validation overlay (player path morphs onto reference)
- [ ] Score breakdown screen with per-criterion bars

### v0.3 — Game modes
- [ ] Daily challenge (fixed seed per calendar day)
- [ ] Time attack (all lines, cumulative score)
- [ ] "Find missing segment" mode
- [ ] "Which line serves these stations?" quiz

### v0.4 — Multiplayer / Social
- [ ] Ghost replay system (local, then remote via Firebase)
- [ ] Leaderboard per line × difficulty
- [ ] Share result card

### v1.0 — Multi-city
- [ ] Abstract city data loader (any GeoJSON tram network)
- [ ] City selection screen
- [ ] Lyon, Bordeaux, Nantes, Montpellier datasets

### Performance notes
- Canvas `drawPath` batches all segments per frame — no per-vertex draw calls
- Fréchet distance is computed on downsampled paths (≤50 pts each) → O(2500) ops
- `stateIn(WhileSubscribed(5_000))` ensures Flow collection survives config changes
- `detectTransformGestures` and `awaitPointerEvent` are on separate `pointerInput` keys so pan and draw don't conflict

---

## Credits
Strasbourg tram data © Eurométropole de Strasbourg — Licence Ouverte v2.0 (Etalab).
