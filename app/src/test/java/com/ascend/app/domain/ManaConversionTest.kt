package com.ascend.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ManaConversionTest {

    @Test
    fun `gold buys xp at the published rate`() {
        assertEquals(1, ManaConversion.xpFor(ManaConversion.GOLD_PER_XP, 0))
        assertEquals(5, ManaConversion.xpFor(ManaConversion.GOLD_PER_XP * 5, 0))
    }

    @Test
    fun `gold below the rate buys nothing`() {
        assertEquals(0, ManaConversion.xpFor(ManaConversion.GOLD_PER_XP - 1, 0))
        assertEquals(0, ManaConversion.xpFor(0, 0))
    }

    @Test
    fun `remainder gold is not converted so it is never billed`() {
        // 2.5x the rate must buy 2 XP, not round up to 3.
        val gold = (ManaConversion.GOLD_PER_XP * 2.5).toInt()
        assertEquals(2, ManaConversion.xpFor(gold, 0))
    }

    @Test
    fun `the daily cap limits a large stake instead of rejecting it`() {
        val hugeStake = ManaConversion.GOLD_PER_XP * 1000
        assertEquals(ManaConversion.DAILY_XP_CAP, ManaConversion.xpFor(hugeStake, 0))
    }

    @Test
    fun `xp already converted today counts against the cap`() {
        val alreadyDone = ManaConversion.DAILY_XP_CAP - 3
        val hugeStake = ManaConversion.GOLD_PER_XP * 1000
        assertEquals(3, ManaConversion.xpFor(hugeStake, alreadyDone))
    }

    @Test
    fun `a spent allowance converts nothing`() {
        val hugeStake = ManaConversion.GOLD_PER_XP * 1000
        assertEquals(0, ManaConversion.xpFor(hugeStake, ManaConversion.DAILY_XP_CAP))
        assertEquals(0, ManaConversion.remainingToday(ManaConversion.DAILY_XP_CAP))
    }

    @Test
    fun `an over-spent allowance never reports negative headroom`() {
        // Defensive: a cap lowered in a future version would leave existing
        // users above it, and that must not produce a negative offer.
        assertEquals(0, ManaConversion.remainingToday(ManaConversion.DAILY_XP_CAP + 50))
        assertEquals(0, ManaConversion.maxSpendableNow(10_000, ManaConversion.DAILY_XP_CAP + 50))
    }

    @Test
    fun `gold charged always matches the xp granted`() {
        // The invariant that stops the user being billed for capped-away XP.
        for (already in 0..ManaConversion.DAILY_XP_CAP) {
            for (offered in listOf(0, 19, 20, 137, 100_000)) {
                val xp = ManaConversion.xpFor(offered, already)
                val charged = ManaConversion.goldFor(xp)
                assert(charged <= offered) {
                    "charged $charged for $xp XP on an offer of $offered"
                }
            }
        }
    }

    @Test
    fun `the offered stake never exceeds the balance or the day's headroom`() {
        assertEquals(60, ManaConversion.maxSpendableNow(goldBalance = 60, xpAlreadyConvertedToday = 0))

        val cappedByDay = ManaConversion.maxSpendableNow(
            goldBalance = 1_000_000,
            xpAlreadyConvertedToday = ManaConversion.DAILY_XP_CAP - 2,
        )
        assertEquals(ManaConversion.GOLD_PER_XP * 2, cappedByDay)
    }

    @Test
    fun `converting is worse than doing the habit`() {
        // The rate exists to be a poor deal. If a day's full allowance ever
        // beats a single non-negotiable, the skill has become a shortcut.
        val fullDayOfConversion = ManaConversion.DAILY_XP_CAP
        val goldForOneDay = ManaConversion.goldFor(fullDayOfConversion)
        val daysOfGoldNeeded = goldForOneDay.toDouble() / Economy.NON_NEGOTIABLE_GOLD
        assert(daysOfGoldNeeded > 1.0) {
            "a day's conversion allowance costs less than a day of gold income"
        }
    }
}
