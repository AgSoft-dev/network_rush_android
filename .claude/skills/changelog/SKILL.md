---
name: changelog
description: Maintain CHANGELOG.md following Keep a Changelog 1.1.0 and Semantic Versioning. Use whenever a change is user-visible (feature, behaviour change, fix, removal, deprecation, security), when preparing a release or bumping versionName/versionCode, or when asked to write release notes or Play Store "what's new" text.
---

# Changelog (Keep a Changelog 1.1.0)

Spec: https://keepachangelog.com/en/1.1.0/. The file is `CHANGELOG.md` at the repo root. It is for **players and reviewers, not for git**: never paste commit logs.

## Rules
- Every user-visible change adds a bullet under `## [Unreleased]` **in the same change/PR**. Skip pure refactors, tests, CI, docs and tooling unless they affect users or release behaviour.
- One bullet per change, past-tense-free imperative or noun phrase, plain language, one line. Say what changed for the player ("Fix crash on rapid back taps"), not how.
- Group under exactly these headings, in this order, omitting empty ones: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`.
- Versions are `## [x.y.z] - YYYY-MM-DD` (ISO date), newest first. `Unreleased` stays at the top. Never rewrite a released section, except to fix a typo.
- Keep the link references at the bottom of the file (`[Unreleased]: .../compare/vX.Y.Z...HEAD`, `[x.y.z]: .../compare/vPREV...vX.Y.Z`), and update them at each release.
- Yanked releases get `[x.y.z] - date [YANKED]`.
- Never put secrets, ids or signing details in it.

## Release procedure
The full workflow (PR, tag after merge, GitHub release, Play text) is automated by the `release` skill (`/release`). The steps below are the changelog-specific part.
1. Choose the version by SemVer: breaking/removed feature or data reset = major, new feature = minor, fixes only = patch.
2. Rename `[Unreleased]` content into a new `## [x.y.z] - YYYY-MM-DD` section and add a fresh empty `## [Unreleased]` above it.
3. Bump `versionName` **and increase `versionCode`** in `app/build.gradle.kts` (the Play Store, and the in-app update prompt, rely on `versionCode` going up on every upload).
4. Update the compare links at the bottom.
5. Tag `vX.Y.Z` after the release commit.
6. Draft Play Console "What's new" (max 500 chars per language, EN/FR/DE) from that section, player-facing tone (see the `terminus-design` skill for copy).

## Related files
- `TODO.md` is the backlog; a done `[x]` item that players notice also needs a changelog bullet.
- `CLAUDE.md` links to this skill.
