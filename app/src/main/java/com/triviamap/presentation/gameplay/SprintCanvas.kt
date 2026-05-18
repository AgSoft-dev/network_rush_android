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
    difficulty: Difficulty,
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
            line.stations.forEach { station ->
                val (nx, ny) = station.position.toNormalized(bounds)
                val sx = nx * size.width
                val sy = ny * size.height
                
                val isVisited = visitedStations.any { it.id == station.id }
                val isTarget = targetStation?.id == station.id
                
                // Ring
                drawCircle(
                    color = if (isVisited) lineColor else if (isTarget) Color.White else OnSurfaceMed.copy(alpha = 0.2f),
                    radius = if (isTarget) stationRadius * 1.5f else stationRadius,
                    center = Offset(sx, sy),
                    style = Stroke(width = 3f)
                )
                
                // Fill
                if (isVisited) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.4f),
                        radius = stationRadius - 3f,
                        center = Offset(sx, sy)
                    )
                }
            }
        }
    }
}
