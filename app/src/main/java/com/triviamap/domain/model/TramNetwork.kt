package com.triviamap.domain.model

/**
 * A geographic coordinate or screen-space point (x, y).
 */
data class GeoPoint(val x: Double, val y: Double)

/**
 * A named tram station.
 */
data class Station(
    val id: String,
    val name: String,
    val position: GeoPoint,
    val lines: List<String> = emptyList()
)

/**
 * A full tram line definition with its polyline geometry and ordered station list.
 */
data class TramLine(
    val id: String,
    val name: String,          // e.g. "A", "B", "C"
    val color: Long,           // 0xAARRGGBB
    val textColor: Long = 0xFFFFFFFFL,
    val geometry: List<GeoPoint>,
    val stations: List<Station>
)
