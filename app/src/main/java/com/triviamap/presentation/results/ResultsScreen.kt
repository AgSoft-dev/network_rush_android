package com.triviamap.presentation.results

import com.triviamap.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.triviamap.domain.model.GameMode
import com.triviamap.domain.progress.RunSummary
import com.triviamap.domain.progress.RunSummaryHolder
import com.triviamap.domain.progress.ShareText
import com.triviamap.presentation.common.*
import com.triviamap.util.shareText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ResultsViewModel @Inject constructor(holder: RunSummaryHolder) : ViewModel() {
    val summary: StateFlow<RunSummary?> = holder.summary
}

@Composable
fun ResultsScreen(
    mode: GameMode,
    score: Int,
    onHome: () -> Unit,
    onRetry: (RunSummary?) -> Unit,
    vm: ResultsViewModel = hiltViewModel()
) {
    // Sprint runs publish a full summary; Trace only passes its score
    val summary = if (mode == GameMode.TRACE_NETWORK) null else vm.summary.collectAsState().value
    val context = LocalContext.current

    var scoreAnimationTriggered by remember { mutableStateOf(false) }
    val animatedScore by animateIntAsState(
        targetValue = if (scoreAnimationTriggered) score else 0,
        animationSpec = tween(durationMillis = 2000, easing = EaseOutExpo),
        label = "score"
    )
    LaunchedEffect(Unit) { scoreAnimationTriggered = true }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Text(
                text = when (mode) {
                    GameMode.STATION_SPRINT -> stringResource(R.string.results_mode_sprint)
                    GameMode.DAILY_SPRINT -> stringResource(R.string.home_daily_title)
                    GameMode.TRACE_NETWORK -> "NETWORK TRACE"
                },
                color = OnSurfaceMed, letterSpacing = 4.sp, fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))

            Box(contentAlignment = Alignment.TopCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = animatedScore.toString(),
                        color = scoreColor(mode, score),
                        fontFamily = DisplayFont,
                        fontSize = 88.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (mode == GameMode.TRACE_NETWORK) "/ 1000" else stringResource(R.string.results_total),
                        color = OnSurfaceMed,
                        fontSize = if (mode == GameMode.TRACE_NETWORK) 20.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (mode == GameMode.TRACE_NETWORK) 0.sp else 2.sp
                    )
                }
                if (summary?.isNewRecord == true && animatedScore == score) {
                    NewRecordBadge(Modifier.padding(top = 10.dp))
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = scoreLabel(mode, score),
                color = scoreColor(mode, score),
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            if (summary != null) {
                Spacer(Modifier.height(28.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(stringResource(R.string.results_levels), summary.level.toString())
                    StatItem(stringResource(R.string.results_max_combo), summary.maxCombo.toString())
                    StatItem(stringResource(R.string.results_accuracy), "${(summary.accuracy * 100).roundToInt()}%")
                }

                Spacer(Modifier.height(24.dp))
                XpCard(summary)

                if (summary.newBadges.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    summary.newBadges.forEach { badge ->
                        Surface(
                            shape = RoundedCornerShape(18.dp), color = Surface,
                            border = BorderStroke(2.dp, Sun),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, null, tint = Accent, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.results_badge, badgeTitle(badge.id)), color = OnSurface, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold)
                                    Text(badgeDescription(badge.id), color = OnSurfaceMed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (summary.streak > 0) {
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Whatshot, null, tint = Accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.results_streak, summary.streak), color = OnSurface, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            if (summary != null) {
                Button(
                    onClick = {
                        shareText(context, buildShareMessage(
                            context, isDaily = summary.mode == GameMode.DAILY_SPRINT,
                            difficultyName = summary.difficulty.name,
                            epochDay = summary.epochDay,
                            level = summary.level,
                            score = summary.score,
                            maxCombo = summary.maxCombo,
                            answerLog = summary.answerLog
                        ))
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Sun, contentColor = Ink)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.results_share), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(12.dp))
            }

            // The daily challenge is a single attempt per day
            if (mode != GameMode.DAILY_SPRINT) {
                Button(
                    onClick = { onRetry(summary) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Surface, contentColor = OnSurface)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.results_play_again), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.results_home), letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun XpCard(summary: RunSummary) {
    val after = summary.levelAfter
    val leveledUp = after.level > summary.levelBefore.level
    Surface(shape = RoundedCornerShape(20.dp), color = Plate, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Ink, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${after.level}", color = Sun, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    levelTitle(after.titleIndex).uppercase(),
                    color = Ink, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.weight(1f)
                )
                Text("+${summary.xpEarned} XP", color = Ink, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = after.progress,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = LineB,
                backgroundColor = PlateEdge.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (leveledUp) stringResource(R.string.xp_level_up) else stringResource(R.string.xp_to_next, after.xpIntoLevel, after.xpForNext),
                color = if (leveledUp) SunEdge else InkMed,
                fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NewRecordBadge(modifier: Modifier = Modifier) {
    Surface(color = Accent, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = Ink, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("NEW RECORD", color = Ink, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = OnSurfaceMed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = OnSurface, fontFamily = DisplayFont, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun scoreColor(mode: GameMode, score: Int) = when {
    mode != GameMode.TRACE_NETWORK -> Accent
    score >= 800 -> Success
    score >= 500 -> Accent
    else -> Error
}

@Composable
private fun scoreLabel(mode: GameMode, score: Int) = when {
    mode != GameMode.TRACE_NETWORK -> when {
        score <= 0 -> stringResource(R.string.label_try_again)
        score > 5000 -> stringResource(R.string.label_legendary)
        score > 2500 -> stringResource(R.string.label_elite)
        score > 1000 -> stringResource(R.string.label_speedster)
        else -> stringResource(R.string.label_good_run)
    }
    score >= 900 -> "PERFECT"
    score >= 700 -> "GREAT"
    score >= 500 -> "GOOD"
    score >= 300 -> "KEEP TRYING"
    else -> "MISSED IT"
}

val EaseOutExpo = Easing { fraction ->
    if (fraction == 1f) 1f else 1f - Math.pow(2.0, -10.0 * fraction).toFloat()
}
