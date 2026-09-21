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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.progress.ShareText
import com.triviamap.util.shareText
import com.triviamap.domain.model.GameMode
import com.triviamap.presentation.common.*

@Composable
fun HomeScreen(
    onPlay: (GameMode, Difficulty) -> Unit,
    onDevSprint: (Difficulty, Int, String) -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val ui by vm.state.collectAsState()
    val context = LocalContext.current
    var pendingMode by remember { mutableStateOf<GameMode?>(null) }
    var showDev by remember { mutableStateOf(false) }

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
                fontFamily = DisplayFont,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Strasbourg Tram Challenge",
                color = OnSurfaceMed,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ui.isSupporter) Text("\u2665 ", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(
                    "LV ${ui.level.level} · ${ui.level.title.uppercase()}",
                    color = OnSurfaceMed, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
                if (ui.streak > 0) {
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.Whatshot, null, tint = Accent, modifier = Modifier.size(16.dp))
                    Text("${ui.streak}", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Trace Network is parked until its scoring is reworked: dev builds only
            if (BuildConfig.DEBUG) {
                ModeButton(
                    title = "TRACE NETWORK (DEV)",
                    icon = Icons.Default.Map,
                    color = Surface,
                    contentColor = OnSurface,
                    onClick = { pendingMode = GameMode.TRACE_NETWORK }
                )

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showDev = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) { Text("DEV · SPRINT VARIANTS", color = Accent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                Spacer(Modifier.height(16.dp))
            }

            SprintHero(onGo = { pendingMode = GameMode.STATION_SPRINT })

            Spacer(Modifier.height(16.dp))

            val daily = ui.dailyToday
            if (daily == null) {
                ModeButton(
                    title = "DAILY CHALLENGE",
                    icon = Icons.Default.CalendarToday,
                    color = Surface,
                    contentColor = OnSurface,
                    onClick = { onPlay(GameMode.DAILY_SPRINT, Difficulty.MEDIUM) }
                )
                Text(
                    "Same puzzles for everyone, one attempt a day",
                    color = OnSurfaceMed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Surface(shape = RoundedCornerShape(20.dp), color = Ticket, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("TODAY'S DAILY · VALIDATED", color = Color(0xFF008A2E), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                            Text("${daily.score} pts · level ${daily.level}", color = Ink, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("Come back tomorrow for a new one", color = InkMed, fontSize = 11.sp)
                        }
                        IconButton(onClick = {
                            shareText(context, ShareText.build(
                                isDaily = true, difficultyName = daily.difficulty.name, epochDay = ui.epochDay,
                                level = daily.level, score = daily.score, maxCombo = daily.maxCombo, answerLog = daily.answerLog
                            ))
                        }) { Icon(Icons.Default.Share, contentDescription = "Share", tint = Ink) }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = onStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = ButtonDefaults.outlinedBorder.copy(
                    brush = Brush.horizontalGradient(listOf(LineB, LineA, LineD))
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface)
            ) {
                Text("PROGRESS", fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onSettings) {
                Text("SETTINGS", color = OnSurfaceMed, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "v0.3 · Station Sprint Edition",
                color = OnSurfaceMed.copy(alpha = 0.4f),
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Only place ads appear in the whole app; never during a run
            if (ui.showBanner) AdBanner()
        }
    }

    if (BuildConfig.DEBUG && showDev) {
        DevSprintDialog(
            onStart = { d, l, f -> showDev = false; onDevSprint(d, l, f) },
            onDismiss = { showDev = false }
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
    onClick: () -> Unit,
    contentColor: Color = Ink
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color, contentColor = contentColor),
        elevation = ButtonDefaults.elevation(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontSize = 16.sp)
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
        shape = RoundedCornerShape(24.dp),
        title = { Text("Select difficulty", fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DifficultyOption("Easy", "60 s clock · the most time back per answer", Success) { onSelect(Difficulty.EASY) }
                DifficultyOption("Medium", "45 s clock · balanced", Sun) { onSelect(Difficulty.MEDIUM) }
                DifficultyOption("Hard", "30 s clock · reversed lines, gaps, less time back", Error) { onSelect(Difficulty.HARD) }
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
    Surface(shape = RoundedCornerShape(18.dp), color = Plate, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).background(color, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(title, color = Ink, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text(desc, color = InkMed, fontSize = 11.sp)
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

/** Debug-only: launch a Sprint with a forced challenge type and starting level (stages: 1, 6, 11, 16, 21+). */
@Composable
private fun DevSprintDialog(
    onStart: (Difficulty, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var level by remember { mutableStateOf(1) }
    var force by remember { mutableStateOf("") }

    @Composable
    fun <T> Choices(label: String, options: List<Pair<String, T>>, selected: T, onPick: (T) -> Unit) {
        Text(label, color = OnSurfaceMed, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (text, value) ->
                val on = value == selected
                OutlinedButton(
                    onClick = { onPick(value) },
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (on) Accent else Color.Transparent,
                        contentColor = if (on) Ink else OnSurface
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text(text, fontSize = 11.sp, maxLines = 1) }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Surface,
        contentColor = OnSurface,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Dev · Sprint variants", fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Choices("Challenge type", listOf("Auto" to "", "Reorder" to "REORDER", "Classify" to "CLASSIFY", "Burst" to "SPEED_BURST"), force) { force = it }
                Choices("Start level (tiles / stage)", listOf("1" to 1, "6" to 6, "11" to 11, "16" to 16, "21" to 21, "40" to 40), level) { level = it }
                Choices("Difficulty", listOf("Easy" to Difficulty.EASY, "Medium" to Difficulty.MEDIUM, "Hard" to Difficulty.HARD), difficulty) { difficulty = it }
                Text("Runs still count toward stats/XP.", color = OnSurfaceMed, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
        },
        buttons = {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceMed) }
                TextButton(onClick = { onStart(difficulty, level, force) }) { Text("START", color = Accent, fontWeight = FontWeight.Black) }
            }
        }
    )
}

/** Main entry: a white signage plate with the route dots and the sun-yellow "go" bar. */
@Composable
private fun SprintHero(onGo: () -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = Plate, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(LineA, LineB, LineD).forEachIndexed { i, c ->
                    if (i > 0) Box(Modifier.width(22.dp).height(4.dp).background(Border, RoundedCornerShape(2.dp)))
                    Box(Modifier.size(14.dp).background(c, CircleShape))
                }
                Box(Modifier.width(22.dp).height(4.dp).background(Border, RoundedCornerShape(2.dp)))
                Box(Modifier.size(14.dp).background(Ink, CircleShape))
            }
            Spacer(Modifier.height(10.dp))
            Text("Station Sprint", color = Ink, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
            Text("Put the stops back in order before the terminus.", color = InkMed, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onGo,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Sun, contentColor = Ink),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
            ) {
                Text("GO", fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = 2.sp)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.ElectricBolt, null, modifier = Modifier.size(26.dp))
            }
        }
    }
}
