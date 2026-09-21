# Network Rush (code name TriviaMap)

Android game (Kotlin, Jetpack Compose, Hilt, Room) to learn the Strasbourg tram network. Only Station Sprint is shipped; Trace Network is parked (debug button). See `README.md` for architecture and build, `TODO.md` for the tracked backlog.

## Skills (read before working in these areas)
- `.claude/skills/terminus-design/SKILL.md`: visual identity, tokens, components. Use for any UI/UX work or copy.
- `.claude/skills/sprint-gameplay/SKILL.md`: frozen gameplay/balance contract. Use before touching rules, scoring, puzzle generation, progression or the daily challenge.

## Conventions
- Game rules stay in `domain/` (pure Kotlin, unit-tested); ViewModels orchestrate; composables only render.
- Use the tokens in `presentation/common/Theme.kt`; no hard-coded colours or fonts.
- Keep `TODO.md` up to date when you fix, add or defer something; monetization setup is in `docs/MONETIZATION.md`.
- Never commit real AdMob ids or signing secrets; `design-explorations/` is intentionally untracked.

## Build and test
`./gradlew testDebugUnitTest lintDebug assembleDebug` (run `assembleRelease` separately from lint). JDK 21 comes from the Gradle daemon JVM setting; if the shell JDK is unsuitable, point `JAVA_HOME` at Android Studio's bundled JBR.
