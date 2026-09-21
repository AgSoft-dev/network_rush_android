package com.triviamap.domain.sprint

import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine
import javax.inject.Inject
import kotlin.random.Random

/**
 * Builds Sprint challenges for a given level. Deterministic for a seeded [Random].
 *
 * @param weights station id to weight (missing = 1.0): heavier stations are drawn more often
 *   (spaced repetition of the stations the player misses).
 */
class ChallengeGenerator @Inject constructor(private val random: Random) {

    private val recent = ArrayDeque<String>()

    fun generate(
        level: Int,
        lines: List<TramLine>,
        difficulty: Difficulty = Difficulty.MEDIUM,
        weights: Map<String, Double> = emptyMap(),
        random: Random = this.random,
        avoidRepeats: Boolean = random === this.random,
        forceType: ChallengeType? = null
    ): Challenge {
        require(lines.isNotEmpty()) { "No lines to generate a challenge from" }
        val stage = SprintRules.stage(level)
        val roll = random.nextInt(100)
        var type = forceType ?: when (stage) {
            1 -> ChallengeType.REORDER
            2 -> if (roll < 10) ChallengeType.SPEED_BURST else ChallengeType.REORDER
            3 -> when {
                level % 4 == 0 -> ChallengeType.CLASSIFY
                roll < 15 -> ChallengeType.SPEED_BURST
                else -> ChallengeType.REORDER
            }
            4 -> when {
                level % 2 == 0 -> ChallengeType.CLASSIFY
                roll < 15 -> ChallengeType.SPEED_BURST
                else -> ChallengeType.REORDER
            }
            else -> when {
                roll < 20 -> ChallengeType.SPEED_BURST
                roll < 55 -> ChallengeType.CLASSIFY
                else -> ChallengeType.REORDER
            }
        }
        if (type == ChallengeType.CLASSIFY && lines.size < 2) type = ChallengeType.REORDER
        if (type == ChallengeType.SPEED_BURST && lines.none { it.stations.size >= BURST_TILES }) type = ChallengeType.REORDER

        // Avoid serving the same segment again right away (a few retries, then accept)
        var challenge = build(type, level, stage, lines, difficulty, weights, random)
        if (avoidRepeats) {
            var tries = 0
            while (key(challenge) in recent && tries++ < 6) {
                challenge = build(type, level, stage, lines, difficulty, weights, random)
            }
            recent.addLast(key(challenge))
            if (recent.size > RECENT_MEMORY) recent.removeFirst()
        }
        return challenge
    }

    private fun key(c: Challenge) = "${c.line.id}:${c.line2?.id}:${c.correctOrder.first().id}:${c.correctOrder.size}"

    private fun build(
        type: ChallengeType, level: Int, stage: Int, lines: List<TramLine>,
        difficulty: Difficulty, weights: Map<String, Double>, random: Random
    ) = when (type) {
        ChallengeType.REORDER -> reorder(level, stage, lines, difficulty, weights, random)
        ChallengeType.CLASSIFY -> classify(level, stage, lines, difficulty, weights, random)
        ChallengeType.SPEED_BURST -> speedBurst(lines, weights, random)
    }

    private fun reorder(
        level: Int, stage: Int, lines: List<TramLine>, difficulty: Difficulty,
        weights: Map<String, Double>, random: Random
    ): Challenge {
        val line = lines.random(random)
        val count = SprintRules.tileCount(level, random)
        val isForward = when (difficulty) {
            Difficulty.EASY -> stage <= 2 || random.nextBoolean()
            Difficulty.MEDIUM -> stage == 1 || random.nextBoolean()
            Difficulty.HARD -> random.nextBoolean()
        }
        val sparse = when (difficulty) {
            Difficulty.EASY -> false
            Difficulty.MEDIUM -> stage >= 3 && random.nextDouble() < 0.3
            Difficulty.HARD -> stage >= 2 && random.nextDouble() < 0.6
        }
        // Stage 1: interchange hubs + terminus only (falls back to the full line if too few)
        val available = (if (stage == 1) {
            line.stations.filterIndexed { i, s -> s.lines.size > 1 || i == 0 || i == line.stations.lastIndex }
        } else line.stations).let { if (it.size >= 3) it else line.stations }

        val chosen = when {
            available.size <= count -> available
            sparse -> weightedSample(available, count, weights, random).sortedBy { s -> line.stations.indexOfFirst { it.id == s.id } }
            else -> weightedWindow(available, count, weights, random)
        }
        val correct = if (isForward) chosen else chosen.reversed()
        return Challenge(ChallengeType.REORDER, line, null, correct, correct.shuffledNotSorted(random), isForward)
    }

