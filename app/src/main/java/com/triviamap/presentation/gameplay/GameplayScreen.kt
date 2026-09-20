package com.triviamap.presentation.gameplay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.TramLine
import com.triviamap.presentation.common.*
import java.util.Locale

@Composable
fun GameplayScreen(
    difficulty: Difficulty,
    onFinished: (Int) -> Unit,
    onBack: () -> Unit,
    vm: GameplayViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.phase) {
        if (state.phase is GamePhase.Finished) {
            val score = (state.phase as GamePhase.Finished).breakdown.total
            onFinished(score)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        when (val phase = state.phase) {
            GamePhase.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            GamePhase.Drawing, GamePhase.Validating -> {
                val bounds = state.bounds ?: return@Box

                TramMapCanvas(
                    allLines = state.allLines,
                    activeLineId = state.activeLineId,
                    bounds = bounds,
                    playerPaths = state.playerPaths,
                    visitedStations = state.visitedStations,
                    snapIndex = state.snapIndex,
                    difficulty = difficulty,
                    showGhost = difficulty == Difficulty.EASY,
                    onGeoPoint = vm::onDraw,
                    onDrawEnd = vm::onDrawEnd,
                    modifier = Modifier.fillMaxSize()
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    TopHud(
                        difficulty = difficulty,
                        elapsedMs = state.elapsedMs,
                        onBack = onBack
                    )
                    
                    Spacer(Modifier.weight(1f))
                    
                    LineSelector(
                        lines = state.allLines,
                        activeLineId = state.activeLineId,
                        onSelect = vm::setActiveLine
                    )

                    BottomHud(
                        isValidating = phase is GamePhase.Validating,
                        hasPath = (state.playerPaths[state.activeLineId]?.size ?: 0) >= 2,
                        onValidate = vm::validate,
                        onClear = vm::clearDrawing
                    )
                }
            }

            is GamePhase.Finished -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
        }
    }
}

@Composable
private fun TopHud(
    difficulty: Difficulty,
    elapsedMs: Long,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Surface.copy(alpha = 0.85f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
        }

        Spacer(Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Surface.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Strasbourg Network", color = OnSurface, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    formatElapsed(elapsedMs),
                    color = if (difficulty == Difficulty.HARD) Error else OnSurfaceMed,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun LineSelector(
    lines: List<TramLine>,
    activeLineId: String?,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(lines) { line ->
            val isActive = line.id == activeLineId
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(line.id) },
                color = if (isActive) Color(line.color.toInt()) else SurfaceHigh,
                shape = CircleShape,
                elevation = if (isActive) 4.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = line.id,
                        color = if (isActive) Color(line.textColor.toInt()) else OnSurfaceMed,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomHud(
    isValidating: Boolean,
    hasPath: Boolean,
    onValidate: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.height(52.dp),
            enabled = hasPath && !isValidating,
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfaceMed)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("CLEAR")
        }

        Button(
            onClick = onValidate,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            enabled = !isValidating,
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = Color.White),
            elevation = ButtonDefaults.elevation(6.dp)
        ) {
            if (isValidating) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Check, contentDescription = "Validate", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("VALIDATE NETWORK", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.getDefault(), "%d:%02d", s / 60, s % 60)
}
