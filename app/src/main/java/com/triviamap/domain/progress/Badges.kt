package com.triviamap.domain.progress

data class Badge(val id: String, val title: String, val description: String)

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
        Badge(FIRST_RUN, "First ride", "Finish your first run"),
        Badge(HUB_EXPERT, "Hub expert", "Solve 10 sorting challenges in one run"),
        Badge(NIGHT_RIDER, "Night rider", "Finish a run between 10 pm and 4 am"),
        Badge(COMBO_10, "On a roll", "Reach a x10 combo"),
        Badge(TERMINUS, "Terminus", "Reach level 21"),
        Badge(STREAK_3, "Three in a row", "Play 3 days in a row"),
        Badge(STREAK_7, "Weekly commuter", "Play 7 days in a row"),
        Badge(DAILY_PLAYER, "Daily rider", "Complete a daily challenge"),
        Badge(REGULAR, "Regular", "Place 200 stations"),
        Badge(LINE_MASTER, "Line master", "Master every station of one line")
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