    private fun classify(
        level: Int, stage: Int, lines: List<TramLine>, difficulty: Difficulty,
        weights: Map<String, Double>, random: Random
    ): Challenge {
        val line1 = lines.random(random)
        val others = lines.filter { it.id != line1.id }
        val line2 = others.filter { o -> o.stations.any { s -> line1.stations.any { it.id == s.id } } }
            .randomOrNull(random) ?: others.random(random)

        val totalCount = if (stage <= 4) random.nextInt(5, 7) else random.nextInt(7, 9)
        val maxHubs = (if (stage <= 4) 1 else 3) + (if (difficulty == Difficulty.HARD) 1 else 0)

        val ids1 = line1.stations.mapTo(HashSet()) { it.id }
        val ids2 = line2.stations.mapTo(HashSet()) { it.id }
        val only1 = weightedSample(line1.stations.filter { it.id !in ids2 }, totalCount / 2, weights, random)
        val only2 = weightedSample(line2.stations.filter { it.id !in ids1 }, totalCount / 2, weights, random)
        val hubs = weightedSample(line1.stations.filter { it.id in ids2 }, maxHubs, weights, random)
        val picked = (only1 + only2 + hubs).distinctBy { it.id }.take(totalCount)

        val isForward = random.nextBoolean()
        fun columnOf(s: Station) = when {
            s.id in ids1 && s.id in ids2 -> Side.HUB
            s.id in ids1 -> Side.LINE_1
            else -> Side.LINE_2
        }
        // Each column follows the order of its own line (hubs follow line 1)
        fun ordered(side: Int, ref: TramLine): List<Station> =
            picked.filter { columnOf(it) == side }
                .sortedBy { s -> ref.stations.indexOfFirst { it.id == s.id } }
                .let { if (isForward) it else it.reversed() }
        val left = ordered(Side.LINE_1, line1)
        val hub = ordered(Side.HUB, line1)
        val right = ordered(Side.LINE_2, line2)
        val correct = left + hub + right

        return Challenge(
            type = ChallengeType.CLASSIFY,
            line = line1,
            line2 = line2,
            correctOrder = correct,
            tiles = correct.shuffledNotSorted(random),
            isForward = isForward,
            correctSides = correct.associate { it.id to columnOf(it) },
            columnOrders = mapOf(Side.LINE_1 to left, Side.HUB to hub, Side.LINE_2 to right)
        )
    }

    private fun speedBurst(lines: List<TramLine>, weights: Map<String, Double>, random: Random): Challenge {
        val line = lines.filter { it.stations.size >= BURST_TILES }.random(random)
        val seq = weightedWindow(line.stations, BURST_TILES, weights, random)
        return Challenge(ChallengeType.SPEED_BURST, line, null, seq, seq.shuffledNotSorted(random), true)
    }

    /** A contiguous window of [count] stations, starts drawn proportionally to their total weight. */
    private fun weightedWindow(items: List<Station>, count: Int, weights: Map<String, Double>, random: Random): List<Station> {
        val starts = items.size - count + 1
        val w = DoubleArray(starts) { s -> (s until s + count).sumOf { weights[items[it].id] ?: 1.0 } }
        val start = pickIndex(w, random)
        return items.subList(start, start + count)
    }

    /** [k] distinct stations, drawn without replacement proportionally to their weight. */
    private fun weightedSample(items: List<Station>, k: Int, weights: Map<String, Double>, random: Random): List<Station> {
        val pool = items.toMutableList()
        val out = ArrayList<Station>()
        while (out.size < k && pool.isNotEmpty()) {
            val i = pickIndex(DoubleArray(pool.size) { weights[pool[it].id] ?: 1.0 }, random)
            out += pool.removeAt(i)
        }
        return out
    }

    private fun pickIndex(w: DoubleArray, random: Random): Int {
        val total = w.sum()
        if (total <= 0.0) return random.nextInt(w.size)
        var r = random.nextDouble() * total
        for (i in w.indices) { r -= w[i]; if (r < 0) return i }
        return w.lastIndex
    }

    /** Shuffles, retrying so the puzzle never starts already solved. */
    private fun List<Station>.shuffledNotSorted(random: Random): List<Station> {
        if (size < 2) return this
        repeat(10) {
            val s = shuffled(random)
            if (s != this) return s
        }
        return reversed()
    }

    private companion object {
        const val BURST_TILES = 3
        const val RECENT_MEMORY = 4
    }
}
