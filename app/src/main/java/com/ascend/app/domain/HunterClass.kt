package com.ascend.app.domain

/**
 * The Job Change payoff. Locked until [UNLOCK_RANK]; choosing one is
 * permanent-feeling but changeable from the profile, and each grants a
 * passive that stacks on top of [StatEffects].
 */
enum class HunterClass(
    val displayName: String,
    val tagline: String,
    val xpBonusPercent: Int = 0,
    val goldBonusPercent: Int = 0,
    val penaltyResistPercent: Int = 0,
    val focusBonusPercent: Int = 0,
) {
    NONE("Unassigned", "No class chosen yet."),

    SCHOLAR(
        displayName = "Scholar",
        tagline = "+20% XP from everything. Levels fastest.",
        xpBonusPercent = 20,
    ),
    MERCHANT(
        displayName = "Merchant",
        tagline = "+35% Gold. Buys rewards twice as often.",
        goldBonusPercent = 35,
    ),
    SENTINEL(
        displayName = "Sentinel",
        tagline = "−40% penalty damage. Survives bad weeks.",
        penaltyResistPercent = 40,
    ),
    ASSASSIN(
        displayName = "Assassin",
        tagline = "+50% focus session bonus. Rewards deep work.",
        focusBonusPercent = 50,
    ),
    SHADOW_MONARCH(
        displayName = "Shadow Monarch",
        tagline = "+10% to everything. Balanced, unlocked at S-Rank.",
        xpBonusPercent = 10,
        goldBonusPercent = 10,
        penaltyResistPercent = 10,
        focusBonusPercent = 10,
    ),
    ;

    companion object {
        /** Job Change becomes available here — a genuine mid-game milestone. */
        val UNLOCK_RANK = Rank.C

        /** Shadow Monarch is deliberately gated behind the endgame rank. */
        fun selectableAt(rank: Rank): List<HunterClass> {
            if (rank.ordinal < UNLOCK_RANK.ordinal) return emptyList()
            val base = listOf(SCHOLAR, MERCHANT, SENTINEL, ASSASSIN)
            return if (rank == Rank.S) base + SHADOW_MONARCH else base
        }
    }
}
