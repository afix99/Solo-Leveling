package com.ascend.app.domain

/** Gold rewards. Kept small relative to reward prices so buying something
 * takes real days of consistency rather than a single good afternoon. */
object Economy {
    const val NON_NEGOTIABLE_GOLD = 10
    const val OPTIONAL_GOLD = 5
    const val PENALTY_QUEST_GOLD = 15
    const val DAILY_QUEST_GOLD = 25
    const val FOCUS_SESSION_GOLD = 8

    /** Suggested costs shown when creating a reward, so the scale is legible. */
    val suggestedRewardCosts = listOf(50, 100, 250, 500, 1000)

    fun goldForCompletion(isNonNegotiable: Boolean, isPenaltyQuest: Boolean): Int = when {
        isPenaltyQuest -> PENALTY_QUEST_GOLD
        isNonNegotiable -> NON_NEGOTIABLE_GOLD
        else -> OPTIONAL_GOLD
    }
}
