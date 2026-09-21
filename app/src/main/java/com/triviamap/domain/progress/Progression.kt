package com.triviamap.domain.progress

/** Player level derived from accumulated XP. */
data class PlayerLevel(
    val level: Int,
    val title: String,
    val xpIntoLevel: Int,
    val xpForNext: Int
) {
    val progress: Float get() = if (xpForNext == 0) 1f else xpIntoLevel.toFloat() / xpForNext
}

object Progression {
    private val TITLES = listOf(
        "Passenger", "Regular", "Commuter", "Conductor", "Line Chief", "Network Master"
    )

    /** XP needed to reach [level] (level 1 = 0, 2 = 100, 3 = 300, 4 = 600...). */
    fun xpToReach(level: Int): Int = 50 * level * (level - 1)

    fun levelFor(xp: Int): PlayerLevel {
        var level = 1
        while (xp >= xpToReach(level + 1)) level++
        val start = xpToReach(level)
        return PlayerLevel(
            level = level,
            title = TITLES[minOf((level - 1) / 3, TITLES.lastIndex)],
            xpIntoLevel = xp - start,
            xpForNext = xpToReach(level + 1) - start
        )
    }

    fun xpForCorrectAnswer(stage: Int, combo: Int): Int = 10 + 2 * stage + minOf(combo, 10)

    fun xpForRun(answersXp: Int, score: Int, isDaily: Boolean): Int =
        answersXp + score / 100 + if (isDaily) DAILY_BONUS_XP else 0

    const val DAILY_BONUS_XP = 50
}
