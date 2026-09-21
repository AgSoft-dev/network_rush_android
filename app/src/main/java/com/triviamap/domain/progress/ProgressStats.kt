package com.triviamap.domain.progress

import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine

data class LineMastery(val line: TramLine, val mastered: Int, val seen: Int, val total: Int) {
    val masteredFraction: Float get() = if (total == 0) 0f else mastered.toFloat() / total
    val seenFraction: Float get() = if (total == 0) 0f else seen.toFloat() / total
}

data class MissedStation(val station: Station, val stat: StationStat, val lines: List<String>)

/** Pure aggregations behind the Progress screen. */
object ProgressStats {

    fun lineMastery(lines: List<TramLine>, stats: Map<String, StationStat>): List<LineMastery> =
        lines.map { line ->
            val ids = line.stations.map { it.id }
            LineMastery(
                line = line,
                mastered = ids.count { stats[it]?.isMastered == true },
                seen = ids.count { (stats[it]?.attempts ?: 0) > 0 },
                total = ids.size
            )
        }

    /** Stations with the lowest accuracy (at least [minAttempts] tries), worst first. */
    fun mostMissed(
        lines: List<TramLine>,
        stats: Map<String, StationStat>,
        minAttempts: Int = 3,
        limit: Int = 8
    ): List<MissedStation> {
        val byId = lines.flatMap { it.stations }.associateBy { it.id }
        return stats.values
            .filter { it.attempts >= minAttempts && it.accuracy < 1f }
            .sortedWith(compareBy<StationStat> { it.accuracy }.thenByDescending { it.attempts })
            .take(limit)
            .mapNotNull { st -> byId[st.stationId]?.let { MissedStation(it, st, it.lines) } }
    }

    fun hasMasteredLine(lines: List<TramLine>, stats: Map<String, StationStat>): Boolean =
        lineMastery(lines, stats).any { it.total > 0 && it.mastered == it.total }

    fun totalPlacements(stats: Map<String, StationStat>): Int = stats.values.sumOf { it.attempts }
}
