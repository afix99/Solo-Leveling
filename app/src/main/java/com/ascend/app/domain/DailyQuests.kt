package com.ascend.app.domain

import kotlin.random.Random

enum class DailyQuestKind {
    /** Finish every non-negotiable today. */
    PERFECT_DAY,
    /** Complete a specific habit. */
    TARGET_HABIT,
    /** Complete any N optional habits. */
    OPTIONAL_COUNT,
    /** Finish a focus session. */
    FOCUS_SESSION,
    /** Complete any N habits at all. */
    TOTAL_COUNT,
}

/** A quest The System issues for a single day, on top of the normal list. */
data class GeneratedQuest(
    val kind: DailyQuestKind,
    val description: String,
    val targetCount: Int,
    val targetHabitId: Long?,
    val xpReward: Int,
    val goldReward: Int,
)

/**
 * Builds one bonus quest per day. Seeded by the date so the quest is stable
 * across app restarts within a day, but different day to day — the novelty
 * that a fixed habit list can't provide on its own.
 */
object DailyQuests {

    fun generate(
        dateSeed: Long,
        nonNegotiables: List<Long>,
        optionalHabits: List<Long>,
        focusHabits: List<Long>,
        habitNameOf: (Long) -> String,
    ): GeneratedQuest? {
        val totalHabits = nonNegotiables.size + optionalHabits.size
        if (totalHabits == 0) return null

        val random = Random(dateSeed)
        val candidates = buildList {
            if (nonNegotiables.isNotEmpty()) add(DailyQuestKind.PERFECT_DAY)
            if (nonNegotiables.isNotEmpty() || optionalHabits.isNotEmpty()) add(DailyQuestKind.TARGET_HABIT)
            if (optionalHabits.size >= 2) add(DailyQuestKind.OPTIONAL_COUNT)
            if (focusHabits.isNotEmpty()) add(DailyQuestKind.FOCUS_SESSION)
            if (totalHabits >= 3) add(DailyQuestKind.TOTAL_COUNT)
        }
        if (candidates.isEmpty()) return null

        return when (candidates[random.nextInt(candidates.size)]) {
            DailyQuestKind.PERFECT_DAY -> GeneratedQuest(
                kind = DailyQuestKind.PERFECT_DAY,
                description = "Clear every non-negotiable today.",
                targetCount = nonNegotiables.size,
                targetHabitId = null,
                xpReward = 40,
                goldReward = Economy.DAILY_QUEST_GOLD,
            )

            DailyQuestKind.TARGET_HABIT -> {
                val pool = nonNegotiables + optionalHabits
                val habitId = pool[random.nextInt(pool.size)]
                GeneratedQuest(
                    kind = DailyQuestKind.TARGET_HABIT,
                    description = "Complete “${habitNameOf(habitId)}”.",
                    targetCount = 1,
                    targetHabitId = habitId,
                    xpReward = 25,
                    goldReward = Economy.DAILY_QUEST_GOLD,
                )
            }

            DailyQuestKind.OPTIONAL_COUNT -> {
                val target = minOf(2, optionalHabits.size)
                GeneratedQuest(
                    kind = DailyQuestKind.OPTIONAL_COUNT,
                    description = "Complete any $target optional habits.",
                    targetCount = target,
                    targetHabitId = null,
                    xpReward = 30,
                    goldReward = Economy.DAILY_QUEST_GOLD,
                )
            }

            DailyQuestKind.FOCUS_SESSION -> GeneratedQuest(
                kind = DailyQuestKind.FOCUS_SESSION,
                description = "Finish one full focus session.",
                targetCount = 1,
                targetHabitId = null,
                xpReward = 35,
                goldReward = Economy.DAILY_QUEST_GOLD,
            )

            DailyQuestKind.TOTAL_COUNT -> {
                val target = minOf(3, totalHabits)
                GeneratedQuest(
                    kind = DailyQuestKind.TOTAL_COUNT,
                    description = "Complete any $target habits today.",
                    targetCount = target,
                    targetHabitId = null,
                    xpReward = 30,
                    goldReward = Economy.DAILY_QUEST_GOLD,
                )
            }
        }
    }

    /** How far along today's quest is, given what's been done. */
    fun progress(
        kind: DailyQuestKind,
        targetHabitId: Long?,
        targetCount: Int,
        completedHabitIds: Set<Long>,
        nonNegotiables: List<Long>,
        optionalHabits: List<Long>,
        focusSessionsToday: Int,
    ): Int = when (kind) {
        DailyQuestKind.PERFECT_DAY -> nonNegotiables.count { it in completedHabitIds }
        DailyQuestKind.TARGET_HABIT -> if (targetHabitId in completedHabitIds) 1 else 0
        DailyQuestKind.OPTIONAL_COUNT -> optionalHabits.count { it in completedHabitIds }
        DailyQuestKind.FOCUS_SESSION -> focusSessionsToday.coerceAtMost(targetCount)
        DailyQuestKind.TOTAL_COUNT -> completedHabitIds.size
    }.coerceAtMost(targetCount)
}
