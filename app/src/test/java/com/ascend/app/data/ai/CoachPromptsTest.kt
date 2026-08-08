package com.ascend.app.data.ai

import com.ascend.app.domain.AdviceType
import com.ascend.app.domain.AthleteProfile
import com.ascend.app.domain.Rank
import com.ascend.app.domain.Stat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachPromptsTest {

    private val ctx = CoachContext(
        hunterName = "Afiq",
        hunterLevel = 4,
        rank = Rank.E,
        statLevels = Stat.entries.associateWith { 3 },
        habits = listOf(
            HabitSnapshot("Cold shower", Stat.STR, true, 40, 2, null),
            HabitSnapshot("Read 20 minutes", Stat.INT, true, 90, 12, 20),
        ),
        perfectDays = 5,
        longestStreak = 12,
        focusSessions = 8,
        focusMinutes = 190,
        missesLast30Days = 7,
        completionRateLast30 = 72,
        athlete = AthleteProfile(age = 24, weightKg = 70, goal = "Build muscle"),
    )

    @Test
    fun `every advice type produces a non-empty prompt pair`() {
        AdviceType.entries.forEach { type ->
            val (system, user) = CoachPrompts.build(type, ctx)
            assertTrue("${type.name} system prompt empty", system.isNotBlank())
            assertTrue("${type.name} user prompt empty", user.isNotBlank())
        }
    }

    @Test
    fun `real user data is embedded rather than described abstractly`() {
        val (_, user) = CoachPrompts.build(AdviceType.DATA_REVIEW, ctx)
        assertTrue("habit names missing", user.contains("Cold shower"))
        assertTrue("completion rate missing", user.contains("72%"))
        assertTrue("miss count missing", user.contains("7"))
        assertTrue("streak missing", user.contains("12"))
    }

    @Test
    fun `safety rules are present on every advice type`() {
        AdviceType.entries.forEach { type ->
            val system = CoachPrompts.systemPrompt(type)
            assertTrue("${type.name} lost the calorie floor", system.contains("1500"))
            assertTrue("${type.name} lost the injury rule", system.contains("injur"))
            assertTrue("${type.name} lost the medical disclaimer", system.contains("diagnose"))
        }
    }

    @Test
    fun `safety rules survive the System persona`() {
        // The persona must never be able to displace the guardrails — that would
        // trade advice quality for flavour, which is the wrong direction.
        AdviceType.entries.forEach { type ->
            val system = CoachPrompts.systemPrompt(type, systemVoice = true)
            assertTrue("${type.name} lost the calorie floor", system.contains("1500"))
            assertTrue("${type.name} lost the injury rule", system.contains("injur"))
        }
    }

    @Test
    fun `system voice adds persona instructions and plain voice does not`() {
        val voiced = CoachPrompts.systemPrompt(AdviceType.DATA_REVIEW, systemVoice = true)
        val plain = CoachPrompts.systemPrompt(AdviceType.DATA_REVIEW, systemVoice = false)
        assertTrue(voiced.contains("THE SYSTEM"))
        assertFalse(plain.contains("THE SYSTEM"))
    }

    @Test
    fun `persona is explicitly forbidden from inventing numbers`() {
        val voiced = CoachPrompts.styleFor(systemVoice = true)
        assertTrue(voiced.contains("Never invent stats"))
        assertTrue("usefulness must outrank the persona", voiced.contains("be useful"))
    }

    @Test
    fun `body details only appear for advice that needs them`() {
        val (_, training) = CoachPrompts.build(AdviceType.TRAINING_PLAN, ctx)
        assertTrue("training plan should know bodyweight", training.contains("70kg"))

        val (_, reading) = CoachPrompts.build(AdviceType.READING, ctx)
        assertFalse("reading advice shouldn't receive body details", reading.contains("70kg"))
    }

    @Test
    fun `chat prompt carries the same data and guardrails`() {
        val prompt = CoachPrompts.chatSystemPrompt(ctx, systemVoice = false)
        assertTrue("chat lost the data", prompt.contains("Cold shower"))
        assertTrue("chat lost the safety rules", prompt.contains("1500"))
        assertTrue("chat lost adherence figures", prompt.contains("72%"))
    }

    @Test
    fun `an empty athlete profile asks rather than inventing details`() {
        val bare = ctx.copy(athlete = AthleteProfile())
        val (_, user) = CoachPrompts.build(AdviceType.NUTRITION, bare)
        assertTrue(user.contains("haven't filled in my body details"))
    }

    @Test
    fun `each advice type asks a distinct question`() {
        val tasks = AdviceType.entries.map { CoachPrompts.build(it, ctx).second }
        assertTrue("advice types are producing duplicate prompts", tasks.toSet().size == tasks.size)
    }
}
