---
name: sprint-gameplay
description: Frozen game design and balance rules of TriviaMap's Station Sprint mode (challenge types, clock economy, scoring, skips, spaced repetition, daily challenge, XP/levels/badges, streak). Use before changing anything that affects how the game plays, is scored, generated or progresses, so changes are deliberate and tests/docs stay in sync.
---

# Station Sprint: gameplay contract

Station Sprint is the **only shipped mode** (Strasbourg tram network is the pilot city). Trace Network is parked and only reachable from a debug-only button. Treat this file as the design contract: **change a rule only on purpose**, and when you do, update the code constants, the tests named below, the README "Game design (Sprint)" section, and this file in the same change.

## Core loop
Reorder the stations of a tram line by drag & drop, press CHECK ORDER, gain time and points if right, lose time if wrong. The run ends when the clock hits 0. Level goes up by 1 per correct answer.

## Where the rules live (pure Kotlin, unit-tested, no Android)
- `domain/sprint/SprintRules.kt`: all balance constants and formulas.
- `domain/sprint/ChallengeGenerator.kt` + `Challenge.kt`: what puzzle comes next, correctness (`misplaced`, `isSolvedBy`).
- `domain/sprint/DailyChallenge.kt`: seeded randomness for the daily.
- `domain/progress/`: `Progression` (XP/levels), `Badges`, `StationStat` (mastery/spaced repetition), `ProgressStats`, `ShareText`.
- `presentation/gameplay/SprintViewModel.kt`: orchestration only (timers, state, persistence). Keep rules out of it and out of composables. Randomness, clock and time are injected (`Random`, `Clock`, `TimeSource`) so tests stay deterministic.

## Clock and economy
- Start clock: Easy 60 s, Medium 45 s, Hard 30 s (`maxTimeMs`; the clock can never exceed it).
- Time gained per correct answer = `perTile x tiles x levelFactor + comboBonus + typeBonus`
  - perTile: Easy 1300 ms, Medium 1050, Hard 950. levelFactor = `max(floor, 1 - 0.03 x (level-1))`, floor Easy 0.40, Medium 0.35, Hard 0.30 (so every run ends).
  - comboBonus = 400 ms x min(combo, 5). typeBonus: Classify +3500 ms, Speed burst +1500 ms, Reorder 0.
- Wrong answer: penalty = `(4000 + 500 x (stage-1)) ms x (0.5 + 0.5 x misplacedFraction)`, so almost-right costs less. The player then sees which tiles are right (green) or wrong (red) and "x / y in place", and can retry the same puzzle.
- Skip: costs 4 s **and resets the combo**, max 3 per run (`MAX_SKIPS`); it is never better than answering. Burst timeout counts as a failed attempt (-3 s).
- Balance is validated by `SprintEconomyTest` (average player survives about 90 s on Medium; Easy > Medium > Hard). Re-run and re-tune it after any economy change.

## Stages and puzzle types
Stage by level: 1-5 -> 1, 6-10 -> 2, 11-15 -> 3, 16-20 -> 4, 21+ -> 5.
- Tiles: stage 1 = 3-4, stage 2 = 5, stage 3 = 6, stage 4 = 6-7, stage 5 = 7 growing to a cap of 10.
- **Reorder**: stations of one line in order; direction shown as "line -> terminus" chips ("first station on top").
- **Classify** (from stage 3): sort stations into 3 columns (line A / shared hub / line B), then each column is ordered by its own line's direction; hubs follow line 1. The generator never serves an already-solved puzzle.
- **Speed burst** (from stage 2): 3 tiles, limit 8 s (stages 1-2), 7 s (3), 6.5 s (4), 6 s (5).
- Type schedule per stage is in `ChallengeGenerator.generate`; a Classify needs >= 2 lines, a burst needs a line with >= 3 stations (falls back to Reorder).
- Difficulty also changes content: Hard mixes reversed direction and non-contiguous station sets early.
- The last 4 served segments are not repeated right away.

## Scoring
`points = (500 + 50 x level) x (2 if Classify) x speedFactor (1..2, relative to puzzle size) x comboFactor (1 + 0.1 x min(combo, 20), so up to x3)`. A wrong answer or a skip resets the combo. The first score of a difficulty counts as a record.

## Spaced repetition and mastery
Per-station stats are stored (Room `station_stats`). A station is **mastered** after 3 correct placements in a row (`MASTERY_STREAK`). Stations that are unseen or often missed get a higher draw weight (`StationStat.weight`). The daily challenge ignores weights (same puzzles for everybody).

## Daily challenge
One attempt per day, fixed rules (Medium), puzzles seeded by `epochDay` + level + skips used (`DailyChallenge.random`), so all players get the same sequence. Result is stored as `DAILY_SPRINT`, shareable as a Wordle-style text (green/red/grey squares). Known limits: reinstalling lets a player replay it; editing the stations data file changes the day's puzzles (see `TODO.md`).

## Progression and retention
- XP: per correct answer `10 + 2 x stage + min(combo, 10)`, plus `score / 100` per run, plus 50 for a daily. Level n needs `50 x n x (n-1)` XP total (2 = 100, 3 = 300, ...). Titles change every 3 levels: Passenger, Regular, Commuter, Conductor, Line Chief, Network Master.
- **Day streak**: a run counts only if it has **at least one correct answer**; a gap of more than one day resets it.
- Badges (10): First ride, Hub expert (10 Classify in a run), Night rider (run between 22:00 and 04:00), On a roll (x10 combo), Terminus (level 21), Three in a row, Weekly commuter, Daily rider, Regular (200 stations placed), Line master. Ids and rules in `domain/progress/Badges.kt`.

## Input and feel constraints
Drag is arbitrated on the pointer `Initial` pass so scrolling and dragging never fight; the drag handle sits on the right, or the left in left-handed mode (Settings). Haptics on drag start, column change, success, failure (Settings toggle). Don't reintroduce per-frame full-list recomposition or state commits during drag (commit to the ViewModel on release). The current line's stations must never be drawn on the background canvas (it leaks the answer).

## Changing the game: checklist
1. Edit the constant/formula in the domain layer (never in a composable).
2. Update or add unit tests (`SprintRulesTest`, `SprintEconomyTest`, `ChallengeTest`, `ChallengeGeneratorTest`, `DailyChallengeTest`, `ProgressTest`, `StreakCalculatorTest`) and keep `./gradlew testDebugUnitTest` green.
3. Update this file and the README "Game design (Sprint)" section; note open questions in `TODO.md`.
4. If the change alters daily puzzles, results storage or the Room schema, think about existing players (Room migration + exported schema in `app/schemas/`, daily fairness).
5. Test the feel on a device or emulator (Dev panel "DEV · SPRINT VARIANTS" on Home in debug builds forces the challenge type and start level).
