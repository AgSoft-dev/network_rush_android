package com.triviamap.util

import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GeoPoint
import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ScoreBreakdown(
    val stationOrder: Float,   // 0..1
    val completion: Float,     // 0..1
    val speedBonus: Float,     // 0..1
    val total: Int             // 0..1000
)

object ScoringEngine {

    private fun timeLimitMs(difficulty: Difficulty): Long = when (difficulty) {
        Difficulty.EASY -> 300_000L    // 5 mins
        Difficulty.MEDIUM -> 240_000L  // 4 mins
        Difficulty.HARD -> 180_000L    // 3 mins
    }

    fun computeGlobal(
        playerPaths: Map<String, List<GeoPoint>>,
        allReferenceLines: List<TramLine>,
        visitedStations: Map<String, List<Station>>,
        durationMs: Long,
        difficulty: Difficulty
    ): ScoreBreakdown {
        var totalStationOrder = 0f
        var totalCompletion = 0f
        
        val count = allReferenceLines.size.toFloat()

        for (line in allReferenceLines) {
            val visited = visitedStations[line.id] ?: emptyList()

            totalStationOrder += computeStationOrderScore(visited, line.stations)
            totalCompletion += if (line.stations.isEmpty()) 1f 
                               else visited.size.toFloat() / line.stations.size
        }

        val avgStationOrder = totalStationOrder / count
        val avgCompletion = totalCompletion / count
        val speedBonus = computeSpeedBonus(durationMs, timeLimitMs(difficulty))

        // Weights: station order is now the primary factor
        val rawScore = when (difficulty) {
            Difficulty.EASY -> 
                avgStationOrder * 0.50f + avgCompletion * 0.40f + speedBonus * 0.10f
            Difficulty.MEDIUM ->
                avgStationOrder * 0.60f + avgCompletion * 0.30f + speedBonus * 0.10f
            Difficulty.HARD ->
                avgStationOrder * 0.60f + avgCompletion * 0.20f + speedBonus * 0.20f
        }

        val total = (rawScore * 1000f).roundToInt().coerceIn(0, 1000)
        return ScoreBreakdown(avgStationOrder, avgCompletion, speedBonus, total)
    }

    private fun computeStationOrderScore(visited: List<Station>, reference: List<Station>): Float {
        if (reference.isEmpty()) return 1f
        if (visited.isEmpty()) return 0f
        val refIds = reference.map { it.id }
        val visIds = visited.map { it.id }
        val lcs = longestCommonSubsequenceLength(refIds, visIds)
        return min(1f, lcs.toFloat() / refIds.size)
    }

    private fun longestCommonSubsequenceLength(a: List<String>, b: List<String>): Int {
        val dp = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in 1..a.size) for (j in 1..b.size) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1] + 1
            else max(dp[i - 1][j], dp[i][j - 1])
        }
        return dp[a.size][b.size]
    }

    private fun computeSpeedBonus(durationMs: Long, limitMs: Long): Float {
        if (durationMs >= limitMs) return 0f
        return (1f - durationMs.toFloat() / limitMs).coerceIn(0f, 1f)
    }
}
