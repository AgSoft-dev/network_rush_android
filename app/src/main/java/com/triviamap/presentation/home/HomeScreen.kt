package com.triviamap.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Map
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triviamap.BuildConfig
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GameMode
import com.triviamap.presentation.common.*

@Composable
fun HomeScreen(
    onPlay: (GameMode, Difficulty) -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit
) {
    var pendingMode by remember { mutableStateOf<GameMode?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        AnimatedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TRIVIAMAP",
                color = OnSurface,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Strasbourg Tram Challenge",
                color = OnSurfaceMed,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(48.dp))

            // Trace Network is parked until its scoring is reworked: dev builds only
            if (BuildConfig.DEBUG) {
                ModeButton(
                    title = "TRACE NETWORK (DEV)",
                    icon = Icons.Default.Map,
                    color = Primary,
                    onClick = { pendingMode = GameMode.TRACE_NETWORK }
                )

                Spacer(Modifier.height(16.dp))
            }

            ModeButton(
                title = "STATION SPRINT",
                icon = Icons.Default.ElectricBolt,
                color = Accent,
                onClick = { pendingMode = GameMode.STATION_SPRINT }
            )

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = onStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = ButtonDefaults.outlinedBorder.copy(
                    brush = Brush.horizontalGradient(listOf(Primary, Accent))
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
            ) {
                Text("STATISTICS", fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onSettings) {
                Text("SETTINGS", color = OnSurfaceMed, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            }
        }

        Text(
            text = "v0.3 · Station Sprint Edition",
            color = OnSurfaceMed.copy(alpha = 0.4f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (pendingMode != null) {
        DifficultyPickerDialog(
            onSelect = { difficulty ->
                val mode = pendingMode!!
                pendingMode = null
                onPlay(mode, difficulty)
            },
            onDismiss = { pendingMode = null }
        )
    }
}

@Composable
private fun ModeButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.elevation(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 16.sp)
        }
    }
}

@Composable
private fun DifficultyPickerDialog(
    onSelect: (Difficulty) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Surface,
        contentColor = OnSurface,
        title = { Text("Select Difficulty", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DifficultyOption("Easy", "60 s clock · +12 s per correct answer", Success) { onSelect(Difficulty.EASY) }
                DifficultyOption("Medium", "45 s clock · +10 s per correct answer", Accent) { onSelect(Difficulty.MEDIUM) }
                DifficultyOption("Hard", "30 s clock · +8 s per correct answer", Error) { onSelect(Difficulty.HARD) }
            }
        },
        buttons = {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceMed) }
            }
        }
    )
}

@Composable
private fun DifficultyOption(title: String, desc: String, color: Color, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceHigh, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(color, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(title, color = OnSurface, fontWeight = FontWeight.SemiBold)
                    Text(desc, color = OnSurfaceMed, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AnimatedBackground() {
    val transition = rememberInfiniteTransition(label = "bg")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(12_000, easing = LinearEasing)),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineColors = listOf(Color(0x22E2001A), Color(0x220065BD), Color(0x2282368C), Color(0x22009A44), Color(0x22FF8200))
        lineColors.forEachIndexed { i, color ->
            val y = size.height * (0.2f + i * 0.15f)
            val offset = (phase + i * 0.2f) % 1f * size.width * 0.5f
            drawLine(color = color, start = Offset(-offset, y), end = Offset(size.width - offset + 100f, y), strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
}
