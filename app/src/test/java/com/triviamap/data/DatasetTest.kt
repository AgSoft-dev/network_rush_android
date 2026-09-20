package com.triviamap.data

import com.triviamap.data.model.GeoJsonParser
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Sanity checks on the bundled network data. */
class DatasetTest {
    private val lines = GeoJsonParser.parseData(File("src/main/assets/strasbourg_stations.json").readText())

    @Test fun hasLinesWithEnoughStations() {
        assertTrue(lines.isNotEmpty())
        lines.forEach { assertTrue("line ${it.id}", it.stations.size >= 3) }
    }

    @Test fun stationIdsAreUniquePerLine() {
        lines.forEach { l -> assertEquals(l.id, l.stations.size, l.stations.map { it.id }.toSet().size) }
    }

    @Test fun sharedStationIdsAreDetectedAsInterchanges() {
        val shared = lines.flatMap { l -> l.stations.map { it.id } }.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue(shared.isNotEmpty())
        lines.flatMap { it.stations }.filter { it.id in shared }.forEach {
            assertEquals(shared.getValue(it.id), it.lines.size)
        }
        lines.flatMap { it.stations }.filter { it.id !in shared }.forEach { assertEquals(1, it.lines.size) }
    }
}
