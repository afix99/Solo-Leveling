package com.ascend.app.domain

/**
 * The Gold → XP trade unlocked by [Skill.MANA_CONVERSION].
 *
 * This is the only sink that competes with the Shop, and that is the point:
 * gold spent here buys progression, gold spent there buys a real-world reward,
 * and the choice between them is the decision the skill exists to create.
 *
 * The rate is deliberately poor. Converting is meant to be a way to put idle
 * gold to work, never a substitute for doing the habit — at [GOLD_PER_XP] gold
 * each, a day's earnings buy a few XP, against the ~20 XP a single
 * non-negotiable pays. The daily cap stops a large balance being dumped into
 * one stat to skip a level outright.
 */
object ManaConversion {

    /** Gold burned per point of XP produced. */
    const val GOLD_PER_XP = 20

    /** Most XP obtainable per day, across all stats combined. */
    const val DAILY_XP_CAP = 25

    /** Gold that would be spent converting at full daily allowance. */
    const val DAILY_GOLD_CAP = GOLD_PER_XP * DAILY_XP_CAP

    /**
     * XP that [goldSpent] produces, given how much has already been converted
     * today. Returns 0 when the skill would produce nothing, so callers can
     * treat 0 as "don't charge them".
     */
    fun xpFor(goldSpent: Int, xpAlreadyConvertedToday: Int): Int {
        if (goldSpent < GOLD_PER_XP) return 0
        val remaining = (DAILY_XP_CAP - xpAlreadyConvertedToday).coerceAtLeast(0)
        return (goldSpent / GOLD_PER_XP).coerceAtMost(remaining)
    }

    /**
     * Gold actually consumed to produce [xp]. Charging for the whole stake
     * would take gold for XP the cap refused to grant, so only the converted
     * portion is billed.
     */
    fun goldFor(xp: Int): Int = xp * GOLD_PER_XP

    /** XP still convertible today. */
    fun remainingToday(xpAlreadyConvertedToday: Int): Int =
        (DAILY_XP_CAP - xpAlreadyConvertedToday).coerceAtLeast(0)

    /**
     * The largest stake worth offering right now — capped by both the balance
     * and what the day has left, so the UI never proposes a trade that would
     * silently under-deliver.
     */
    fun maxSpendableNow(goldBalance: Int, xpAlreadyConvertedToday: Int): Int =
        minOf(goldBalance, goldFor(remainingToday(xpAlreadyConvertedToday)))
}
