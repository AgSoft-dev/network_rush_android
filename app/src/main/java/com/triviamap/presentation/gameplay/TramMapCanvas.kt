package com.triviamap.presentation.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GeoPoint
import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine
import com.triviamap.presentation.common.Background
import com.triviamap.presentation.common.OnSurface
import com.triviamap.presentation.common.OnSurfaceMed
import com.triviamap.util.GeoBounds
import com.triviamap.util.smoothPath
import com.triviamap.util.toNormalized

@Composable
fun TramMapCanvas(
    allLines: List<TramLine>,
    activeLineId: String?,
    bounds: GeoBounds,
    playerPaths: Map<String, List<GeoPoint>>,
    visitedStations: Map<String, List<Station>>,
    snapIndex: Int,
    difficulty: Difficulty,
    showGhost: Boolean,
    onGeoPoint: (GeoPoint) -> Unit,
    onDrawEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val stationRadius = 14f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(bounds) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(0.5f, 10f)
                    val zoomFactor = scale / oldScale
                    offsetX = (offsetX - centroid.x) * zoomFactor + centroid.x + pan.x
                    offsetY = (offsetY - centroid.y) * zoomFactor + centroid.y + pan.y
                }
            }
            .pointerInput(bounds, activeLineId, scale, offsetX, offsetY) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.size == 1) {
                            val change = event.changes.first()
                            if (event.type == PointerEventType.Move && change.pressed) {
                                change.consume()
                                val px = change.position.x
                                val py = change.position.y
                                val cx = (px - offsetX) / scale
                                val cy = (py - offsetY) / scale
                                val nx = cx / size.width.toFloat()
                                val ny = cy / size.height.toFloat()
                                val x = bounds.minX + nx * (bounds.maxX - bounds.minX)
                                val y = bounds.minY + ny * (bounds.maxY - bounds.minY)
                                onGeoPoint(GeoPoint(x, y))
                            } else if (event.type == PointerEventType.Release) {
                                onDrawEnd()
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            withTransform({
                translate(offsetX, offsetY)
                scale(scale, scale, pivot = Offset(0f, 0f))
            }) {
                allLines.forEach { line ->
                    val isLineActive = line.id == activeLineId
                    val lineColor = Color(line.color.toInt())
                    val path = playerPaths[line.id] ?: emptyList()
                    val visited = visitedStations[line.id] ?: emptyList()

                    if (showGhost) {
                        drawReferencePath(line.geometry, bounds, OnSurfaceMed.copy(alpha = 0.08f))
                    }

                    if (path.size >= 2) {
                        drawPlayerPath(path, bounds, lineColor.copy(alpha = if (isLineActive) 1f else 0.4f))
                    }

                    line.stations.forEachIndexed { idx, station ->
                        val (nx, ny) = station.position.toNormalized(bounds)
                        val sx = nx * size.width
                        val sy = ny * size.height
                        val isVisited = visited.any { it.id == station.id }
                        val isSnapping = isLineActive && idx == snapIndex

                        drawCircle(
                            color = if (isVisited) lineColor else OnSurfaceMed.copy(alpha = 0.3f),
                            radius = if (isSnapping) stationRadius * 1.6f else stationRadius,
                            center = Offset(sx, sy),
                            style = Stroke(width = 2.5f)
                        )
                        
                        drawCircle(
                            color = if (isVisited) lineColor.copy(alpha = 0.2f) else Background,
                            radius = if (isSnapping) stationRadius * 1.6f - 2.5f else stationRadius - 2.5f,
                            center = Offset(sx, sy)
                        )

                        val shouldShowName = when (difficulty) {
                            Difficulty.EASY -> isVisited
                            Difficulty.MEDIUM -> true
                            Difficulty.HARD -> false
                        }

                        if (shouldShowName) {
                            drawContext.canvas.nativeCanvas.drawText(
                                station.name,
                                sx + stationRadius + 6f,
                                sy + 5f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 18f
                                    alpha = if (isLineActive) 255 else 100
                                    isAntiAlias = true
                                    setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawReferencePath(geometry: List<GeoPoint>, bounds: GeoBounds, color: Color) {
    if (geometry.size < 2) return
    val points = geometry.map { gp ->
        val (nx, ny) = gp.toNormalized(bounds)
        Offset(nx * size.width, ny * size.height)
    }
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (p in points.drop(1)) lineTo(p.x, p.y)
    }
    drawPath(path, color = color, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawPlayerPath(geoPath: List<GeoPoint>, bounds: GeoBounds, color: Color) {
    val screen = geoPath.map { gp ->
        val (nx, ny) = gp.toNormalized(bounds)
        nx to ny
    }
    val segments = smoothPath(screen)
    val path = Path()
    segments.forEachIndexed { i, seg ->
        if (i == 0) path.moveTo(seg.p0x * size.width, seg.p0y * size.height)
        path.cubicTo(
            seg.c1x * size.width, seg.c1y * size.height,
            seg.c2x * size.width, seg.c2y * size.height,
            seg.p1x * size.width, seg.p1y * size.height
        )
    }
    drawPath(path, color = color.copy(alpha = 0.2f), style = Stroke(width = 18f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color = color, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
