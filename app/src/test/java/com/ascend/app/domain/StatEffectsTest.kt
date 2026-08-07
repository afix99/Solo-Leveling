package com.ascend.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatEffectsTest {

    @Test
    fun `level 1 stats give no bonus`() {
        assertEquals(0, StatEffects.xpBonusPercent(1))
        assertEquals(0, StatEffects.goldBonusPercent(1))
        assertEquals(0, StatEffects.penaltyResistPercent(1))
        assertEquals(0, StatEffects.focusBonusPercent(1))
    }

    @Test
    fun `bonuses rise with level and then cap`() {
        assertTrue(StatEffects.xpBonusPercent(5) > StatEffects.xpBonusPercent(2))
        // Far beyond the cap should still be clamped, not unbounded.
        assertEquals(StatEffects.xpBonusPercent(100), StatEffects.xpBonusPercent(1000))
    }

    @Test
    fun `xp bonus increases final xp`() {
        val base = 20
        val low = StatEffects.finalXp(base, mindLevel = 1, streakDays = 0, movementLevel = 1)
        val high = StatEffects.finalXp(base, mindLevel = 10, streakDays = 0, movementLevel = 1)
        assertEquals(base, low)
        assertTrue(high > low)
    }

    @Test
    fun `streak bonus increases xp and is capped by movement level`() {
        val noStreak = StatEffects.finalXp(20, mindLevel = 1, streakDays = 0, movementLevel = 1)
        val withStreak = StatEffects.finalXp(20, mindLevel = 1, streakDays = 5, movementLevel = 1)
        assertTrue(withStreak > noStreak)

        // A huge streak is limited by the Movement-derived cap.
        val cappedLowMovement = StatEffects.streakBonusPercent(500, movementLevel = 1)
        val cappedHighMovement = StatEffects.streakBonusPercent(500, movementLevel = 10)
        assertEquals(StatEffects.streakBonusCapPercent(1), cappedLowMovement)
        assertTrue(cappedHighMovement > cappedLowMovement)
    }

    @Test
    fun `class bonus stacks on top of stat bonus`() {
        val withoutClass = StatEffects.finalXp(20, mindLevel = 5, streakDays = 0, movementLevel = 1)
        val withClass = StatEffects.finalXp(
            20,
            mindLevel = 5,
            streakDays = 0,
            movementLevel = 1,
            classXpBonusPercent = HunterClass.SCHOLAR.xpBonusPercent,
        )
        assertTrue(withClass > withoutClass)
    }

    @Test
    fun `health reduces penalty but never to zero`() {
        val base = Leveling.PENALTY_XP_LOSS
        val noResist = StatEffects.finalPenalty(base, healthLevel = 1)
        val highResist = StatEffects.finalPenalty(base, healthLevel = 30)
        assertEquals(base, noResist)
        assertTrue(highResist < noResist)
        assertTrue("penalty must stay meaningful", highResist >= 1)
    }

    @Test
    fun `penalty resistance is clamped even with an extreme class bonus`() {
        val result = StatEffects.finalPenalty(
            basePenalty = 100,
            healthLevel = 100,
            classPenaltyResistPercent = 500,
        )
        assertTrue("resistance must never fully negate a penalty", result >= 1)
    }

    @Test
    fun `gold scales with body level`() {
        val low = StatEffects.finalGold(10, bodyLevel = 1)
        val high = StatEffects.finalGold(10, bodyLevel = 10)
        assertEquals(10, low)
        assertTrue(high > low)
    }

    @Test
    fun `every stat has a non-blank effect description`() {
        Stat.entries.forEach { stat ->
            listOf(1, 5, 20).forEach { level ->
                assertTrue(StatEffects.describeEffect(stat, level).isNotBlank())
            }
        }
    }

    @Test
    fun `xp never rounds down to zero`() {
        assertTrue(StatEffects.finalXp(1, mindLevel = 1, streakDays = 0, movementLevel = 1) >= 1)
        assertTrue(StatEffects.finalGold(1, bodyLevel = 1) >= 1)
    }
}
