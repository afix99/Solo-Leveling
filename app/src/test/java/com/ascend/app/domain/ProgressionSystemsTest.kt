package com.ascend.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatAllocationTest {

    @Test
    fun `level 1 grants no points`() {
        assertEquals(0, StatAllocation.pointsEarned(1))
    }

    @Test
    fun `each level grants a fixed number of points`() {
        assertEquals(StatAllocation.POINTS_PER_LEVEL, StatAllocation.pointsEarned(2))
        assertEquals(StatAllocation.POINTS_PER_LEVEL * 9, StatAllocation.pointsEarned(10))
    }

    @Test
    fun `spending reduces the pool`() {
        val allocated = mapOf(Stat.INT to 2)
        assertEquals(
            StatAllocation.pointsEarned(5) - 2,
            StatAllocation.pointsAvailable(5, allocated),
        )
    }

    @Test
    fun `cannot allocate beyond earned points`() {
        val hunterLevel = 2 // 3 points
        var allocated = emptyMap<Stat, Int>()
        repeat(3) { allocated = StatAllocation.allocate(Stat.INT, hunterLevel, allocated) }
        assertEquals(3, StatAllocation.pointsSpent(allocated))

        val overspent = StatAllocation.allocate(Stat.INT, hunterLevel, allocated)
        assertEquals("a fourth point must be refused", allocated, overspent)
        assertFalse(StatAllocation.canAllocate(hunterLevel, allocated))
    }

    @Test
    fun `available points never go negative`() {
        val allocated = mapOf(Stat.INT to 99)
        assertTrue(StatAllocation.pointsAvailable(2, allocated) >= 0)
    }

    @Test
    fun `respec clears every allocation`() {
        assertTrue(StatAllocation.respec().isEmpty())
    }
}

class SkillsTest {

    @Test
    fun `skill points accumulate with level and rank`() {
        val low = Skill.pointsEarned(hunterLevel = 2, rank = Rank.E)
        val high = Skill.pointsEarned(hunterLevel = 20, rank = Rank.B)
        assertTrue(high > low)
    }

    @Test
    fun `unlocking spends points`() {
        val unlocked = setOf(Skill.IRON_BODY, Skill.SCHOLARS_EYE)
        assertEquals(
            Skill.IRON_BODY.cost + Skill.SCHOLARS_EYE.cost,
            Skill.pointsSpent(unlocked),
        )
    }

    @Test
    fun `cannot unlock a skill twice`() {
        val unlocked = setOf(Skill.IRON_BODY)
        assertFalse(Skill.canUnlock(Skill.IRON_BODY, 30, Rank.S, unlocked))
    }

    @Test
    fun `cannot unlock below the required level`() {
        assertFalse(
            Skill.canUnlock(Skill.MONARCHS_WILL, hunterLevel = 5, rank = Rank.S, unlocked = emptySet()),
        )
    }

    @Test
    fun `cannot unlock without enough points`() {
        // Level 2 / E-rank yields 1 point; a tier-3 skill costs 3.
        assertFalse(
            Skill.canUnlock(Skill.SHADOW_PRESERVATION, hunterLevel = 2, rank = Rank.E, unlocked = emptySet()),
        )
    }

    @Test
    fun `a reachable skill can be unlocked`() {
        assertTrue(
            Skill.canUnlock(Skill.IRON_BODY, hunterLevel = 4, rank = Rank.E, unlocked = emptySet()),
        )
    }

    @Test
    fun `higher tiers cost more and gate later`() {
        val t1 = Skill.entries.filter { it.tier == 1 }
        val t3 = Skill.entries.filter { it.tier == 3 }
        assertTrue(t3.minOf { it.cost } > t1.maxOf { it.cost })
        assertTrue(t3.minOf { it.requiredHunterLevel } > t1.maxOf { it.requiredHunterLevel })
    }

    @Test
    fun `every skill has a distinct non-blank description`() {
        val descriptions = Skill.entries.map { it.description }
        assertEquals(descriptions.size, descriptions.toSet().size)
        assertTrue(descriptions.all { it.isNotBlank() })
    }
}

class GatesTest {

    private fun run(rank: GateRank, days: Int = 0, status: GateStatus = GateStatus.ACTIVE) =
        GateRun(1L, rank, LocalDate.of(2026, 1, 1), days, status)

    @Test
    fun `a perfect day advances progress`() {
        val advanced = Gates.advance(run(GateRank.C), dayWasPerfect = true)
        assertEquals(1, advanced.daysCleared)
        assertEquals(GateStatus.ACTIVE, advanced.status)
    }

    @Test
    fun `a missed day fails the run`() {
        val failed = Gates.advance(run(GateRank.C, days = 5), dayWasPerfect = false)
        assertEquals(GateStatus.FAILED, failed.status)
    }

