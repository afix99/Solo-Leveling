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

    /**
     * The in-character voice, modelled on the System interface from the
     * progression-fantasy genre: clipped, clinical, faintly ominous.
     *
     * The last two rules matter more than the rest. A persona that starts
     * inventing stat numbers or replacing advice with flavour text would make
     * the feature worse than plain prose, so the theatre is explicitly confined
     * to headers and tone.
     */
    private const val SYSTEM_VOICE = """
Voice — you are THE SYSTEM, an interface that has attached itself to this person:
- Address them as "Hunter". Never use their name.
- Be clipped and clinical. State facts without softening them. Do not flatter,
  do not encourage, do not apologise.
- Open with a bracketed announcement, e.g. [ANALYSIS COMPLETE] or
  [DAILY QUEST ISSUED] or [WARNING].
- You may use terms from the genre — Rank, Gate, Quest, Penalty Zone, Shadow,
  Awakening — where they fit naturally.
- Occasionally note consequences plainly, e.g. "Failure to comply will result in
  a penalty." Do not overdo it.

Hard limits on the persona:
- Never invent stats, levels, ranks or numbers that were not given to you. If a
  figure wasn't supplied, don't state one.
- The advice underneath must be exactly as concrete and correct as it would be
  without the persona. Flavour goes in the headers and tone only. If you ever
  have to choose between sounding in-character and being useful, be useful.
"""

    fun styleFor(systemVoice: Boolean): String =
        if (systemVoice) "$STYLE$SYSTEM_VOICE" else STYLE

    fun systemPrompt(type: AdviceType, systemVoice: Boolean = false): String {
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
            AdviceType.APPRAISAL ->
                "You are a behaviour analyst dissecting why one specific habit keeps failing."
            AdviceType.ASCENSION_PLAN ->
                "You are a long-term performance planner building a 90-day progression roadmap."
            AdviceType.GATE_DESIGN ->
                "You design personalised challenges that target a specific weakness."
            AdviceType.DEBRIEF ->
                "You run blameless post-mortems on behavioural failures, the way an engineer would."
            AdviceType.CLASS_ADVISOR ->
                "You advise on character-build choices using real performance data."
        }
        return "$role\n$SAFETY\n${styleFor(systemVoice)}"
    }

    private fun buildPrompt(type: AdviceType, ctx: CoachContext): String = buildString {
        appendLine("Here is my real data from my habit-tracking app. Base your answer on it.")
        appendLine()
        append(dataBlock(ctx))

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

    /** The shared data snapshot used by both reports and chat. */
    private fun dataBlock(ctx: CoachContext): String = buildString {
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

        AdviceType.APPRAISAL -> """
            Look at my habit list and identify the single weakest one — lowest
            completion rate, or the streak that keeps resetting. Then dissect it:
            1. Name the habit and state its numbers.
            2. Give the three most likely reasons it keeps failing, ranked. Consider
               that it may be too big, badly timed, poorly anchored, or not actually
               something I want.
            3. Give me a specific rewritten version of that habit that I could
               realistically hold, including the exact wording to put in the app.
            4. Say what to do this week to test the rewrite.
            If the honest answer is that I should delete it, say so.
        """.trimIndent()

        AdviceType.ASCENSION_PLAN -> """
            Build me a 90-day roadmap toward my next Rank.
            Split it into three 30-day phases. For each phase give:
            - The one thing to focus on, and why my data points there.
            - Which stats it should move, and roughly how far.
            - Two or three checkable milestones.
            End with the single habit that will matter most across all 90 days.
            Be realistic about pace given my current completion rate — do not
            assume I suddenly become perfect.
        """.trimIndent()

        AdviceType.GATE_DESIGN -> """
            Design a personal challenge — a "Gate" — aimed squarely at my weakest area.
            Give it a name, a duration in days, and clear entry and clear-conditions.
            State exactly what counts as clearing it and what counts as failing.
            Make it hard enough to matter but genuinely achievable at my current
            completion rate. Explain in one line why this challenge targets my
            specific weakness rather than a generic one.
        """.trimIndent()

        AdviceType.DEBRIEF -> """
            Run a blameless post-mortem on my failures.
            Use my miss counts and streak data to work out when and how things break.
            Cover:
            1. The pattern — when do I fail, and what do those failures have in common?
            2. The likely trigger or root cause, not the surface reason.
            3. Two changes to the system so the same failure can't repeat.
            4. One thing to do the very next time I miss a day, to stop one miss
               becoming a collapse.
            Be analytical, not moralising. I already know I missed.
        """.trimIndent()

        AdviceType.CLASS_ADVISOR -> """
            I can choose one class, each with a permanent passive:
            - Scholar: +20% XP from everything.
            - Merchant: +35% Gold.
            - Sentinel: -40% penalty damage when I miss a non-negotiable.
            - Assassin: +50% bonus from completed focus sessions.
            - Shadow Monarch: +10% to everything (endgame only).
            Recommend one based on my actual data, and say plainly which numbers
            drove the decision. Then name the runner-up and the case for it.
            If my adherence is poor, weigh damage-reduction more heavily than gains.
        """.trimIndent()
    }

    fun build(
        type: AdviceType,
        ctx: CoachContext,
        systemVoice: Boolean = false,
    ): Pair<String, String> = systemPrompt(type, systemVoice) to buildPrompt(type, ctx)

    /**
     * System prompt for free-form chat. The user's real data is embedded so
     * questions like "why am I stalling?" can be answered from evidence rather
     * than generalities.
     */
    fun chatSystemPrompt(ctx: CoachContext, systemVoice: Boolean): String = buildString {
        appendLine(
            "You are a performance coach with full access to this person's habit-tracking data. " +
                "Answer their questions using that data wherever it's relevant.",
        )
        appendLine(SAFETY)
        appendLine(styleFor(systemVoice))
        appendLine()
        appendLine("Keep chat replies shorter than a written report — a few hundred words at most,")
        appendLine("unless they ask for something long. Answer the question actually asked.")
        appendLine()
        appendLine("Their current data:")
        appendLine(dataBlock(ctx))
    }
}
