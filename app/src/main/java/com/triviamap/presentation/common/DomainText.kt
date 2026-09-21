package com.triviamap.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.triviamap.R
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.monetization.SupportTier
import com.triviamap.domain.progress.ShareText
import java.text.NumberFormat
import java.time.LocalDate

/** Display texts of domain concepts (badges, level titles, tips, difficulties), resolved from string resources. */

@StringRes
private fun badgeTitleRes(id: String): Int = when (id) {
    "first_run" -> R.string.badge_first_run
    "hub_expert" -> R.string.badge_hub_expert
    "night_rider" -> R.string.badge_night_rider
    "combo_10" -> R.string.badge_combo_10
    "terminus" -> R.string.badge_terminus
    "streak_3" -> R.string.badge_streak_3
    "streak_7" -> R.string.badge_streak_7
    "daily_player" -> R.string.badge_daily_player
    "regular" -> R.string.badge_regular
    else -> R.string.badge_line_master
}

@StringRes
private fun badgeDescriptionRes(id: String): Int = when (id) {
    "first_run" -> R.string.badge_first_run_d
    "hub_expert" -> R.string.badge_hub_expert_d
    "night_rider" -> R.string.badge_night_rider_d
    "combo_10" -> R.string.badge_combo_10_d
    "terminus" -> R.string.badge_terminus_d
    "streak_3" -> R.string.badge_streak_3_d
    "streak_7" -> R.string.badge_streak_7_d
    "daily_player" -> R.string.badge_daily_player_d
    "regular" -> R.string.badge_regular_d
    else -> R.string.badge_line_master_d
}

private val levelTitleRes = intArrayOf(
    R.string.level_0, R.string.level_1, R.string.level_2, R.string.level_3, R.string.level_4, R.string.level_5
)

@Composable fun badgeTitle(id: String) = stringResource(badgeTitleRes(id))
@Composable fun badgeDescription(id: String) = stringResource(badgeDescriptionRes(id))
@Composable fun levelTitle(index: Int) = stringResource(levelTitleRes[index.coerceIn(0, levelTitleRes.lastIndex)])

@Composable
fun tierTitle(tier: SupportTier) = stringResource(
    when (tier) {
        SupportTier.COFFEE -> R.string.tier_coffee
        SupportTier.TRAM_TICKET -> R.string.tier_tram_ticket
    }
)

@StringRes
fun difficultyLabelRes(d: Difficulty): Int = when (d) {
    Difficulty.EASY -> R.string.diff_easy
    Difficulty.MEDIUM -> R.string.diff_medium
    Difficulty.HARD -> R.string.diff_hard
}

@Composable fun difficultyLabel(d: Difficulty) = stringResource(difficultyLabelRes(d))

/** Localized Wordle-style recap (uses the current app locale). */
fun buildShareMessage(
    context: Context,
    isDaily: Boolean,
    difficultyName: String,
    epochDay: Long,
    level: Int,
    score: Int,
    maxCombo: Int,
    answerLog: String
): String {
    val header = if (isDaily) context.getString(R.string.share_daily, LocalDate.ofEpochDay(epochDay).toString())
    else context.getString(
        R.string.share_sprint,
        context.getString(difficultyLabelRes(Difficulty.valueOf(difficultyName))).lowercase()
    )
    val stats = context.getString(R.string.share_stats, level, NumberFormat.getIntegerInstance().format(score), maxCombo)
    return ShareText.build(header, stats, answerLog)
}
