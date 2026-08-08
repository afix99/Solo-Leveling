package com.ascend.app.domain

/**
 * The choice layer. Levelling used to be entirely passive — numbers rose and
 * you watched. Now each Hunter Level hands you points you have to spend, and
 * since every stat does something different, spending them is a real
 * trade-off with no dominant answer.
 */
object StatAllocation {
    const val POINTS_PER_LEVEL = 3

    /** Respec costs gold, so rebuilding is possible but not free. */
    const val RESPEC_GOLD_COST = 250

    fun pointsEarned(hunterLevel: Int): Int =
        ((hunterLevel - 1) * POINTS_PER_LEVEL).coerceAtLeast(0)

    fun pointsSpent(allocated: Map<Stat, Int>): Int = allocated.values.sum()

    fun pointsAvailable(hunterLevel: Int, allocated: Map<Stat, Int>): Int =
        (pointsEarned(hunterLevel) - pointsSpent(allocated)).coerceAtLeast(0)

    fun canAllocate(hunterLevel: Int, allocated: Map<Stat, Int>): Boolean =
        pointsAvailable(hunterLevel, allocated) > 0

    /** Returns the new allocation map, or the original if no points are free. */
    fun allocate(stat: Stat, hunterLevel: Int, allocated: Map<Stat, Int>): Map<Stat, Int> {
        if (!canAllocate(hunterLevel, allocated)) return allocated
        return allocated + (stat to ((allocated[stat] ?: 0) + 1))
    }

    fun respec(): Map<Stat, Int> = emptyMap()
}
