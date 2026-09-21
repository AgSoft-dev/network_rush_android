package com.triviamap.presentation.stats

import com.triviamap.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triviamap.domain.model.GameMode
import com.triviamap.domain.model.GameResult
import com.triviamap.domain.progress.Badges
import com.triviamap.domain.progress.LineMastery
import com.triviamap.domain.progress.MissedStation
import com.triviamap.domain.progress.PlayerLevel
import com.triviamap.domain.progress.ProgressStats
import com.triviamap.domain.progress.Progression
import com.triviamap.domain.repository.GameResultRepository
import com.triviamap.domain.repository.StationStatsRepository
import com.triviamap.domain.repository.TramLineRepository
import com.triviamap.domain.repository.UserPreferencesRepository
import com.triviamap.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt

data class ProgressUiState(
    val level: PlayerLevel = Progression.levelFor(0),
    val totalXp: Int = 0,
    val lineMastery: List<LineMastery> = emptyList(),
    val mostMissed: List<MissedStation> = emptyList(),
    /** Scores of the latest Sprint runs, oldest first. */
    val history: List<Int> = emptyList(),
    val earnedBadges: Set<String> = emptySet(),
    val bestScores: List<GameResult> = emptyList(),
    val totalPlacements: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    resultRepository: GameResultRepository,
    statsRepository: StationStatsRepository,
    lineRepository: TramLineRepository,
    prefs: UserPreferencesRepository
) : ViewModel() {

    val state = combine(
        combine(prefs.xp, prefs.earnedBadges, statsRepository.stats, lineRepository.getAllLines(), resultRepository.recentSprintResults(20)) {
                xp, badges, stats, lines, recent ->
            ProgressUiState(
                level = Progression.levelFor(xp),
                totalXp = xp,
                lineMastery = ProgressStats.lineMastery(lines, stats),
                mostMissed = ProgressStats.mostMissed(lines, stats),
                history = recent.map { it.score }.reversed(),
                earnedBadges = badges,
                totalPlacements = ProgressStats.totalPlacements(stats)
            )
        },
        resultRepository.getBestScores()
    ) { progress, best -> progress.copy(bestScores = best) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
}

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    vm: StatsViewModel = hiltViewModel()
) {
    val ui by vm.state.collectAsState()

    Scaffold(
        backgroundColor = Background,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                backgroundColor = Background,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = OnSurface)
                    }
                },
                title = { Text(stringResource(R.string.progress_title), color = OnSurface, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { LevelCard(ui.level, ui.totalXp) }

            if (ui.history.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.stats_latest)) }
                item { HistoryChart(ui.history) }
            }

            if (ui.lineMastery.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.stats_knowledge)) }
                item {
                    Text(
                        stringResource(R.string.stats_mastered_hint),
                        color = OnSurfaceMed, fontSize = 11.sp
                    )
                }
                items(ui.lineMastery.size) { LineMasteryRow(ui.lineMastery[it]) }
            }

            if (ui.mostMissed.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.stats_missed)) }
                items(ui.mostMissed.size) { MissedRow(ui.mostMissed[it]) }
            }

            item { SectionTitle(stringResource(R.string.stats_badges, ui.earnedBadges.count { Badges.byId(it) != null }, Badges.all.size)) }
            items(Badges.all.size) { i ->
                val badge = Badges.all[i]
                BadgeRow(badgeTitle(badge.id), badgeDescription(badge.id), badge.id in ui.earnedBadges)
            }

            if (ui.bestScores.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.stats_best)) }
                items(ui.bestScores.size) { BestScoreCard(ui.bestScores[it]) }
            } else {
                item { Text(stringResource(R.string.stats_empty), color = OnSurfaceMed, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(), color = OnSurfaceMed, fontSize = 12.sp, letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun LevelCard(level: PlayerLevel, totalXp: Int) {
    Surface(shape = RoundedCornerShape(20.dp), color = Plate, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).background(Ink, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("${level.level}", color = Sun, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(levelTitle(level.titleIndex), color = Ink, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("$totalXp XP", color = InkMed, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = level.progress,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = LineB,
                backgroundColor = PlateEdge.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.stats_xp_to_level, level.xpIntoLevel, level.xpForNext, level.level + 1), color = InkMed, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HistoryChart(scores: List<Int>) {
    val max = (scores.maxOrNull() ?: 1).coerceAtLeast(1)
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Canvas(Modifier.fillMaxWidth().height(90.dp)) {
                val gap = 6.dp.toPx()
                val barW = (size.width - gap * (scores.size - 1)) / scores.size
                scores.forEachIndexed { i, score ->
                    val h = size.height * score / max
                    val isLast = i == scores.lastIndex
                    drawRoundRect(
                        color = if (isLast) Sun else LineB.copy(alpha = 0.8f),
                        topLeft = Offset(i * (barW + gap), size.height - h),
                        size = Size(barW, h.coerceAtLeast(2.dp.toPx())),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val avg = scores.average().roundToInt()
            Text(stringResource(R.string.stats_chart, max, avg, scores.last()), color = OnSurfaceMed, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LineMasteryRow(m: LineMastery) {
    val color = Color(m.line.color.toInt())
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        LinePill(m.line.id, color, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                drawRoundRect(OnSurface.copy(alpha = 0.1f), size = size, cornerRadius = CornerRadius(4.dp.toPx()))
                drawRoundRect(color.copy(alpha = 0.35f), size = Size(size.width * m.seenFraction, size.height), cornerRadius = CornerRadius(4.dp.toPx()))
                drawRoundRect(color, size = Size(size.width * m.masteredFraction, size.height), cornerRadius = CornerRadius(4.dp.toPx()))
            }
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.stats_mastery, m.mastered, m.total, m.seen), color = OnSurfaceMed, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MissedRow(m: MissedStation) {
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceHigh, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.station.name, color = OnSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.stats_line, m.lines.joinToString(", ")), color = OnSurfaceMed, fontSize = 11.sp)
            }
            Text(
                stringResource(R.string.stats_accuracy, (m.stat.accuracy * 100).roundToInt(), m.stat.attempts),
                color = if (m.stat.accuracy < 0.5f) Error else Accent, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BadgeRow(title: String, description: String, earned: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            if (earned) Icons.Default.EmojiEvents else Icons.Default.Lock, null,
            tint = if (earned) Accent else OnSurfaceMed.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = if (earned) OnSurface else OnSurfaceMed, fontWeight = FontWeight.SemiBold)
            Text(description, color = OnSurfaceMed, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BestScoreCard(result: GameResult) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceHigh, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Sun, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (result.mode) {
                        GameMode.STATION_SPRINT -> "S"
                        GameMode.DAILY_SPRINT -> "D"
                        GameMode.TRACE_NETWORK -> "T"
                    },
                    color = Ink, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (result.mode) {
                        GameMode.STATION_SPRINT -> "Station Sprint"
                        GameMode.DAILY_SPRINT -> stringResource(R.string.stats_mode_daily)
                        GameMode.TRACE_NETWORK -> "Trace Network"
                    },
                    color = OnSurface, fontWeight = FontWeight.SemiBold
                )
                Text(
                    difficultyLabel(result.difficulty) +
                        (if (result.mode != GameMode.TRACE_NETWORK) " · " + stringResource(R.string.stats_level, result.level) else "") +
                        " · " + SimpleDateFormat("dd/MM/yyyy", locale).format(Date(result.timestampMs)),
                    color = OnSurfaceMed, fontSize = 11.sp
                )
            }
            Text("${result.score}", color = if (result.mode == GameMode.TRACE_NETWORK) Accent else Success, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        }
    }
}
