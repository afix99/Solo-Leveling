package com.ascend.app.domain

import kotlin.math.roundToInt

/**
 * Everything that modifies a payout, in one value.
 *
 * Before this existed, each effect function grew another parameter every time
 * a system was added (`classXpBonusPercent`, then shadows, then skills...).
 * Bundling them means the interactions live in one readable place and the
 * call sites stay short — which matters, because the interactions *are* the
 * design.
 */
data class HunterLoadout(
    /** Natural level per stat, derived from XP. */
    val statLevels: Map<Stat, Int> = Stat.entries.associateWith { 1 },
    /** Manually allocated stat points, on top of the natural level. */
    val allocatedPoints: Map<Stat, Int> = emptyMap(),
    val unlockedSkills: Set<Skill> = emptySet(),
    val shadows: List<Shadow> = emptyList(),
    val hunterClass: HunterClass = HunterClass.NONE,
) {
    /** Allocated points raise a stat exactly as if it had levelled naturally. */
    fun effectiveLevel(stat: Stat): Int =
        (statLevels[stat] ?: 1) + (allocatedPoints[stat] ?: 0)

    fun has(skill: Skill): Boolean = skill in unlockedSkills

    private val monarchBonus: Int get() = if (has(Skill.MONARCHS_WILL)) 10 else 0

    private fun shadowBonus(stat: Stat): Int =
        Shadows.bonusPercentFor(stat, shadows, has(Skill.SHADOW_PRESERVATION))

    // ---- Resolved effect percentages -------------------------------------

    fun xpBonusPercent(): Int =
        StatEffects.xpBonusPercent(effectiveLevel(Stat.INT)) +
            hunterClass.xpBonusPercent +
            shadowBonus(Stat.INT) +
            monarchBonus

    fun goldBonusPercent(isOptionalHabit: Boolean): Int =
        StatEffects.goldBonusPercent(effectiveLevel(Stat.STR)) +
            hunterClass.goldBonusPercent +
            shadowBonus(Stat.STR) +
            monarchBonus +
            if (isOptionalHabit && has(Skill.COIN_PURSE)) 30 else 0

    fun penaltyResistPercent(): Int =
        StatEffects.penaltyResistPercent(effectiveLevel(Stat.VIT)) +
            hunterClass.penaltyResistPercent +
            shadowBonus(Stat.VIT) +
            monarchBonus

    fun focusBonusPercent(): Int =
        StatEffects.focusBonusPercent(effectiveLevel(Stat.PER)) +
            hunterClass.focusBonusPercent +
            shadowBonus(Stat.PER) +
            monarchBonus +
            if (has(Skill.SCHOLARS_EYE)) 25 else 0

    fun streakBonusPercent(streakDays: Int): Int =
        StatEffects.streakBonusPercent(streakDays, effectiveLevel(Stat.AGI))

    /** Penalty quests normally pay 1.5x; Ruler's Authority raises it. */
    fun penaltyQuestMultiplier(): Double =
        if (has(Skill.RULERS_AUTHORITY)) 2.5 else Leveling.PENALTY_QUEST_MULTIPLIER

    fun dailyQuestMultiplier(): Int = if (has(Skill.DOMAIN_EXPANSION)) 2 else 1

    // ---- Final payouts ----------------------------------------------------

    private fun applyPercent(base: Int, percent: Int): Int =
        (base * (1 + percent / 100.0)).roundToInt().coerceAtLeast(1)

    fun finalXp(isNonNegotiable: Boolean, isPenaltyQuest: Boolean, streakDays: Int): Int {
        val base = if (isNonNegotiable) Leveling.NON_NEGOTIABLE_XP else Leveling.OPTIONAL_XP
        val withQuest =
            if (isPenaltyQuest) (base * penaltyQuestMultiplier()).roundToInt() else base
        return applyPercent(withQuest, xpBonusPercent() + streakBonusPercent(streakDays))
    }

    fun finalGold(isNonNegotiable: Boolean, isPenaltyQuest: Boolean): Int {
        val base = Economy.goldForCompletion(isNonNegotiable, isPenaltyQuest)
        return applyPercent(base, goldBonusPercent(isOptionalHabit = !isNonNegotiable))
    }

    /**
     * Penalty XP loss. Second Wind can waive it entirely for the first miss of
     * a week; otherwise resistance is clamped so a penalty is never fully
     * negated — accountability has to keep some teeth.
     */
    fun finalPenalty(isFirstMissThisWeek: Boolean): Int {
        if (has(Skill.SECOND_WIND) && isFirstMissThisWeek) return 0
        val resist = penaltyResistPercent().coerceIn(0, 90)
        return (Leveling.PENALTY_XP_LOSS * (1 - resist / 100.0)).roundToInt().coerceAtLeast(1)
    }

    fun finalFocusBonus(): Int =
        applyPercent(Leveling.FOCUS_SESSION_BONUS_XP, focusBonusPercent())

    /** Iron Body absorbs one miss per rolling week before a streak breaks. */
    fun streakSurvivesMiss(missesThisWeek: Int): Boolean =
        has(Skill.IRON_BODY) && missesThisWeek <= 1
}
