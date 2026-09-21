package com.triviamap.domain.sprint

import kotlin.random.Random

/** Everyone gets the same challenges on a given day: challenge #k of day d always uses the same seed. */
object DailyChallenge {
    private const val SALT = 0x5EED_7A11L

    fun random(epochDay: Long, challengeIndex: Int): Random =
        Random((epochDay * 1_000_003L + challengeIndex) xor SALT)
}