    @Test
    fun `finishing the last day clears the gate`() {
        val nearlyDone = run(GateRank.E, days = GateRank.E.days - 1)
        val cleared = Gates.advance(nearlyDone, dayWasPerfect = true)
        assertEquals(GateStatus.CLEARED, cleared.status)
        assertEquals(GateRank.E.days, cleared.daysCleared)
    }

    @Test
    fun `a settled run is never advanced again`() {
        val cleared = run(GateRank.E, days = GateRank.E.days, status = GateStatus.CLEARED)
        assertEquals(cleared, Gates.advance(cleared, dayWasPerfect = true))
        assertEquals(cleared, Gates.advance(cleared, dayWasPerfect = false))

        val failed = run(GateRank.E, days = 1, status = GateStatus.FAILED)
        assertEquals(failed, Gates.advance(failed, dayWasPerfect = true))
    }

    @Test
    fun `only cleared runs pay out`() {
        assertEquals(0, Gates.goldPayout(run(GateRank.C, days = 3)))
        assertEquals(0, Gates.goldPayout(run(GateRank.C, days = 3, status = GateStatus.FAILED)))
        val cleared = run(GateRank.C, days = GateRank.C.days, status = GateStatus.CLEARED)
        assertEquals(GateRank.C.payout, Gates.goldPayout(cleared))
        assertEquals(GateRank.C.xpReward, Gates.xpPayout(cleared))
    }

    @Test
    fun `every gate pays back more than it stakes`() {
        GateRank.entries.forEach {
            assertTrue("${it.name} must be worth entering", it.payout > it.stake)
        }
    }

    @Test
    fun `harder gates demand more and pay more`() {
        val ordered = GateRank.entries
        ordered.zipWithNext().forEach { (a, b) ->
            assertTrue("${b.name} should run longer than ${a.name}", b.days > a.days)
            assertTrue("${b.name} should stake more than ${a.name}", b.stake > a.stake)
            assertTrue("${b.name} should pay more than ${a.name}", b.payout > a.payout)
        }
    }

    @Test
    fun `cannot enter while a run is active`() {
        val active = run(GateRank.E)
        assertFalse(Gates.canEnter(active, GateRank.E, hunterLevel = 99, gold = 99_999))
    }

    @Test
    fun `cannot enter without the level or the gold`() {
        assertFalse(Gates.canEnter(null, GateRank.S, hunterLevel = 1, gold = 99_999))
        assertFalse(Gates.canEnter(null, GateRank.E, hunterLevel = 99, gold = 0))
        assertTrue(Gates.canEnter(null, GateRank.E, hunterLevel = 99, gold = GateRank.E.stake))
    }

    @Test
    fun `gates unlock progressively`() {
        assertEquals(listOf(GateRank.E), GateRank.availableAt(1))
        assertTrue(GateRank.availableAt(99).containsAll(GateRank.entries))
    }

    @Test
    fun `progress fraction tracks days cleared`() {
        assertEquals(0f, run(GateRank.C).progressFraction, 0.001f)
        assertEquals(1f, run(GateRank.C, days = GateRank.C.days).progressFraction, 0.001f)
    }
}

class ShadowsTest {

    @Test
    fun `no shadows means no bonus`() {
        assertEquals(0, Shadows.bonusPercentFor(Stat.INT, emptyList(), preservation = false))
    }

    @Test
    fun `shadows only buff their own stat`() {
        val shadows = listOf(Shadow(1, "s", 1L, Stat.STR, 0L, rank = 4))
        assertTrue(Shadows.bonusPercentFor(Stat.STR, shadows, false) > 0)
        assertEquals(0, Shadows.bonusPercentFor(Stat.INT, shadows, false))
    }

    @Test
    fun `higher ranked shadows contribute more`() {
        val weak = listOf(Shadow(1, "s", 1L, Stat.INT, 0L, rank = 1))
        val strong = listOf(Shadow(1, "s", 1L, Stat.INT, 0L, rank = 5))
        assertTrue(
            Shadows.bonusPercentFor(Stat.INT, strong, false) >
                Shadows.bonusPercentFor(Stat.INT, weak, false),
        )
    }

    @Test
    fun `rank titles escalate`() {
        assertEquals("Soldier", Shadows.rankTitle(1))
        assertEquals("Knight", Shadows.rankTitle(3))
        assertEquals("Commander", Shadows.rankTitle(6))
        assertEquals("Marshal", Shadows.rankTitle(10))
    }

    @Test
    fun `shadow names fall back when the habit name is blank`() {
        assertEquals("Unnamed", Shadows.nameFor("   "))
        assertNotEquals("Unnamed", Shadows.nameFor("Cold shower"))
    }
}
