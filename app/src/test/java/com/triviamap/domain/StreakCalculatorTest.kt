package com.triviamap.domain

import com.triviamap.domain.model.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {
    @Test fun firstGameStartsStreak() = assertEquals(1, StreakCalculator.next(-1, 0, 20_000))
    @Test fun sameDayKeepsStreak() = assertEquals(4, StreakCalculator.next(20_000, 4, 20_000))
    @Test fun nextDayIncrements() = assertEquals(5, StreakCalculator.next(20_000, 4, 20_001))
    @Test fun skippedDayResets() = assertEquals(1, StreakCalculator.next(20_000, 4, 20_002))
    @Test fun clockGoingBackwardsResets() = assertEquals(1, StreakCalculator.next(20_005, 4, 20_000))
}
