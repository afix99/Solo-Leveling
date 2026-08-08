package com.ascend.app.domain

/**
 * A Shadow extracted from a habit you missed and then redeemed.
 *
 * Thematically this is the manhwa's signature mechanic — defeated enemies
 * become permanent soldiers. Mechanically it's the reason failing is worth
 * recovering from rather than hiding: a miss you redeem leaves you
 * permanently stronger than never having missed at all, which is a far
 * healthier framing than pure loss-aversion.
 */
data class Shadow(
    val id: Long,
    val name: String,
    val habitId: Long,
    val stat: Stat,
    val extractedAtEpochMillis: Long,
    /** Rises each time the same habit is redeemed again. */
    val rank: Int = 1,
)

object Shadows {
    /** Each shadow rank contributes this much bonus to its stat. */
    const val PERCENT_PER_RANK = 2

    /** Hard ceiling per stat, so shadow farming can't dominate the build. */
    const val MAX_PERCENT_PER_STAT = 30

    /**
     * Bonus percent this collection grants to [stat]. Doubled by
     * SHADOW_PRESERVATION, but the cap still applies afterwards — the skill
     * makes reaching the ceiling easier, it doesn't raise it.
     */
    fun bonusPercentFor(stat: Stat, shadows: List<Shadow>, preservation: Boolean): Int {
        val raw = shadows.filter { it.stat == stat }.sumOf { it.rank * PERCENT_PER_RANK }
        val scaled = if (preservation) raw * 2 else raw
        return scaled.coerceAtMost(MAX_PERCENT_PER_STAT)
    }

    /** Shadows are named after the habit they were extracted from. */
    fun nameFor(habitName: String): String = habitName.trim().ifBlank { "Unnamed" }

    /** Flavour label shown next to a shadow, escalating with rank. */
    fun rankTitle(rank: Int): String = when {
        rank >= 10 -> "Marshal"
        rank >= 6 -> "Commander"
        rank >= 3 -> "Knight"
        else -> "Soldier"
    }
}
