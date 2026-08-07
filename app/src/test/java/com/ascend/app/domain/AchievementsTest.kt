package com.ascend.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {

    @Test
    fun `achievement ids are unique`() {
        val ids = Achievements.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `title ids referenced by achievements all exist`() {
        val titleIds = Achievements.titles.map { it.id }.toSet()
        Achievements.all.mapNotNull { it.titleId }.forEach { referenced ->
            assertTrue("unknown title id: $referenced", referenced in titleIds)
        }
    }

    @Test
    fun `nothing unlocks on a blank slate`() {
        val fresh = HunterStats()
        val unlocked = Achievements.newlyUnlocked(fresh, emptySet())
        assertTrue("a brand new hunter should have no achievements", unlocked.isEmpty())
    }

    @Test
    fun `first completion unlocks the first achievement`() {
        val stats = HunterStats(totalCompletions = 1)
        val unlocked = Achievements.newlyUnlocked(stats, emptySet())
        assertTrue(unlocked.any { it.id == "first_completion" })
    }

    @Test
    fun `already-unlocked achievements are not returned again`() {
        val stats = HunterStats(totalCompletions = 1)
        val unlocked = Achievements.newlyUnlocked(stats, setOf("first_completion"))
        assertFalse(unlocked.any { it.id == "first_completion" })
    }

    @Test
    fun `streak tiers unlock cumulatively`() {
        val stats = HunterStats(longestStreak = 30)
        val ids = Achievements.newlyUnlocked(stats, emptySet()).map { it.id }
        assertTrue(ids.contains("streak_7"))
        assertTrue(ids.contains("streak_30"))
        assertFalse(ids.contains("streak_100"))
    }

    @Test
    fun `rank achievements respect rank ordering`() {
        val ids = Achievements.newlyUnlocked(HunterStats(rank = Rank.A), emptySet()).map { it.id }
        assertTrue(ids.contains("rank_c"))
        assertTrue(ids.contains("rank_a"))
        assertFalse(ids.contains("rank_s"))
    }

    @Test
    fun `every achievement grants a positive gold reward`() {
        Achievements.all.forEach {
            assertTrue("${it.id} should reward gold", it.goldReward > 0)
        }
    }

    @Test
    fun `titleById resolves known titles and tolerates null`() {
        assertEquals("Unbroken", Achievements.titleById("unbroken")?.text)
        assertEquals(null, Achievements.titleById(null))
        assertEquals(null, Achievements.titleById("nope"))
    }
}
