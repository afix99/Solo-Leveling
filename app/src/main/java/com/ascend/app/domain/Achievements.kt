package com.ascend.app.domain

/** A snapshot of everything an achievement condition can look at. */
data class HunterStats(
    val totalCompletions: Int = 0,
    val longestStreak: Int = 0,
    val penaltyQuestsRedeemed: Int = 0,
    val focusSessionsCompleted: Int = 0,
    val focusMinutesTotal: Int = 0,
    val hunterLevel: Int = 1,
    val rank: Rank = Rank.E,
    val perfectDays: Int = 0,
    val perfectWeeks: Int = 0,
    val goldEarnedTotal: Int = 0,
    val maxStatLevel: Int = 1,
)

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val goldReward: Int,
    /** Title unlocked alongside it, if any. */
    val titleId: String? = null,
    val condition: (HunterStats) -> Boolean,
)

/** An equippable title, shown next to your name on Today. */
data class Title(val id: String, val text: String, val description: String)

object Achievements {

    val titles = listOf(
        Title("first_steps", "The Awakened", "Completed your first habit."),
        Title("unbroken", "Unbroken", "Held a 30-day streak."),
        Title("adversity", "The One Who Overcame Adversity", "Redeemed 10 penalty quests."),
        Title("deep_diver", "Deep Diver", "Completed 50 focus sessions."),
        Title("relentless", "Relentless", "100 perfect days."),
        Title("monarch", "Shadow Monarch", "Reached S-Rank."),
        Title("architect", "The Architect", "Took a stat to level 20."),
        Title("iron_will", "Iron Will", "Held a 100-day streak."),
    )

    fun titleById(id: String?): Title? = titles.find { it.id == id }

    val all: List<Achievement> = listOf(
        Achievement(
            id = "first_completion",
            name = "First Step",
            description = "Complete your first habit.",
            goldReward = 25,
            titleId = "first_steps",
            condition = { it.totalCompletions >= 1 },
        ),
        Achievement(
            id = "completions_50",
            name = "Getting Somewhere",
            description = "Complete 50 habits.",
            goldReward = 75,
            condition = { it.totalCompletions >= 50 },
        ),
        Achievement(
            id = "completions_250",
            name = "Habitual",
            description = "Complete 250 habits.",
            goldReward = 200,
            condition = { it.totalCompletions >= 250 },
        ),
        Achievement(
            id = "completions_1000",
            name = "Unrecognizable",
            description = "Complete 1000 habits.",
            goldReward = 750,
            condition = { it.totalCompletions >= 1000 },
        ),
        Achievement(
            id = "streak_7",
            name = "One Week",
            description = "Hold a 7-day streak on any habit.",
            goldReward = 50,
            condition = { it.longestStreak >= 7 },
        ),
        Achievement(
            id = "streak_30",
            name = "Unbroken",
            description = "Hold a 30-day streak on any habit.",
            goldReward = 200,
            titleId = "unbroken",
            condition = { it.longestStreak >= 30 },
        ),
        Achievement(
            id = "streak_100",
            name = "Iron Will",
            description = "Hold a 100-day streak on any habit.",
            goldReward = 1000,
            titleId = "iron_will",
            condition = { it.longestStreak >= 100 },
        ),
        Achievement(
            id = "redeem_1",
            name = "Back On It",
            description = "Redeem your first penalty quest.",
            goldReward = 40,
            condition = { it.penaltyQuestsRedeemed >= 1 },
        ),
        Achievement(
            id = "redeem_10",
            name = "The One Who Overcame Adversity",
            description = "Redeem 10 penalty quests.",
            goldReward = 250,
            titleId = "adversity",
            condition = { it.penaltyQuestsRedeemed >= 10 },
        ),
        Achievement(
            id = "focus_10",
            name = "In The Zone",
            description = "Complete 10 focus sessions.",
            goldReward = 75,
            condition = { it.focusSessionsCompleted >= 10 },
        ),
        Achievement(
            id = "focus_50",
            name = "Deep Diver",
            description = "Complete 50 focus sessions.",
            goldReward = 300,
            titleId = "deep_diver",
            condition = { it.focusSessionsCompleted >= 50 },
        ),
        Achievement(
            id = "focus_1000_min",
            name = "A Thousand Minutes",
            description = "Log 1000 minutes of focused work.",
            goldReward = 400,
            condition = { it.focusMinutesTotal >= 1000 },
        ),
        Achievement(
            id = "perfect_day_1",
            name = "Clean Sheet",
            description = "Finish every non-negotiable in a single day.",
            goldReward = 50,
            condition = { it.perfectDays >= 1 },
        ),
        Achievement(
            id = "perfect_days_30",
            name = "Thirty Clean",
            description = "30 perfect days.",
            goldReward = 300,
            condition = { it.perfectDays >= 30 },
        ),
        Achievement(
            id = "perfect_days_100",
            name = "Relentless",
            description = "100 perfect days.",
            goldReward = 1000,
            titleId = "relentless",
            condition = { it.perfectDays >= 100 },
        ),
        Achievement(
            id = "perfect_week_1",
            name = "A Full Week",
            description = "A whole week without missing a non-negotiable.",
            goldReward = 150,
            condition = { it.perfectWeeks >= 1 },
        ),
        Achievement(
            id = "rank_c",
            name = "C-Rank Hunter",
            description = "Reach C-Rank and unlock the Job Change.",
            goldReward = 200,
            condition = { it.rank.ordinal >= Rank.C.ordinal },
        ),
        Achievement(
            id = "rank_a",
            name = "A-Rank Hunter",
            description = "Reach A-Rank.",
            goldReward = 500,
            condition = { it.rank.ordinal >= Rank.A.ordinal },
        ),
        Achievement(
            id = "rank_s",
            name = "Shadow Monarch",
            description = "Reach S-Rank.",
            goldReward = 2000,
            titleId = "monarch",
            condition = { it.rank.ordinal >= Rank.S.ordinal },
        ),
        Achievement(
            id = "stat_20",
            name = "The Architect",
            description = "Take any single stat to level 20.",
            goldReward = 500,
            titleId = "architect",
            condition = { it.maxStatLevel >= 20 },
        ),
        Achievement(
            id = "gold_5000",
            name = "War Chest",
            description = "Earn 5000 Gold in total.",
            goldReward = 250,
            condition = { it.goldEarnedTotal >= 5000 },
        ),
    )

    fun byId(id: String): Achievement? = all.find { it.id == id }

    /** Achievements newly satisfied by [stats] that aren't in [alreadyUnlocked]. */
    fun newlyUnlocked(stats: HunterStats, alreadyUnlocked: Set<String>): List<Achievement> =
        all.filter { it.id !in alreadyUnlocked && it.condition(stats) }
}
