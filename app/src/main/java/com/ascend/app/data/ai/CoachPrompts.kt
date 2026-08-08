package com.ascend.app.data.ai

import com.ascend.app.domain.AdviceType
import com.ascend.app.domain.AthleteProfile
import com.ascend.app.domain.Rank
import com.ascend.app.domain.Stat

/** A snapshot of everything the coach is allowed to reason about. */
data class CoachContext(
    val hunterName: String,
    val hunterLevel: Int,
    val rank: Rank,
    val statLevels: Map<Stat, Int>,
    val habits: List<HabitSnapshot>,
    val perfectDays: Int,
    val longestStreak: Int,
    val focusSessions: Int,
    val focusMinutes: Int,
    val missesLast30Days: Int,
    val completionRateLast30: Int,
    val athlete: AthleteProfile,
)

data class HabitSnapshot(
    val name: String,
    val stat: Stat,
    val isNonNegotiable: Boolean,
    val completionRate: Int,
    val currentStreak: Int,
    val focusMinutes: Int?,
)

object CoachPrompts {

    /**
     * Shared guardrails. Fitness and nutrition guidance is ordinary for a
     * training app, but an LLM left unconstrained will happily prescribe crash
     * deficits or train through injuries, so the boundaries are stated up front
     * rather than hoped for.
     */
    private const val SAFETY = """
Rules you must follow:
- Give practical, specific, actionable guidance. No vague filler.
- Never recommend a calorie intake below roughly 1500 kcal/day for men or 1200 for women, and never a deficit steeper than about 1% of bodyweight per week.
- Never recommend training through pain or an injury. If injuries are mentioned, work around them and say to get them properly assessed.
- Do not diagnose anything or give medical advice. If something sounds like a medical issue, say plainly that it needs a doctor.
- Do not recommend supplements beyond well-established basics (creatine monohydrate, vitamin D, protein powder), and note these are optional.
- If the user's data is thin, say what you'd need rather than inventing detail.
"""

    private const val STYLE = """
Style:
- Write in plain English. Short paragraphs, concrete numbers, no hype.
- Use markdown headings and bullet points.
- Keep it under roughly 500 words unless a schedule genuinely needs more.
- Address the user directly as "you". Do not open with a greeting or a preamble.
"""

    fun systemPrompt(type: AdviceType): String {
        val role = when (type) {
            AdviceType.TRAINING_PLAN ->
                "You are an experienced strength coach writing a weekly training plan."
            AdviceType.NUTRITION ->
                "You are a sports nutrition coach writing an eating schedule for muscle growth."
            AdviceType.SLEEP ->
                "You are a recovery and sleep coach."
            AdviceType.READING ->
                "You are a learning-science tutor specialising in reading speed and retention."
            AdviceType.DEEP_WORK ->
                "You are a focus and attention coach grounded in the deep-work and flow literature."
            AdviceType.HABIT_DESIGN ->
                "You are a behaviour-design coach who builds habit systems from real adherence data."
            AdviceType.DATA_REVIEW ->
                "You are a performance analyst reviewing a person's habit-tracking data."
        }
        return "$role\n$SAFETY\n$STYLE"
    }

