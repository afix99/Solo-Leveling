package com.ascend.app.domain

import kotlin.math.floor
import kotlin.math.sqrt

/** Level + progress-within-level, ready for a progress bar. */
data class LevelProgress(val level: Int, val xpIntoLevel: Int, val xpSpanForLevel: Int) {
    val fraction: Float get() = if (xpSpanForLevel <= 0) 1f else xpIntoLevel.toFloat() / xpSpanForLevel
}

/**
 * Pure, Android-free leveling/XP/penalty math. See design spec §4 and §4.5.
 * Every constant here is deliberately simple — no lookup tables, no streak
 * multipliers in v1.
 */
object Leveling {
    const val NON_NEGOTIABLE_XP = 20
    const val OPTIONAL_XP = 10
    const val PENALTY_QUEST_MULTIPLIER = 1.5
    const val PENALTY_XP_LOSS = 15
    const val FOCUS_SESSION_BONUS_XP = 15

    /** Hunter Level is derived from the same curve as stat levels, but scaled
     * down first so it deliberately lags behind any single maxed-out stat —
     * ranking up rewards breadth, not one obsession. */
    private const val HUNTER_LEVEL_SCALE = 8

    /** Rolling-week miss count that triggers the Yerkes-Dodson recalibration nudge. */
    const val RECALIBRATION_MISS_THRESHOLD = 3

    /** XP awarded for completing a habit today. */
    fun xpForCompletion(isNonNegotiable: Boolean, isPenaltyQuest: Boolean): Int {
        val base = if (isNonNegotiable) NON_NEGOTIABLE_XP else OPTIONAL_XP
        return if (isPenaltyQuest) (base * PENALTY_QUEST_MULTIPLIER).toInt() else base
    }

    /** level = floor(sqrt(xp / 50)) + 1 — simple, monotonic, diminishing returns. */
    fun levelForXp(xp: Int): Int {
        if (xp <= 0) return 1
        return floor(sqrt(xp / 50.0)).toInt() + 1
    }

    /** Minimum XP required to *be at* [level] (inverse of [levelForXp]). */
    fun xpFloorForLevel(level: Int): Int {
        if (level <= 1) return 0
        val n = level - 1
        return n * n * 50
    }

    fun progressForXp(xp: Int): LevelProgress {
        val level = levelForXp(xp)
        val floor = xpFloorForLevel(level)
        val nextFloor = xpFloorForLevel(level + 1)
        val span = (nextFloor - floor).coerceAtLeast(1)
        return LevelProgress(level, (xp - floor).coerceIn(0, span), span)
    }

    fun hunterLevelForTotalXp(totalXp: Int): Int = levelForXp(totalXp / HUNTER_LEVEL_SCALE)

    fun rankForTotalXp(totalXp: Int): Rank = Rank.forHunterLevel(hunterLevelForTotalXp(totalXp))

    /** Progress-within-level for the Hunter ring, on the same scaled basis as [hunterLevelForTotalXp]. */
    fun hunterLevelProgress(totalXp: Int): LevelProgress = progressForXp(totalXp / HUNTER_LEVEL_SCALE)

    /**
     * Applies the flat XP penalty for a missed non-negotiable, floored at the
     * XP required for the stat's *current* level — a bad day can't erase a
     * level already earned.
     */
    fun applyPenalty(currentStatXp: Int): Int {
        val floor = xpFloorForLevel(levelForXp(currentStatXp))
        return (currentStatXp - PENALTY_XP_LOSS).coerceAtLeast(floor)
    }

    /** Next streak value given whether today was completed. */
    fun nextStreak(previousStreak: Int, completedToday: Boolean): Int =
        if (completedToday) previousStreak + 1 else 0

    /** Yerkes-Dodson recalibration nudge: too many misses in a rolling week. */
    fun shouldShowRecalibrationNudge(missesInLast7Days: Int): Boolean =
        missesInLast7Days >= RECALIBRATION_MISS_THRESHOLD

    /** Deliberate-practice target-bump suggestion: 3 "too easy" ratings in a row. */
    fun shouldSuggestRaisingTarget(lastThreeRatings: List<DifficultyRating>): Boolean =
        lastThreeRatings.size >= 3 && lastThreeRatings.takeLast(3).all { it == DifficultyRating.TOO_EASY }

    /** Streak milestones that generate an Evidence Log entry. */
    val streakMilestones = listOf(7, 30, 100)

    fun isStreakMilestone(streak: Int): Boolean = streak in streakMilestones
}
