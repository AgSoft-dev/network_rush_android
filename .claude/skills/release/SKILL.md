---
name: release
description: Cut a Network Rush release for Google Play (invoked as /release [major|minor|patch|x.y.z]). Bumps versionName/versionCode, finalizes CHANGELOG.md, writes the Play "What's new" text (EN/FR/DE), opens the release PR, then tags and creates the GitHub release after the merge. Use when the user asks to release, ship, publish a version or prepare the Play Store upload.
---

# /release: ship a version to Google Play

Argument: bump kind (`major`, `minor`, `patch`) or an explicit `x.y.z`. If absent, propose one from the `[Unreleased]` content (SemVer: breaking or data reset = major, new feature = minor, fixes only = patch) and ask.

Follow the `changelog` skill for CHANGELOG rules. This skill is the whole workflow around it. **Do each phase in order and stop at the confirmation points.**

## Constraints learned the hard way
- `main` is protected: nothing lands without a PR that the user merges. Never push to `main`, never force-push, never rewrite a published tag or release.
- Branch off `origin/main` (`git fetch origin`, then `git switch -c release/x.y.z origin/main --no-track`). Do not use the stale local `master`.
- The tag goes on the commit that is **on `main` after the merge** (squash/rebase changes hashes), never on the PR branch.
- `gh` lives in `/opt/homebrew/bin`: `export PATH=/opt/homebrew/bin:$PATH`. Needs `gh auth status` OK.
- The auto-mode permission check may refuse pushes, deletions or `git fetch --prune`. If refused, do not work around it: give the user the exact command to run.
- Never touch signing config, keystores or `nrStore*` properties. The user builds and signs the AAB (`./gradlew bundleRelease`, JDK from `JAVA_HOME` = Android Studio JBR if needed).
- Never commit AdMob ids or secrets.

## Phase 1: preflight (read only)
1. Working tree clean, on a fresh branch from `origin/main`; `CHANGELOG.md` has content under `[Unreleased]` (else stop: nothing to release).
2. Read current `versionName`/`versionCode` in `app/build.gradle.kts` and the latest tag (`git tag --sort=-v:refname | head -1`).
3. **versionCode**: it must be higher than every code ever uploaded to Play, including internal/closed tracks and rejected drafts, and `main`'s value is not proof (code 2 was once rejected as "already used"). Propose `current + 1` and ask the user to confirm the last code shown in Play Console.
4. Run `./gradlew testDebugUnitTest lintDebug assembleDebug` and report failures before going further.

## Phase 2: prepare the release PR
1. `app/build.gradle.kts`: set `versionName` and `versionCode`.
2. `CHANGELOG.md`: turn `[Unreleased]` content into `## [x.y.z] - YYYY-MM-DD` (today), leave an empty `## [Unreleased]` above, update the compare links at the bottom (`[Unreleased]: .../compare/vx.y.z...HEAD`, `[x.y.z]: .../compare/vPREV...vx.y.z`). Repo URL: `https://github.com/AgSoft-dev/network_rush_android`.
3. `docs/legal/STORE_LISTING.md`: add or replace a `## What's new: x.y.z (max 500 per language)` section with `### EN`, `### FR`, `### DE`. Player-facing, one `•` bullet per change, French uses "tu" (see `terminus-design` for copy). Check each language is at most 500 characters.
4. `TODO.md`: tick or add release items if relevant (project convention).
5. Commit (`Prepare release x.y.z`), push the branch, `gh pr create --base main` with the changelog section as the body. Report the PR URL and ask the user to merge. **Stop here.**

## Phase 3: after the user has merged
1. `git fetch origin` (no `--prune`), confirm the release PR is merged: `gh pr view <n> --json state,mergeCommit`.
2. Tag the merge commit on main: `git tag -a vX.Y.Z -m "Release X.Y.Z" origin/main`. Confirm with the user before `git push origin vX.Y.Z`.
3. Release notes = the `[x.y.z]` section body: extract with `sed -n '/^## \[x.y.z\]/,/^## \[PREV\]/p' CHANGELOG.md | sed '1d;$d' > notes`, then `gh release create vX.Y.Z --verify-tag --title "Network Rush X.Y.Z" --notes-file notes`.
4. Tell the user the remaining manual steps: `./gradlew bundleRelease` (output `app/build/outputs/bundle/release/app-release.aab`), upload to Play Console, paste the What's new text, roll out. Remind that an old AAB in `app/build/outputs` is stale if its date predates the version bump.
5. Housekeeping: offer to delete the merged release branch (local and remote); give the commands if the permission check refuses.

## Hotfixes
Same flow with a `patch` bump from `origin/main`; keep `[Unreleased]` for unrelated pending work by moving only the fix bullets into the new section.
