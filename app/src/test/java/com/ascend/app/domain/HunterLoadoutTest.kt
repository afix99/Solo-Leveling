package com.ascend.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HunterLoadoutTest {

    private val bare = HunterLoadout()

    @Test
    fun `bare loadout pays the base rates`() {
        assertEquals(
            Leveling.NON_NEGOTIABLE_XP,
            bare.finalXp(isNonNegotiable = true, isPenaltyQuest = false, streakDays = 0),
        )
        assertEquals(
            Economy.NON_NEGOTIABLE_GOLD,
            bare.finalGold(isNonNegotiable = true, isPenaltyQuest = false),
        )
    }

    @Test
    fun `allocated points raise a stat exactly like natural levels`() {
        val natural = HunterLoadout(statLevels = mapOf(Stat.INT to 6))
        val allocated = HunterLoadout(
            statLevels = mapOf(Stat.INT to 1),
            allocatedPoints = mapOf(Stat.INT to 5),
        )
        assertEquals(natural.effectiveLevel(Stat.INT), allocated.effectiveLevel(Stat.INT))
        assertEquals(natural.xpBonusPercent(), allocated.xpBonusPercent())
    }

    @Test
    fun `allocating into Mind increases xp payout`() {
        val invested = HunterLoadout(allocatedPoints = mapOf(Stat.INT to 10))
        assertTrue(
            invested.finalXp(true, false, 0) > bare.finalXp(true, false, 0),
        )
    }

    @Test
    fun `allocating into Body increases gold but not xp`() {
        val invested = HunterLoadout(allocatedPoints = mapOf(Stat.STR to 10))
        assertTrue(invested.finalGold(true, false) > bare.finalGold(true, false))
        assertEquals(bare.finalXp(true, false, 0), invested.finalXp(true, false, 0))
    }

    @Test
    fun `stat investments are genuinely different choices`() {
        // No stat should be a strictly dominant pick — each buys a different axis.
        val mind = HunterLoadout(allocatedPoints = mapOf(Stat.INT to 10))
        val body = HunterLoadout(allocatedPoints = mapOf(Stat.STR to 10))
        assertTrue(mind.finalXp(true, false, 0) > body.finalXp(true, false, 0))
        assertTrue(body.finalGold(true, false) > mind.finalGold(true, false))
    }

    @Test
    fun `coin purse only boosts optional habits`() {
        val purse = HunterLoadout(unlockedSkills = setOf(Skill.COIN_PURSE))
        assertTrue(purse.finalGold(false, false) > bare.finalGold(false, false))
        assertEquals(bare.finalGold(true, false), purse.finalGold(true, false))
    }

    @Test
    fun `rulers authority raises penalty quest payout`() {
        val authority = HunterLoadout(unlockedSkills = setOf(Skill.RULERS_AUTHORITY))
        assertTrue(
            authority.finalXp(true, isPenaltyQuest = true, streakDays = 0) >
                bare.finalXp(true, isPenaltyQuest = true, streakDays = 0),
        )
    }

    @Test
    fun `second wind waives only the first miss of the week`() {
        val wind = HunterLoadout(unlockedSkills = setOf(Skill.SECOND_WIND))
        assertEquals(0, wind.finalPenalty(isFirstMissThisWeek = true))
        assertTrue(wind.finalPenalty(isFirstMissThisWeek = false) > 0)
    }

    @Test
    fun `penalties are never fully negated without second wind`() {
        val stacked = HunterLoadout(
            statLevels = mapOf(Stat.VIT to 99),
            allocatedPoints = mapOf(Stat.VIT to 99),
            unlockedSkills = setOf(Skill.MONARCHS_WILL),
            shadows = List(30) { Shadow(it.toLong(), "s", 1L, Stat.VIT, 0L, rank = 5) },
            hunterClass = HunterClass.SENTINEL,
        )
        assertTrue(
            "a maxed defensive build must still take some penalty",
            stacked.finalPenalty(isFirstMissThisWeek = false) >= 1,
        )
    }

    @Test
    fun `shadows raise the stat they were extracted for`() {
        val withShadows = HunterLoadout(
            shadows = listOf(
                Shadow(1, "Cold shower", 1L, Stat.STR, 0L, rank = 3),
                Shadow(2, "Read", 2L, Stat.INT, 0L, rank = 2),
            ),
        )
        assertTrue(withShadows.goldBonusPercent(false) > bare.goldBonusPercent(false))
        assertTrue(withShadows.xpBonusPercent() > bare.xpBonusPercent())
    }

    @Test
    fun `shadow bonus is capped even with preservation`() {
        val many = List(50) { Shadow(it.toLong(), "s", 1L, Stat.INT, 0L, rank = 9) }
        val capped = Shadows.bonusPercentFor(Stat.INT, many, preservation = true)
        assertEquals(Shadows.MAX_PERCENT_PER_STAT, capped)
    }

    @Test
    fun `preservation reaches the cap faster but does not raise it`() {
        val few = listOf(Shadow(1, "s", 1L, Stat.INT, 0L, rank = 3))
        val plain = Shadows.bonusPercentFor(Stat.INT, few, preservation = false)
        val doubled = Shadows.bonusPercentFor(Stat.INT, few, preservation = true)
        assertEquals(plain * 2, doubled)
        assertTrue(doubled <= Shadows.MAX_PERCENT_PER_STAT)
    }

    @Test
    fun `iron body absorbs one miss per week and no more`() {
        val iron = HunterLoadout(unlockedSkills = setOf(Skill.IRON_BODY))
        assertTrue(iron.streakSurvivesMiss(missesThisWeek = 1))
        assertFalse(iron.streakSurvivesMiss(missesThisWeek = 2))
        assertFalse("without the skill nothing is absorbed", bare.streakSurvivesMiss(1))
    }

    @Test
    fun `domain expansion doubles daily quest rewards`() {
        assertEquals(1, bare.dailyQuestMultiplier())
        assertEquals(2, HunterLoadout(unlockedSkills = setOf(Skill.DOMAIN_EXPANSION)).dailyQuestMultiplier())
    }

    @Test
    fun `class and stat bonuses stack rather than replace`() {
        val statOnly = HunterLoadout(allocatedPoints = mapOf(Stat.INT to 5))
        val both = statOnly.copy(hunterClass = HunterClass.SCHOLAR)
        assertTrue(both.xpBonusPercent() > statOnly.xpBonusPercent())
    }

    @Test
    fun `payouts never round down to zero`() {
        Stat.entries.forEach { _ ->
            assertTrue(bare.finalXp(false, false, 0) >= 1)
            assertTrue(bare.finalGold(false, false) >= 1)
            assertTrue(bare.finalFocusBonus() >= 1)
        }
    }
}
