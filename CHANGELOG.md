# Changelog

All notable changes to Network Rush are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- In-app update prompt: a banner on the home screen when a newer version is on Google Play (flexible update, background download, then "restart"), dismissible once per version. Only appears for builds installed from Google Play.
- Changelog (this file) and a `changelog` skill describing how to maintain it.

## [1.0.0] - Unreleased

First public release, not yet published on Google Play (`versionCode` 1).

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
- The Sprint tile list fits on screen instead of scrolling.

### Fixed
- Crash on rapid back taps.
- CLASSIFY drag getting stuck at the screen edge, which blocked horizontal moves.
- Data-loading race at start-up that could leave the loading screen forever.

[Unreleased]: https://github.com/AgSoft-dev/network_rush_android/compare/v1.0.0...HEAD
