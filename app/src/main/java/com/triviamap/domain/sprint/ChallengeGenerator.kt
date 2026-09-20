package com.triviamap.domain.sprint

import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine
import javax.inject.Inject
import kotlin.random.Random

/** Builds Sprint challenges for a given level. Deterministic for a seeded [Random]. */
class ChallengeGenerator @Inject constructor(private val random: Random) {

    fun generate(level: Int, lines: List<TramLine>): Challenge {
        require(lines.isNotEmpty()) { "No lines to generate a challenge from" }
        val stage = SprintRules.stage(level)
        var type = when (stage) {
            1, 2 -> ChallengeType.REORDER
            3 -> if (level % 4 == 0) ChallengeType.CLASSIFY else ChallengeType.REORDER
            4 -> if (level % 2 == 0) ChallengeType.CLASSIFY else ChallengeType.REORDER
            else -> when (random.nextInt(1, 11)) {
                9, 10 -> ChallengeType.SPEED_BURST
                in 5..8 -> ChallengeType.CLASSIFY
                else -> ChallengeType.REORDER
            }
        }
        if (type == ChallengeType.CLASSIFY && lines.size < 2) type = ChallengeType.REORDER
        if (type == ChallengeType.SPEED_BURST && lines.none { it.stations.size >= BURST_TILES }) type = ChallengeType.REORDER
        return when (type) {
            ChallengeType.REORDER -> reorder(stage, lines)
            ChallengeType.CLASSIFY -> classify(stage, lines)
            ChallengeType.SPEED_BURST -> speedBurst(lines)
        }
    }

    private fun reorder(stage: Int, lines: List<TramLine>): Challenge {
        val line = lines.random(random)
        val count = when (stage) {
            1 -> random.nextInt(3, 5)
            2 -> 5
            3 -> 6
            4 -> random.nextInt(6, 8)
            else -> random.nextInt(7, 9)
        }
        val isForward = stage == 1 || random.nextBoolean()
        // Stage 1: interchange hubs + terminus only (falls back to the full line if too few)
        val available = (if (stage == 1) {
            line.stations.filterIndexed { i, s -> s.lines.size > 1 || i == 0 || i == line.stations.lastIndex }
        } else line.stations).let { if (it.size >= 3) it else line.stations }

        val window = if (available.size <= count) available else {
            val start = random.nextInt(0, available.size - count + 1)
            available.subList(start, start + count)
        }
        val correct = if (isForward) window else window.reversed()
        return Challenge(ChallengeType.REORDER, line, null, correct, correct.shuffledNotSorted(), isForward)
    }

    private fun classify(stage: Int, lines: List<TramLine>): Challenge {
        val line1 = lines.random(random)
        val others = lines.filter { it.id != line1.id }
        val line2 = others.filter { o -> o.stations.any { s -> line1.stations.any { it.id == s.id } } }
            .randomOrNull(random) ?: others.random(random)

        val totalCount = if (stage <= 4) random.nextInt(5, 7) else random.nextInt(7, 9)
        val maxHubs = if (stage <= 4) 1 else 3

        val ids1 = line1.stations.mapTo(HashSet()) { it.id }
        val ids2 = line2.stations.mapTo(HashSet()) { it.id }
        val only1 = line1.stations.filter { it.id !in ids2 }.shuffled(random).take(totalCount / 2)
        val only2 = line2.stations.filter { it.id !in ids1 }.shuffled(random).take(totalCount / 2)
        val hubs = line1.stations.filter { it.id in ids2 }.shuffled(random).take(maxHubs)
        val picked = (only1 + only2 + hubs).distinctBy { it.id }.take(totalCount)

        val isForward = random.nextBoolean()
        // NOTE: ordering stations of two different lines this way is arbitrary (see TODO §4 CLASSIFY).
        val sorted = picked.sortedBy { s ->
            val i1 = line1.stations.indexOfFirst { it.id == s.id }.let { if (it == -1) Int.MAX_VALUE else it }
            val i2 = line2.stations.indexOfFirst { it.id == s.id }.let { if (it == -1) Int.MAX_VALUE else it }
            minOf(i1, i2)
        }
        val correct = if (isForward) sorted else sorted.reversed()

        val sides = correct.associate { s ->
            s.id to when {
                s.id in ids1 && s.id in ids2 -> Side.HUB
                s.id in ids1 -> Side.LINE_1
                else -> Side.LINE_2
            }
        }
        return Challenge(ChallengeType.CLASSIFY, line1, line2, correct, correct.shuffledNotSorted(), isForward, sides)
    }

    private fun speedBurst(lines: List<TramLine>): Challenge {
        val line = lines.filter { it.stations.size >= BURST_TILES }.random(random)
        val start = random.nextInt(0, line.stations.size - BURST_TILES + 1)
        val seq = line.stations.subList(start, start + BURST_TILES)
        return Challenge(ChallengeType.SPEED_BURST, line, null, seq, seq.shuffledNotSorted(), true)
    }

    /** Shuffles, retrying so the puzzle never starts already solved. */
    private fun List<Station>.shuffledNotSorted(): List<Station> {
        if (size < 2) return this
        repeat(10) {
            val s = shuffled(random)
            if (s != this) return s
        }
        return reversed()
    }

    private companion object {
        const val BURST_TILES = 3
    }
}
