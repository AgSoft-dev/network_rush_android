package com.triviamap.domain.progress

/** Display texts live in string resources (see presentation/common/DomainText.kt). */
data class Badge(val id: String)

object Badges {
    const val FIRST_RUN = "first_run"
    const val HUB_EXPERT = "hub_expert"
    const val NIGHT_RIDER = "night_rider"
    const val COMBO_10 = "combo_10"
    const val TERMINUS = "terminus"
    const val STREAK_3 = "streak_3"
    const val STREAK_7 = "streak_7"
    const val DAILY_PLAYER = "daily_player"
    const val REGULAR = "regular"
    const val LINE_MASTER = "line_master"

    val all = listOf(
        Badge(FIRST_RUN),
        Badge(HUB_EXPERT),
        Badge(NIGHT_RIDER),
        Badge(COMBO_10),
        Badge(TERMINUS),
        Badge(STREAK_3),
        Badge(STREAK_7),
        Badge(DAILY_PLAYER),
        Badge(REGULAR),
        Badge(LINE_MASTER)
    )

    fun byId(id: String): Badge? = all.firstOrNull { it.id == id }

    data class Context(
        val classifySolved: Int,
        val maxCombo: Int,
        val level: Int,
        val streak: Int,
        val hourOfDay: Int,
        val isDaily: Boolean,
        val totalPlacements: Int,
        val hasMasteredLine: Boolean
    )

    /** Every badge whose condition holds now (the caller diffs with the ones already earned). */
    fun evaluate(c: Context): Set<String> = buildSet {
        add(FIRST_RUN)
        if (c.classifySolved >= 10) add(HUB_EXPERT)
        if (c.hourOfDay >= 22 || c.hourOfDay <= 4) add(NIGHT_RIDER)
        if (c.maxCombo >= 10) add(COMBO_10)
        if (c.level >= 21) add(TERMINUS)
        if (c.streak >= 3) add(STREAK_3)
        if (c.streak >= 7) add(STREAK_7)
        if (c.isDaily) add(DAILY_PLAYER)
        if (c.totalPlacements >= 200) add(REGULAR)
        if (c.hasMasteredLine) add(LINE_MASTER)
    }
}
