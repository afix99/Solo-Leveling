package com.ascend.app.domain

import kotlin.math.roundToInt

/**
 * Makes the 5 stats actually *do* something. In v1 they were decorative
 * counters — leveling Body changed nothing about how the app played, which
 * is the main reason progression felt hollow.
 *
 * Every effect is a percentage that scales with that stat's level and is
 * capped, so a single obsessively-farmed stat can't trivialise the game.
 */
object StatEffects {

    /** Percent bonus per level above 1, and the ceiling for each effect. */
    private const val PER_LEVEL = 3
    private const val CAP = 60

    private fun bonusPercent(level: Int): Int =
        ((level - 1) * PER_LEVEL).coerceIn(0, CAP)

    /** Mind (INT): every completion yields more XP. */
    fun xpBonusPercent(mindLevel: Int): Int = bonusPercent(mindLevel)

    /** Body (STR): every completion yields more Gold. */
    fun goldBonusPercent(bodyLevel: Int): Int = bonusPercent(bodyLevel)

    /** Health (VIT): missed non-negotiables cost less XP. */
    fun penaltyResistPercent(healthLevel: Int): Int = bonusPercent(healthLevel)

    /** Focus (PER): completed focus sessions pay a bigger bonus. */
    fun focusBonusPercent(focusLevel: Int): Int = bonusPercent(focusLevel)

    /**
     * Movement (AGI): raises the ceiling on the streak bonus, so consistency
     * pays off more the more you invest in it.
     */
    fun streakBonusCapPercent(movementLevel: Int): Int =
        (20 + (movementLevel - 1) * 5).coerceIn(20, 100)

    /** Streaks add +2% XP per consecutive day, up to the Movement-raised cap. */
    fun streakBonusPercent(streakDays: Int, movementLevel: Int): Int =
        (streakDays * 2).coerceIn(0, streakBonusCapPercent(movementLevel))

    private fun applyPercent(base: Int, percent: Int): Int =
        (base * (1 + percent / 100.0)).roundToInt()

    /**
     * Full XP calculation for a completion, folding in the Mind bonus, the
     * streak bonus, and any active class perk.
     */
    fun finalXp(
        baseXp: Int,
        mindLevel: Int,
        streakDays: Int,
        movementLevel: Int,
        classXpBonusPercent: Int = 0,
    ): Int {
        val total = xpBonusPercent(mindLevel) +
            streakBonusPercent(streakDays, movementLevel) +
            classXpBonusPercent
        return applyPercent(baseXp, total).coerceAtLeast(1)
    }

    fun finalGold(baseGold: Int, bodyLevel: Int, classGoldBonusPercent: Int = 0): Int =
        applyPercent(baseGold, goldBonusPercent(bodyLevel) + classGoldBonusPercent).coerceAtLeast(1)

    fun finalPenalty(basePenalty: Int, healthLevel: Int, classPenaltyResistPercent: Int = 0): Int {
        val resist = (penaltyResistPercent(healthLevel) + classPenaltyResistPercent).coerceIn(0, 90)
        return (basePenalty * (1 - resist / 100.0)).roundToInt().coerceAtLeast(1)
    }

    fun finalFocusBonus(baseBonus: Int, focusLevel: Int, classFocusBonusPercent: Int = 0): Int =
        applyPercent(baseBonus, focusBonusPercent(focusLevel) + classFocusBonusPercent).coerceAtLeast(1)

    /** One-line description of what a stat currently does, for the Stats screen. */
    fun describeEffect(stat: Stat, level: Int): String = when (stat) {
        Stat.INT -> "+${xpBonusPercent(level)}% XP from every habit"
        Stat.STR -> "+${goldBonusPercent(level)}% Gold from every habit"
        Stat.VIT -> "−${penaltyResistPercent(level)}% XP lost to penalties"
        Stat.PER -> "+${focusBonusPercent(level)}% bonus from focus sessions"
        Stat.AGI -> "Streak bonus caps at +${streakBonusCapPercent(level)}% XP"
    }
}
