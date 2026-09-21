package com.triviamap.presentation.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GeoPoint
import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine
import com.triviamap.presentation.common.Background
import com.triviamap.presentation.common.OnSurfaceMed
import com.triviamap.util.GeoBounds
import com.triviamap.util.toNormalized

@Composable
fun SprintCanvas(
    line: TramLine,
    bounds: GeoBounds,
    targetStation: Station?,
    visitedStations: List<Station>,
    sessionStations: List<Station> = emptyList(),
    @Suppress("UNUSED_PARAMETER") difficulty: Difficulty,
    onTap: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val stationRadius = 18f
    val lineColor = Color(line.color.toInt())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(bounds) {
                detectTapGestures { offset ->
                    val nx = offset.x / size.width.toFloat()
                    val ny = offset.y / size.height.toFloat()
                    val x = bounds.minX + nx * (bounds.maxX - bounds.minX)
                    val y = bounds.minY + ny * (bounds.maxY - bounds.minY)
                    onTap(GeoPoint(x, y))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Draw connections for session stations (the cumulative path)
            if (sessionStations.size > 1) {
                for (i in 0 until sessionStations.size - 1) {
                    val p1 = sessionStations[i].position.toNormalized(bounds)
                    val p2 = sessionStations[i + 1].position.toNormalized(bounds)
                    
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f), // Ghostly connection
                        start = Offset(p1.first * size.width, p1.second * size.height),
                        end = Offset(p2.first * size.width, p2.second * size.height),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // The current line's stations are deliberately NOT drawn: their layout would give
            // the answer away. Only the path of previously solved segments is shown.
        }
    }
}
