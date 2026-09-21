package com.triviamap.presentation.gameplay

import androidx.lifecycle.SavedStateHandle
import com.triviamap.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triviamap.domain.model.*
import com.triviamap.domain.repository.GameResultRepository
import com.triviamap.domain.progress.Badges
import com.triviamap.domain.progress.ProgressStats
import com.triviamap.domain.progress.Progression
import com.triviamap.domain.progress.RunSummary
import com.triviamap.domain.progress.RunSummaryHolder
import com.triviamap.domain.progress.StationStat
import com.triviamap.domain.repository.LinesState
import com.triviamap.domain.repository.StationStatsRepository
import com.triviamap.domain.repository.TramLineRepository
import com.triviamap.domain.repository.UserPreferencesRepository
import com.triviamap.domain.sprint.Challenge
import com.triviamap.domain.sprint.ChallengeGenerator
import com.triviamap.domain.sprint.ChallengeType
import com.triviamap.domain.sprint.Direction
import com.triviamap.domain.sprint.DailyChallenge
import com.triviamap.domain.sprint.Side
import com.triviamap.domain.sprint.SprintRules
import com.triviamap.util.TimeSource
import com.triviamap.util.GeoBounds
import com.triviamap.util.ScoreBreakdown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.roundToInt

data class SprintUiState(
    val phase: GamePhase = GamePhase.Loading,
    val challengeType: ChallengeType = ChallengeType.REORDER,
    val correctOrder: List<Station> = emptyList(),
    val currentTiles: List<Station> = emptyList(),
    /** For CLASSIFY: station ID to current horizontal side (-1 left, 0 center, 1 right) */
    val stationSides: Map<String, Int> = emptyMap(),
    /** For CLASSIFY: station ID to target horizontal side */
    val correctSides: Map<String, Int> = emptyMap(),
    val line: TramLine? = null,
    val line2: TramLine? = null,
    val bounds: GeoBounds? = null,
    val score: Int = 0,
    val timeLeftMs: Long = 0,
    val level: Int = 1,
    val stage: Int = 1,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isDaily: Boolean = false,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val isForward: Boolean = true,
    /** Direction hints ("Line A toward Illkirch") of the current challenge. */
    val directions: List<Direction> = emptyList(),
    val skipsLeft: Int = SprintRules.MAX_SKIPS,
    /** Visual feedback triggers */
    val lastTimeGain: Int = 0,
    val showFeedback: Boolean = false,
    val isCorrectFeedback: Boolean = true,
    val feedbackTrigger: Int = 0,
    val lastTimePenalty: Int = 0,
    /** After a wrong answer: tiles that are not where they belong, and how many are fine. */
    val misplacedIds: Set<String> = emptySet(),
    val placedCount: Int = 0,
    val tileCount: Int = 0,
    /** True when the bundled line data could not be loaded. */
    val loadFailed: Boolean = false,
    /** Settings */
    val leftHanded: Boolean = false,
    val hapticsEnabled: Boolean = true,

    /** Stats for scoring improvements */
    val challengeStartedAt: Long = 0L,
    val correctSubmissions: Int = 0,
    val totalSubmissions: Int = 0,
    val correctClassifyCount: Int = 0,
    /** One char per answer: G correct, R wrong / timeout, S skipped. */
    val answerLog: String = "",

    /** Cumulative session path for the map */
    val sessionStations: List<Station> = emptyList(),

    /** Speed Burst local timer (0..1.0) */
    val burstTimer: Float = 0f,

    /** Result data */
    val isNewRecord: Boolean = false,
    val streak: Int = 0
)

