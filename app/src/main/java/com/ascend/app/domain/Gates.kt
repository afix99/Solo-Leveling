package com.ascend.app.domain

import java.time.LocalDate

/**
 * Gates are the only place the app asks you to take a real risk, and they are
 * deliberately opt-in with a fixed stake.
 *
 * The research on gamification is blunt about this: loss-aversion and scarcity
 * ("black hat" motivation) drive compulsion rather than habit, and a system
 * that leans on them is building dependency. So nothing here is mandatory,
 * nothing expires on its own, ignoring Gates entirely costs you nothing, and
 * the stake can never exceed what a Gate pays back.
 *
 * What they *do* provide is self-selected difficulty — the Yerkes-Dodson point
 * that performance peaks at moderate pressure, made into something you choose
 * rather than something imposed.
 */
enum class GateRank(
    val displayName: String,
    val days: Int,
    val stake: Int,
    val rewardMultiplier: Double,
    val xpReward: Int,
    val requiredHunterLevel: Int,
) {
    E("E-Rank Gate", days = 3, stake = 50, rewardMultiplier = 2.0, xpReward = 60, requiredHunterLevel = 1),
    D("D-Rank Gate", days = 5, stake = 120, rewardMultiplier = 2.2, xpReward = 140, requiredHunterLevel = 5),
    C("C-Rank Gate", days = 7, stake = 250, rewardMultiplier = 2.5, xpReward = 300, requiredHunterLevel = 10),
    B("B-Rank Gate", days = 14, stake = 500, rewardMultiplier = 3.0, xpReward = 700, requiredHunterLevel = 20),
    A("A-Rank Gate", days = 21, stake = 900, rewardMultiplier = 3.5, xpReward = 1400, requiredHunterLevel = 35),
    S("S-Rank Gate", days = 30, stake = 1500, rewardMultiplier = 4.0, xpReward = 2500, requiredHunterLevel = 55),
    ;

    /** Gold returned on a clear, stake included. */
    val payout: Int get() = (stake * rewardMultiplier).toInt()

    companion object {
        fun availableAt(hunterLevel: Int): List<GateRank> =
            entries.filter { hunterLevel >= it.requiredHunterLevel }
    }
}

enum class GateStatus { ACTIVE, CLEARED, FAILED }

data class GateRun(
    val id: Long,
    val rank: GateRank,
    val startDate: LocalDate,
    val daysCleared: Int,
    val status: GateStatus,
) {
    val daysRemaining: Int get() = (rank.days - daysCleared).coerceAtLeast(0)
    val progressFraction: Float get() = daysCleared.toFloat() / rank.days.coerceAtLeast(1)
}

object Gates {
    /** You can only be inside one Gate at a time — keeps the choice sharp. */
    fun canEnter(activeRun: GateRun?, rank: GateRank, hunterLevel: Int, gold: Int): Boolean =
        activeRun == null && hunterLevel >= rank.requiredHunterLevel && gold >= rank.stake

    /**
     * Advances a run for one finished day. A perfect day banks progress; any
     * miss fails the run outright, which is what makes the stake mean anything.
     */
    fun advance(run: GateRun, dayWasPerfect: Boolean): GateRun = when {
        run.status != GateStatus.ACTIVE -> run
        !dayWasPerfect -> run.copy(status = GateStatus.FAILED)
        run.daysCleared + 1 >= run.rank.days ->
            run.copy(daysCleared = run.rank.days, status = GateStatus.CLEARED)
        else -> run.copy(daysCleared = run.daysCleared + 1)
    }

    /** Gold paid out on a clear; nothing on an active or failed run. */
    fun goldPayout(run: GateRun): Int =
        if (run.status == GateStatus.CLEARED) run.rank.payout else 0

    fun xpPayout(run: GateRun): Int =
        if (run.status == GateStatus.CLEARED) run.rank.xpReward else 0
}
