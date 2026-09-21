package com.triviamap.domain.progress

/** Wordle-style recap of a run, ready to paste anywhere. Texts are localized by the caller. */
object ShareText {
    private const val MAX_MARKS = 30

    fun build(header: String, statsLine: String, answerLog: String): String {
        val marks = answerLog.take(MAX_MARKS).map {
            when (it) {
                'G' -> "🟩"
                'S' -> "⬜"
                else -> "🟥"
            }
        }.joinToString("") + if (answerLog.length > MAX_MARKS) "…" else ""
        return buildString {
            appendLine(header)
            appendLine(statsLine)
            if (marks.isNotEmpty()) append(marks)
        }.trimEnd()
    }
}
