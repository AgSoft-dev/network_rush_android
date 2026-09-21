package com.triviamap.domain.progress

import java.time.LocalDate
import java.util.Locale

/** Wordle-style recap of a run, ready to paste anywhere. */
object ShareText {
    private const val MAX_MARKS = 30

    fun build(
        isDaily: Boolean,
        difficultyName: String,
        epochDay: Long,
        level: Int,
        score: Int,
        maxCombo: Int,
        answerLog: String
    ): String {
        val header = if (isDaily) "Next Stop Strasbourg · Daily ${LocalDate.ofEpochDay(epochDay)}"
        else "Next Stop Strasbourg · Sprint ${difficultyName.lowercase(Locale.ROOT)}"
        val marks = answerLog.take(MAX_MARKS).map {
            when (it) {
                'G' -> "🟩"
                'S' -> "⬜"
                else -> "🟥"
            }
        }.joinToString("") + if (answerLog.length > MAX_MARKS) "…" else ""
        return buildString {
            appendLine(header)
            appendLine("🚇 Level $level · ${"%,d".format(Locale.ROOT, score)} pts · x$maxCombo combo")
            if (marks.isNotEmpty()) append(marks)
        }.trimEnd()
    }
}
