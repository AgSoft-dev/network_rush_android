package com.triviamap.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triviamap.domain.model.GameMode
import com.triviamap.domain.model.GameResult
import com.triviamap.domain.repository.GameResultRepository
import com.triviamap.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    resultRepository: GameResultRepository
) : ViewModel() {
    val bestScores = resultRepository.getBestScores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    vm: StatsViewModel = hiltViewModel()
) {
    val scores by vm.bestScores.collectAsState()

    Scaffold(
        backgroundColor = Background,
        topBar = {
            TopAppBar(
                backgroundColor = Background,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
                    }
                },
                title = {
                    Text("Statistics", color = OnSurface, fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { padding ->
        if (scores.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No games yet!", color = OnSurfaceMed)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Best scores",
                        color = OnSurfaceMed,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(scores, key = { "${it.mode}-${it.difficulty}" }) { result ->
                    BestScoreCard(result)
                }
            }
        }
    }
}

@Composable
private fun BestScoreCard(result: GameResult) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(Primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (result.mode == GameMode.STATION_SPRINT) "S" else "T", color = Primary, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (result.mode == GameMode.STATION_SPRINT) "Station Sprint" else "Trace Network", color = OnSurface, fontWeight = FontWeight.SemiBold)
                Text(
                    result.difficulty.name.lowercase().replaceFirstChar { it.uppercase() } +
                            (if (result.mode == GameMode.STATION_SPRINT) " · level ${result.level}" else "") +
                            " · " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(Date(result.timestampMs)),
                    color = OnSurfaceMed,
                    fontSize = 11.sp
                )
            }
            Text(
                "${result.score}",
                color = scoreColor(result.score),
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )
        }
    }
}

private fun scoreColor(score: Int) = when {
    score >= 5000 -> Success
    score >= 2000 -> Accent
    else -> Error
}