    private fun buildPrompt(type: AdviceType, ctx: CoachContext): String = buildString {
        appendLine("Here is my real data from my habit-tracking app. Base your answer on it.")
        appendLine()

        appendLine("## Progression")
        appendLine("- Level ${ctx.hunterLevel}, ${ctx.rank.displayName}")
        ctx.statLevels.forEach { (stat, level) ->
            appendLine("- ${stat.plainName} (${stat.description}): level $level")
        }
        appendLine()

        appendLine("## Adherence")
        appendLine("- Completion rate over the last 30 days: ${ctx.completionRateLast30}%")
        appendLine("- Missed non-negotiables in the last 30 days: ${ctx.missesLast30Days}")
        appendLine("- Perfect days all-time: ${ctx.perfectDays}")
        appendLine("- Longest streak: ${ctx.longestStreak} days")
        appendLine("- Focus sessions completed: ${ctx.focusSessions} (${ctx.focusMinutes} minutes total)")
        appendLine()

        appendLine("## My habits")
        if (ctx.habits.isEmpty()) {
            appendLine("- (none set up yet)")
        } else {
            ctx.habits.forEach { h ->
                val kind = if (h.isNonNegotiable) "non-negotiable" else "optional"
                val focus = h.focusMinutes?.let { ", ${it}min timed session" } ?: ""
                appendLine(
                    "- \"${h.name}\" — $kind, ${h.stat.plainName}, " +
                        "${h.completionRate}% completed, ${h.currentStreak}-day streak$focus",
                )
            }
        }
        appendLine()

        if (type.needsAthleteProfile) {
            appendLine("## About me")
            if (ctx.athlete.isEmpty) {
                appendLine("- I haven't filled in my body details. Ask me for what you need, and")
                appendLine("  give the best general answer you can in the meantime.")
            } else {
                with(ctx.athlete) {
                    age?.let { appendLine("- Age: $it") }
                    sex?.takeIf { it.isNotBlank() }?.let { appendLine("- Sex: $it") }
                    heightCm?.let { appendLine("- Height: ${it}cm") }
                    weightKg?.let { appendLine("- Weight: ${it}kg") }
                    goal?.takeIf { it.isNotBlank() }?.let { appendLine("- Goal: $it") }
                    experience?.takeIf { it.isNotBlank() }?.let { appendLine("- Training experience: $it") }
                    equipment?.takeIf { it.isNotBlank() }?.let { appendLine("- Equipment available: $it") }
                    dietaryNotes?.takeIf { it.isNotBlank() }?.let { appendLine("- Dietary notes: $it") }
                    injuries?.takeIf { it.isNotBlank() }?.let { appendLine("- Injuries / limitations: $it") }
                }
            }
            appendLine()
        }

        appendLine("## What I want")
        appendLine(task(type))
    }

    private fun task(type: AdviceType): String = when (type) {
        AdviceType.DATA_REVIEW -> """
            Review the data above and tell me:
            1. What is genuinely working, with the numbers that show it.
            2. The single biggest weakness, and why you picked it.
            3. Three specific changes for the next two weeks.
            Be blunt. If my adherence is poor, say so.
        """.trimIndent()

        AdviceType.TRAINING_PLAN -> """
            Write me a weekly training plan for muscle growth.
            Include the split by day, the exercises, sets, reps, and rough rest times.
            Match it to the equipment I have and my experience level.
            Say how to progress the load week to week.
            Then tell me which habits I should add to this app to track it.
        """.trimIndent()

        AdviceType.NUTRITION -> """
            Write me an eating schedule for muscle growth.
            Give me a daily calorie target and a protein target in grams, and show
            how you calculated them from my stats.
            Lay out meal timing across the day, including around training.
            Give example meals that fit my dietary notes.
            Then tell me which habits I should add to this app to track it.
        """.trimIndent()

        AdviceType.READING -> """
            Give me a concrete system for reading faster and retaining more.
            Cover: how to choose what to read, an active-reading method, note-taking,
            and a review schedule that actually fits spaced repetition.
            Tie it to how much I currently read according to my data.
        """.trimIndent()

        AdviceType.DEEP_WORK -> """
            Based on my focus session history, tell me how to extend my focus and
            get more from each session. Cover session length progression, what to do
            about distractions, and how to structure a working block.
            Give me a target session length to aim for next.
        """.trimIndent()

        AdviceType.HABIT_DESIGN -> """
            Audit my habit list. Tell me:
            1. Which habits to cut, and why the data says so.
            2. Which to keep but change (make easier, harder, or re-time).
            3. What is missing given my goals, with specific habit names I can add.
            Consider whether I have too many non-negotiables to sustain.
        """.trimIndent()

        AdviceType.SLEEP -> """
            Give me a sleep and recovery plan. Cover a target sleep window, a
            wind-down routine, and what to change given my current wake/sleep habits
            and training load. Flag anything in my data that is likely hurting recovery.
        """.trimIndent()
    }

    fun build(type: AdviceType, ctx: CoachContext): Pair<String, String> =
        systemPrompt(type) to buildPrompt(type, ctx)
}
