package com.triviamap.presentation.results

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triviamap.domain.model.GameMode
import com.triviamap.presentation.common.*
import kotlin.math.roundToInt

@Composable
fun ResultsScreen(
    mode: GameMode,
    score: Int,
    accuracy: Float = 0f,
    level: Int = 0,
    maxCombo: Int = 0,
    isNewRecord: Boolean = false,
    streak: Int = 0,
    onHome: () -> Unit,
    onRetry: () -> Unit
) {
    var scoreAnimationTriggered by remember { mutableStateOf(false) }
    
    val animatedScore by animateIntAsState(
        targetValue = if (scoreAnimationTriggered) score else 0,
        animationSpec = tween(durationMillis = 2000, easing = EaseOutExpo),
        label = "score"
    )

    LaunchedEffect(Unit) {
        scoreAnimationTriggered = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (mode == GameMode.STATION_SPRINT) "STATION SPRINT" else "NETWORK TRACE",
                color = OnSurfaceMed,
                letterSpacing = 4.sp,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(8.dp))

            // Score
            Box(contentAlignment = Alignment.TopCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = animatedScore.toString(),
                        color = scoreColor(mode, score),
                        fontSize = 88.sp,
                        fontWeight = FontWeight.Black
                    )
                    
                    if (mode == GameMode.TRACE_NETWORK) {
                        Text(
                            text = "/ 1000",
                            color = OnSurfaceMed,
                            fontSize = 20.sp
                        )
                    } else {
                        Text(
                            text = "TOTAL POINTS",
                            color = OnSurfaceMed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }

                if (isNewRecord && animatedScore == score) {
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

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (mode == GameMode.STATION_SPRINT) {
                    StatItem("LEVELS", level.toString())
                    StatItem("MAX COMBO", maxCombo.toString())
                    StatItem("ACCURACY", "${(accuracy * 100).roundToInt()}%")
                } else {
                    StatItem("ACCURACY", "${(accuracy * 100).roundToInt()}%")
                }
            }

            if (streak > 0) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Whatshot, null, tint = Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$streak DAY STREAK",
                        color = OnSurface,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Action buttons
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("PLAY AGAIN", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("HOME", letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun NewRecordBadge(modifier: Modifier = Modifier) {
    Surface(
        color = Accent,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, null, tint = Color.Black, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("NEW RECORD", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = OnSurfaceMed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            color = OnSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
    }
}

private fun scoreColor(mode: GameMode, score: Int) = when {
    mode == GameMode.STATION_SPRINT -> Accent
    score >= 800 -> Success
    score >= 500 -> Accent
    else -> Error
}

private fun scoreLabel(mode: GameMode, score: Int) = when {
    mode == GameMode.STATION_SPRINT -> {
        when {
            score > 5000 -> "LEGENDARY"
            score > 2500 -> "ELITE"
            score > 1000 -> "SPEEDSTER"
            else -> "GOOD RUN"
        }
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
