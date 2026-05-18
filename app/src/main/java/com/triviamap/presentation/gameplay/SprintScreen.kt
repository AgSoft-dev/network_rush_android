package com.triviamap.presentation.gameplay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.triviamap.domain.model.Difficulty
import com.triviamap.presentation.common.*
import java.util.Locale

@Composable
fun SprintScreen(
    difficulty: Difficulty,
    onFinished: (Int) -> Unit,
    onBack: () -> Unit,
    vm: SprintViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.phase) {
        if (state.phase is GamePhase.Finished) {
            onFinished(state.score)
        }
    }

    Scaffold(
        backgroundColor = Background,
        topBar = {
            SprintTopBar(
                elapsedMs = state.elapsedMs,
                onBack = onBack
            )
        },
        bottomBar = {
            if (state.phase is GamePhase.Drawing) {
                SubmitSection(onSubmit = vm::submit)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (state.phase) {
                GamePhase.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary)
                }
                GamePhase.Drawing, GamePhase.Validating -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Reorder the stations",
                            color = OnSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap to select, then tap target position to swap",
                            color = OnSurfaceMed,
                            fontSize = 14.sp
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        TileList(
                            tiles = state.currentTiles,
                            onMove = vm::moveTile
                        )
                    }
                }
                else -> {}
            }
            
            if (state.phase is GamePhase.Validating) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SprintTopBar(elapsedMs: Long, onBack: () -> Unit) {
    TopAppBar(
        backgroundColor = Background,
        elevation = 0.dp,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = OnSurface)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Station Sprint", color = OnSurface, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text(
                    formatElapsed(elapsedMs),
                    color = Accent,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(16.dp))
            }
        }
    )
}

@Composable
private fun TileList(
    tiles: List<com.triviamap.domain.model.Station>,
    onMove: (Int, Int) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        itemsIndexed(tiles) { index, station ->
            val isSelected = selectedIndex == index
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .let { 
                        if (selectedIndex == null) {
                            it.background(SurfaceHigh)
                        } else if (isSelected) {
                            it.background(Brush.horizontalGradient(listOf(Primary, Accent)))
                        } else {
                            it.background(SurfaceHigh)
                        }
                    }
                    .clickable {
                        if (selectedIndex == null) {
                            selectedIndex = index
                        } else {
                            if (selectedIndex != index) {
                                onMove(selectedIndex!!, index)
                            }
                            selectedIndex = null
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        color = if (isSelected) Color.White else OnSurfaceMed,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        text = station.name,
                        color = if (isSelected) Color.White else OnSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = null,
                        tint = if (isSelected) Color.White.copy(alpha = 0.6f) else OnSurfaceMed.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmitSection(onSubmit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Surface,
        elevation = 16.dp
    ) {
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = Color.White)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("CHECK ORDER", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val s = ms / 1000
    val m = ms % 1000 / 10
    return String.format(Locale.getDefault(), "%02d:%02d", s, m)
}
