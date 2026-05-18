package com.triviamap.presentation.lineselection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.TramLine
import com.triviamap.domain.usecase.GetAllLinesUseCase
import com.triviamap.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LineSelectionViewModel @Inject constructor(
    getAllLines: GetAllLinesUseCase
) : ViewModel() {
    val lines = getAllLines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun LineSelectionScreen(
    difficulty: Difficulty,
    onLineSelected: (String) -> Unit,
    onBack: () -> Unit,
    vm: LineSelectionViewModel = hiltViewModel()
) {
    val lines by vm.lines.collectAsState()

    Scaffold(
        backgroundColor = Background,
        topBar = {
            TopAppBar(
                backgroundColor = Background,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurface)
                    }
                },
                title = {
                    Column {
                        Text("Select a line", color = OnSurface, fontWeight = FontWeight.Bold)
                        Text(
                            text = difficulty.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            color = difficultyColor(difficulty),
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (lines.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(lines, key = { it.id }) { line ->
                    LineCard(line = line, onClick = { onLineSelected(line.id) })
                }
            }
        }
    }
}

@Composable
private fun LineCard(line: TramLine, onClick: () -> Unit) {
    val lineColor = Color(line.color.toInt())
    val textColor = Color(line.textColor.toInt())
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Line badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(lineColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = line.id,
                    color = textColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(line.name, color = OnSurface, fontWeight = FontWeight.SemiBold)
                Text(
                    "${line.stations.size} stations · ${line.geometry.size} pts",
                    color = OnSurfaceMed,
                    fontSize = 12.sp
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceMed)
        }
    }
}

private fun difficultyColor(d: Difficulty) = when (d) {
    Difficulty.EASY   -> Success
    Difficulty.MEDIUM -> Accent
    Difficulty.HARD   -> Error
}
