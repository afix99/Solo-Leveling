package com.ascend.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelingTest {

    @Test
    fun `level 1 at zero xp`() {
        assertEquals(1, Leveling.levelForXp(0))
    }

    @Test
    fun `xp floor and level are inverses`() {
        for (level in 1..30) {
            val floor = Leveling.xpFloorForLevel(level)
            assertEquals("level at its own floor should equal itself", level, Leveling.levelForXp(floor))
            if (level > 1) {
                assertEquals("one xp below floor should be previous level", level - 1, Leveling.levelForXp(floor - 1))
            }
        }
    }

    @Test
    fun `level rises with xp`() {
        var last = Leveling.levelForXp(0)
        for (xp in 0..5000 step 50) {
            val level = Leveling.levelForXp(xp)
            assertTrue(level >= last)
            last = level
        }
    }

    @Test
    fun `non-negotiable awards more xp than optional`() {
        assertTrue(
            Leveling.xpForCompletion(isNonNegotiable = true, isPenaltyQuest = false) >
                Leveling.xpForCompletion(isNonNegotiable = false, isPenaltyQuest = false),
        )
    }

    @Test
    fun `penalty quest awards bonus xp over normal completion`() {
        val normal = Leveling.xpForCompletion(isNonNegotiable = true, isPenaltyQuest = false)
        val penalty = Leveling.xpForCompletion(isNonNegotiable = true, isPenaltyQuest = true)
        assertTrue(penalty > normal)
    }

    @Test
    fun `penalty never drops xp below current level floor`() {
        // Sit exactly at a level floor; a penalty must not push below it.
        val level = 5
        val floor = Leveling.xpFloorForLevel(level)
        val result = Leveling.applyPenalty(floor)
        assertEquals(floor, result)
    }

    @Test
    fun `penalty subtracts flat amount when there is headroom`() {
        val floor = Leveling.xpFloorForLevel(5)
        val xp = floor + 100
        val result = Leveling.applyPenalty(xp)
        assertEquals(xp - Leveling.PENALTY_XP_LOSS, result)
    }

    @Test
    fun `hunter level lags behind a single maxed stat`() {
        val singleStatXp = Leveling.xpFloorForLevel(20)
        val hunterLevel = Leveling.hunterLevelForTotalXp(singleStatXp)
        assertTrue(
            "hunter level ($hunterLevel) should be lower than a lone stat's level (20) at equal xp",
            hunterLevel < 20,
        )
    }

    @Test
    fun `rank thresholds match spec`() {
        assertEquals(Rank.E, Rank.forHunterLevel(1))
        assertEquals(Rank.E, Rank.forHunterLevel(4))
        assertEquals(Rank.D, Rank.forHunterLevel(5))
        assertEquals(Rank.D, Rank.forHunterLevel(9))
        assertEquals(Rank.C, Rank.forHunterLevel(10))
        assertEquals(Rank.C, Rank.forHunterLevel(19))
        assertEquals(Rank.B, Rank.forHunterLevel(20))
        assertEquals(Rank.B, Rank.forHunterLevel(34))
        assertEquals(Rank.A, Rank.forHunterLevel(35))
        assertEquals(Rank.A, Rank.forHunterLevel(54))
        assertEquals(Rank.S, Rank.forHunterLevel(55))
        assertEquals(Rank.S, Rank.forHunterLevel(999))
    }

    @Test
    fun `streak resets to zero on a miss and increments on completion`() {
        assertEquals(0, Leveling.nextStreak(previousStreak = 12, completedToday = false))
        assertEquals(13, Leveling.nextStreak(previousStreak = 12, completedToday = true))
    }

    @Test
    fun `recalibration nudge triggers at three misses in a week`() {
        assertFalse(Leveling.shouldShowRecalibrationNudge(2))
        assertTrue(Leveling.shouldShowRecalibrationNudge(3))
        assertTrue(Leveling.shouldShowRecalibrationNudge(5))
    }

    @Test
    fun `raise-target suggestion needs three consecutive too-easy ratings`() {
        assertFalse(
            Leveling.shouldSuggestRaisingTarget(
                listOf(DifficultyRating.TOO_EASY, DifficultyRating.TOO_EASY),
            ),
        )
        assertFalse(
            Leveling.shouldSuggestRaisingTarget(
                listOf(DifficultyRating.TOO_EASY, DifficultyRating.JUST_RIGHT, DifficultyRating.TOO_EASY),
            ),
        )
        assertTrue(
            Leveling.shouldSuggestRaisingTarget(
                listOf(DifficultyRating.TOO_EASY, DifficultyRating.TOO_EASY, DifficultyRating.TOO_EASY),
            ),
        )
    }

    @Test
    fun `streak milestones are 7, 30, 100`() {
        assertTrue(Leveling.isStreakMilestone(7))
        assertTrue(Leveling.isStreakMilestone(30))
        assertTrue(Leveling.isStreakMilestone(100))
        assertFalse(Leveling.isStreakMilestone(8))
    }
}