@HiltViewModel
class SprintViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lineRepository: TramLineRepository,
    private val resultRepository: GameResultRepository,
    private val userPrefs: UserPreferencesRepository,
    private val statsRepository: StationStatsRepository,
    private val summaryHolder: RunSummaryHolder,
    private val generator: ChallengeGenerator,
    private val time: TimeSource,
    private val clock: Clock
) : ViewModel() {

    private val isDaily: Boolean = savedStateHandle.get<Boolean>("daily") ?: false

    // The daily challenge is always played on the same rules, whatever the picked difficulty
    private val difficulty: Difficulty =
        if (isDaily) Difficulty.MEDIUM
        else Difficulty.valueOf(checkNotNull(savedStateHandle["difficulty"]))

    // Dev-only knobs (debug builds): start at a given level / force one challenge type
    private val devStartLevel: Int =
        if (BuildConfig.DEBUG) (savedStateHandle.get<Int>("startLevel") ?: 1).coerceAtLeast(1) else 1
    private val devForcedType: ChallengeType? =
        if (BuildConfig.DEBUG) savedStateHandle.get<String>("force")?.let { runCatching { ChallengeType.valueOf(it) }.getOrNull() } else null

    private val maxTimeMs: Long = SprintRules.maxTimeMs(difficulty)
    private val epochDay: Long = LocalDate.now(clock).toEpochDay()

    private val _state = MutableStateFlow(SprintUiState(
        difficulty = difficulty,
        isDaily = isDaily,
        level = devStartLevel,
        timeLeftMs = maxTimeMs
    ))
    val state: StateFlow<SprintUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var burstTimerJob: Job? = null
    private var allLines: List<TramLine> = emptyList()
    private var current: Challenge? = null
    private var weights: Map<String, Double> = emptyMap()
    private val statsWrites = mutableListOf<Job>()
    private var answersXp = 0
    private var skipsUsed = 0

    /** Set once the run is over; every state-mutating entry point bails out afterwards. */
    private var finished = false
    private var paused = false
    private var pausedAt = 0L
    private var gameStartedAt = 0L

    init {
        viewModelScope.launch {
            userPrefs.leftHanded.collect { left -> _state.update { it.copy(leftHanded = left) } }
        }
        viewModelScope.launch {
            userPrefs.hapticsEnabled.collect { on -> _state.update { it.copy(hapticsEnabled = on) } }
        }
        loadLines()
    }

    private fun loadLines() = viewModelScope.launch {
        _state.update { it.copy(loadFailed = false) }
        launch { lineRepository.load() }
        when (val loaded = lineRepository.state.first { it !is LinesState.Loading }) {
            is LinesState.Loaded -> {
                // Sprint needs at least 3 stations per line (speed burst window)
                allLines = loaded.lines.filter { it.stations.size >= 3 }
                if (allLines.isEmpty()) {
                    _state.update { it.copy(loadFailed = true) }
                } else if (!finished && timerJob == null) {
                    // Spaced repetition: stations the player misses (or never saw) come up more often
                    if (!isDaily) {
                        val stats = statsRepository.snapshot()
                        weights = allLines.flatMap { it.stations }
                            .associate { it.id to StationStat.weight(stats[it.id]) }
                    }
                    gameStartedAt = time.elapsedMs()
                    generateChallenge()
                }
            }
            is LinesState.Error -> _state.update { it.copy(loadFailed = true) }
            LinesState.Loading -> Unit
        }
    }

    fun retryLoad() {
        if (allLines.isEmpty()) loadLines()
    }

    /** Called when the screen leaves the foreground: freezes every timer. */
    fun pause() {
        if (finished || paused || timerJob == null) return
        paused = true
        pausedAt = time.elapsedMs()
        burstTimerJob?.cancel()
    }

    fun resume() {
        if (!paused) return
        paused = false
        val pausedFor = time.elapsedMs() - pausedAt
        _state.update { it.copy(challengeStartedAt = it.challengeStartedAt + pausedFor) }
        if (_state.value.challengeType == ChallengeType.SPEED_BURST) startBurstTimer()
    }

    private fun generateChallenge() {
        if (allLines.isEmpty() || finished) return

        val level = _state.value.level
        val challenge = if (isDaily) {
            // Same seed for everybody: challenge depends only on (day, level, skips used)
            generator.generate(level, allLines, difficulty, emptyMap(), DailyChallenge.random(epochDay, level * 4 + skipsUsed))
        } else {
            generator.generate(level, allLines, difficulty, weights, forceType = devForcedType)
        }
        val geometry = challenge.line.geometry.ifEmpty { challenge.line.stations.map { it.position } }

        _state.update {
            it.copy(
                challengeType = challenge.type,
                line = challenge.line,
                line2 = challenge.line2,
                bounds = GeoBounds.from(geometry),
                correctOrder = challenge.correctOrder,
                currentTiles = challenge.tiles,
                stationSides = challenge.tiles.associate { s -> s.id to Side.HUB },
                correctSides = challenge.correctSides,
                isForward = challenge.isForward,
                directions = challenge.directions,
                misplacedIds = emptySet(),
                phase = GamePhase.Drawing,
                challengeStartedAt = time.elapsedMs(),
                stage = SprintRules.stage(level)
            )
        }
        current = challenge

        if (timerJob == null) startTimer()
        if (challenge.type == ChallengeType.SPEED_BURST) startBurstTimer() else stopBurstTimer()
    }

    private fun startBurstTimer() {
        burstTimerJob?.cancel()
        burstTimerJob = viewModelScope.launch {
            val limit = SprintRules.burstLimitMs(_state.value.level)
            val start = time.elapsedMs()
            while (true) {
                val progress = (time.elapsedMs() - start).toFloat() / limit
                if (progress >= 1f) {
                    _state.update { it.copy(burstTimer = 1f) }
                    handleBurstTimeout()
                    break
                }
                _state.update { it.copy(burstTimer = progress) }
                delay(30)
            }
        }
    }

    private fun stopBurstTimer() {
        burstTimerJob?.cancel()
        _state.update { it.copy(burstTimer = 0f) }
    }

    private fun handleBurstTimeout() {
        if (finished) return
        val penalty = SprintRules.BURST_TIMEOUT_PENALTY_MS
        recordOutcomes(current, allCorrect = false)
        viewModelScope.launch {
            _state.update { it.copy(
                timeLeftMs = (it.timeLeftMs - penalty).coerceAtLeast(0),
                combo = 0,
                totalSubmissions = it.totalSubmissions + 1,
                answerLog = it.answerLog + 'R',
                showFeedback = true,
                isCorrectFeedback = false,
                misplacedIds = emptySet(),
                placedCount = 0,
                tileCount = 0,
                lastTimePenalty = (penalty / 1000).toInt(),
                feedbackTrigger = it.feedbackTrigger + 1
            ) }
            delay(700)
            _state.update { it.copy(showFeedback = false) }
            generateChallenge()
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            var lastUpdate = time.elapsedMs()
            while (!finished && _state.value.timeLeftMs > 0) {
                delay(50)
                val now = time.elapsedMs()
                if (paused) { lastUpdate = now; continue }
                val delta = now - lastUpdate
                lastUpdate = now
                _state.update { it.copy(timeLeftMs = (it.timeLeftMs - delta).coerceAtLeast(0)) }
            }
            endGame()
        }
    }

    /** Persists, per station, whether it was placed correctly (feeds mastery + spaced repetition). */
    private fun recordOutcomes(challenge: Challenge?, allCorrect: Boolean, misplaced: Set<String> = emptySet()) {
        challenge ?: return
        val outcomes = challenge.tiles.associate { it.id to (allCorrect || it.id !in misplaced) }
        statsWrites += viewModelScope.launch { statsRepository.record(outcomes) }
    }

    private fun endGame() = viewModelScope.launch {
        if (finished) return@launch
        finished = true
        val finalState = _state.value
        stopBurstTimer()
        _state.update { it.copy(phase = GamePhase.Validating, showFeedback = false) }

        val mode = if (isDaily) GameMode.DAILY_SPRINT else GameMode.STATION_SPRINT
        val previousHigh = resultRepository.getHighScore(mode, difficulty)
        val isNewRecord = finalState.score > 0 && finalState.score > previousHigh

        val accuracy = if (finalState.totalSubmissions > 0) {
            finalState.correctSubmissions.toFloat() / finalState.totalSubmissions
        } else 0f

        resultRepository.saveResult(GameResult(
            lineId = "SPRINT",
            mode = mode,
            difficulty = difficulty,
            score = finalState.score,
            stationOrderScore = accuracy,
            pathAccuracyScore = 0f,
            completionScore = 0f,
            speedBonusScore = 0f,
            durationMs = time.elapsedMs() - gameStartedAt,
            level = finalState.level,
            maxCombo = finalState.maxCombo,
            accuracy = accuracy,
            answerLog = finalState.answerLog
        ))

        // A run without a single correct answer does not keep the day streak alive
        val streakCounts = finalState.correctSubmissions > 0
        if (streakCounts) userPrefs.updateStreak()
        val currentStreak = if (streakCounts) userPrefs.dailyStreak.first() else 0

        // XP and level
        statsWrites.toList().joinAll()
        val xpBefore = userPrefs.xp.first()
        val xpEarned = Progression.xpForRun(answersXp, finalState.score, isDaily)
        userPrefs.addXp(xpEarned)

        // Badges
        val stats = statsRepository.snapshot()
        val earnedBefore = userPrefs.earnedBadges.first()
        val nowBadges = Badges.evaluate(Badges.Context(
            classifySolved = finalState.correctClassifyCount,
            maxCombo = finalState.maxCombo,
            level = finalState.level,
            streak = currentStreak,
            hourOfDay = LocalDateTime.now(clock).hour,
            isDaily = isDaily,
            totalPlacements = ProgressStats.totalPlacements(stats),
            hasMasteredLine = ProgressStats.hasMasteredLine(allLines, stats)
        ))
        val newBadges = (nowBadges - earnedBefore).mapNotNull { Badges.byId(it) }
        newBadges.forEach { userPrefs.earnBadge(it.id) }

        summaryHolder.publish(RunSummary(
            mode = mode,
            difficulty = difficulty,
            score = finalState.score,
            level = finalState.level,
            maxCombo = finalState.maxCombo,
            accuracy = accuracy,
            isNewRecord = isNewRecord,
            streak = currentStreak,
            xpEarned = xpEarned,
            levelBefore = Progression.levelFor(xpBefore),
            levelAfter = Progression.levelFor(xpBefore + xpEarned),
            newBadges = newBadges,
            answerLog = finalState.answerLog,
            epochDay = epochDay
        ))

        delay(800)
        _state.update { it.copy(
            isNewRecord = isNewRecord,
            streak = currentStreak,
            phase = GamePhase.Finished(
                // Sprint only uses accuracy + total; the rest travels in RunSummary
                ScoreBreakdown(
                    stationOrder = accuracy,
                    completion = 0f,
                    speedBonus = 0f,
                    total = finalState.score
                )
            )
        ) }
    }

    /** Commits the tile order after a drag (the UI reorders locally while dragging). */
    fun setTileOrder(order: List<Station>) {
        if (finished || paused || _state.value.phase !is GamePhase.Drawing) return
        _state.update { state ->
            // Ignore stale/foreign lists (e.g. a drag released just after a new challenge)
            if (order.toSet() != state.currentTiles.toSet()) state
            else state.copy(currentTiles = order, misplacedIds = emptySet())
        }
    }

    fun setStationSide(stationId: String, side: Int) {
        if (finished || paused || _state.value.phase !is GamePhase.Drawing) return
        _state.update { it.copy(stationSides = it.stationSides + (stationId to side), misplacedIds = emptySet()) }
    }

    /** Skipping costs time and the combo, and is limited per run: never better than answering. */
    fun skipQuestion() {
        val s = _state.value
        if (finished || paused || s.showFeedback || s.phase !is GamePhase.Drawing || s.skipsLeft <= 0) return
        stopBurstTimer()
        recordOutcomes(current, allCorrect = false)
        skipsUsed++
        _state.update { it.copy(
            totalSubmissions = it.totalSubmissions + 1,
            skipsLeft = it.skipsLeft - 1,
            answerLog = it.answerLog + 'S',
            timeLeftMs = (it.timeLeftMs - SprintRules.SKIP_PENALTY_MS).coerceAtLeast(0),
            combo = 0
        ) }
        generateChallenge()
    }

    fun submit() {
        val currentState = _state.value
        if (finished || paused || currentState.showFeedback || currentState.phase !is GamePhase.Drawing) return
        val challenge = current ?: return

        val misplaced = challenge.misplaced(currentState.currentTiles, currentState.stationSides)
        _state.update { it.copy(totalSubmissions = it.totalSubmissions + 1) }
        recordOutcomes(challenge, allCorrect = misplaced.isEmpty(), misplaced = misplaced)

        if (misplaced.isEmpty()) {
            handleCorrectAnswer(challenge)
        } else {
            val fraction = misplaced.size.toFloat() / challenge.tiles.size
            val timePenalty = SprintRules.wrongAnswerPenaltyMs(currentState.level, fraction)

            viewModelScope.launch {
                _state.update { it.copy(
                    timeLeftMs = (it.timeLeftMs - timePenalty).coerceAtLeast(0),
                    combo = 0,
                    answerLog = it.answerLog + 'R',
                    showFeedback = true,
                    isCorrectFeedback = false,
                    misplacedIds = misplaced,
                    placedCount = challenge.tiles.size - misplaced.size,
                    tileCount = challenge.tiles.size,
                    lastTimePenalty = (timePenalty / 1000f).roundToInt(),
                    feedbackTrigger = it.feedbackTrigger + 1
                ) }
                delay(700)
                _state.update { it.copy(showFeedback = false) }
            }
        }
    }

    private fun handleCorrectAnswer(challenge: Challenge) {
        val currentState = _state.value
        val timeTakenMs = time.elapsedMs() - currentState.challengeStartedAt

        val newCombo = currentState.combo + 1
        val reward = SprintRules.correctAnswerReward(
            difficulty, currentState.level, challenge.type, challenge.tiles.size, newCombo, timeTakenMs
        )
        answersXp += Progression.xpForCorrectAnswer(currentState.stage, newCombo)

        viewModelScope.launch {
            stopBurstTimer()
            _state.update { state ->
                val newTimeLeft = (state.timeLeftMs + reward.timeGainMs).coerceAtMost(maxTimeMs)
                val actualGainMs = newTimeLeft - state.timeLeftMs

                state.copy(
                    score = state.score + reward.points,
                    timeLeftMs = newTimeLeft,
                    lastTimeGain = (actualGainMs / 1000f).roundToInt(),
                    level = state.level + 1,
                    combo = newCombo,
                    maxCombo = maxOf(state.maxCombo, newCombo),
                    correctSubmissions = state.correctSubmissions + 1,
                    correctClassifyCount = if (challenge.type == ChallengeType.CLASSIFY) state.correctClassifyCount + 1 else state.correctClassifyCount,
                    answerLog = state.answerLog + 'G',
                    misplacedIds = emptySet(),
                    showFeedback = true,
                    isCorrectFeedback = true,
                    feedbackTrigger = state.feedbackTrigger + 1,
                    // Sorting challenges mix two lines: their "path" would be meaningless on the map
                    sessionStations = if (challenge.type == ChallengeType.CLASSIFY) state.sessionStations
                                      else state.sessionStations + challenge.correctOrder
                )
            }
            delay(700)
            _state.update { it.copy(showFeedback = false) }
            generateChallenge()
        }
    }
}
