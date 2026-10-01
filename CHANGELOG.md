# Changelog

All notable changes to Network Rush are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Game over: the HOME button could sit under the Android navigation buttons on some devices.
- Game over: the "New record" badge no longer covers the score.
- Home: the ad banner no longer overlaps the buttons on small screens or with large text.

## [1.0.2] - 2026-09-30

### Changed
- Puzzles are at most 7 stations, and the Sprint header is more compact (stage and type on one line; in Sort & Reorder each line's direction sits under its badge), so the tile list never needs scrolling on most phones.

### Fixed
- Sort & Reorder: stations could not be dragged sideways between columns after earlier Reorder puzzles in a run.

## [1.0.1] - 2026-09-29

### Added
- In-app update prompt: a banner on the home screen when a newer version is on Google Play (background download, then "restart"), dismissible once per version. Only appears for builds installed from Google Play.

### Changed
- The Sprint tile list fits on screen instead of scrolling.

### Fixed
- Crash on rapid back taps.
- CLASSIFY drag getting stuck at the screen edge, which blocked horizontal moves.

## [1.0.0] - 2026-09-21

First release (`versionCode` 1).

### Added
- Station Sprint: reorder or classify tram stations against the clock, with combo scoring, skips, spaced repetition and rising difficulty.
- Daily challenge: 8 questions, one attempt each, rising difficulty, elapsed time as tie-breaker, shareable result.
- Progression: XP, levels, badges and a daily streak.
- Optional tips through Google Play Billing (remove the home banner, nothing is locked) and a consent-aware AdMob banner on the home screen only.
- English, French and German localization.
- Legal section in Settings (open data attribution, privacy policy and terms).
- Left-handed setting and haptics toggle.
- Terminus visual identity (tram signage look, adaptive icon).

### Changed
- Targets API 36: system-bar insets, predictive back, centred UI on wide windows.

### Fixed
- Data-loading race at start-up that could leave the loading screen forever.

[Unreleased]: https://github.com/AgSoft-dev/network_rush_android/compare/v1.0.2...HEAD
[1.0.2]: https://github.com/AgSoft-dev/network_rush_android/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/AgSoft-dev/network_rush_android/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/AgSoft-dev/network_rush_android/releases/tag/v1.0.0
