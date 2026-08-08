package com.ascend.app.domain

/**
 * Skills deliberately do NOT add parallel systems — every one of them rewrites
 * a rule that already exists (penalty size, quest payout, streak survival,
 * gold rate). That's where depth comes from: a handful of mechanics that
 * interact, rather than a pile of features sitting side by side.
 */
enum class Skill(
    val displayName: String,
    val description: String,
    val cost: Int,
    val requiredHunterLevel: Int,
    val tier: Int,
) {
    // ---- Tier 1 ----------------------------------------------------------
    IRON_BODY(
        displayName = "Iron Body",
        description = "Your streak survives one missed day every 7 days.",
        cost = 1,
        requiredHunterLevel = 1,
        tier = 1,
    ),
    SCHOLARS_EYE(
        displayName = "Scholar's Eye",
        description = "+25% XP from completed focus sessions.",
        cost = 1,
        requiredHunterLevel = 1,
        tier = 1,
    ),
    COIN_PURSE(
        displayName = "Coin Purse",
        description = "+30% Gold from optional habits.",
        cost = 1,
        requiredHunterLevel = 3,
        tier = 1,
    ),

    // ---- Tier 2 ----------------------------------------------------------
    RULERS_AUTHORITY(
        displayName = "Ruler's Authority",
        description = "Penalty quests pay 2.5x instead of 1.5x.",
        cost = 2,
        requiredHunterLevel = 8,
        tier = 2,
    ),
    SECOND_WIND(
        displayName = "Second Wind",
        description = "The first missed non-negotiable each week costs no XP.",
        cost = 2,
        requiredHunterLevel = 8,
        tier = 2,
    ),
    MANA_CONVERSION(
        displayName = "Mana Conversion",
        description = "Trade Gold for XP from the System screen.",
        cost = 2,
        requiredHunterLevel = 12,
        tier = 2,
    ),

    // ---- Tier 3 ----------------------------------------------------------
    SHADOW_PRESERVATION(
        displayName = "Shadow Preservation",
        description = "Every Shadow you command grants double its bonus.",
        cost = 3,
        requiredHunterLevel = 18,
        tier = 3,
    ),
    DOMAIN_EXPANSION(
        displayName = "Domain Expansion",
        description = "Daily Quest rewards are doubled.",
        cost = 3,
        requiredHunterLevel = 18,
        tier = 3,
    ),
    MONARCHS_WILL(
        displayName = "Monarch's Will",
        description = "+10 percentage points to every stat effect.",
        cost = 3,
        requiredHunterLevel = 25,
        tier = 3,
    ),
    ;

    companion object {
        /** Skill points earned: one per 2 Hunter Levels, plus two per rank crossed. */
        fun pointsEarned(hunterLevel: Int, rank: Rank): Int =
            (hunterLevel / 2) + (rank.ordinal * 2)

        fun pointsSpent(unlocked: Set<Skill>): Int = unlocked.sumOf { it.cost }

        fun pointsAvailable(hunterLevel: Int, rank: Rank, unlocked: Set<Skill>): Int =
            pointsEarned(hunterLevel, rank) - pointsSpent(unlocked)

        /** A skill is buyable only if it's locked, level-gated open, and affordable. */
        fun canUnlock(skill: Skill, hunterLevel: Int, rank: Rank, unlocked: Set<Skill>): Boolean =
            skill !in unlocked &&
                hunterLevel >= skill.requiredHunterLevel &&
                pointsAvailable(hunterLevel, rank, unlocked) >= skill.cost

        val byTier: Map<Int, List<Skill>> get() = entries.groupBy { it.tier }
    }
}
