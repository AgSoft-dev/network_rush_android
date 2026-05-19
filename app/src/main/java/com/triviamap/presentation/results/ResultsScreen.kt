package com.triviamap.presentation.results

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
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

@Composable
fun ResultsScreen(
    mode: GameMode,
    score: Int,
    onHome: () -> Unit,
    onRetry: () -> Unit
) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1200, easing = EaseOut),
        label = "score"
    )

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

            Spacer(Modifier.height(4.dp))

            Text(
                text = scoreLabel(mode, score),
                color = scoreColor(mode, score),
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

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
