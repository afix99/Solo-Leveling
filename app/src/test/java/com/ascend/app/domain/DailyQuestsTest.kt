package com.ascend.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyQuestsTest {

    private fun name(id: Long) = "Habit $id"

    @Test
    fun `no quest when there are no habits`() {
        val quest = DailyQuests.generate(1L, emptyList(), emptyList(), emptyList(), ::name)
        assertNull(quest)
    }

    @Test
    fun `generates a quest when habits exist`() {
        val quest = DailyQuests.generate(1L, listOf(1L, 2L), listOf(3L), listOf(1L), ::name)
        assertNotNull(quest)
        assertTrue(quest!!.targetCount > 0)
        assertTrue(quest.xpReward > 0)
        assertTrue(quest.goldReward > 0)
        assertTrue(quest.description.isNotBlank())
    }

    @Test
    fun `same seed produces the same quest`() {
        val a = DailyQuests.generate(42L, listOf(1L, 2L), listOf(3L, 4L), listOf(1L), ::name)
        val b = DailyQuests.generate(42L, listOf(1L, 2L), listOf(3L, 4L), listOf(1L), ::name)
        assertEquals(a, b)
    }

    @Test
    fun `different seeds eventually produce different quests`() {
        val quests = (1L..40L).map {
            DailyQuests.generate(it, listOf(1L, 2L), listOf(3L, 4L), listOf(1L), ::name)
        }.toSet()
        assertTrue("quests should vary day to day", quests.size > 1)
    }

    @Test
    fun `perfect day progress counts completed non-negotiables`() {
        val progress = DailyQuests.progress(
            kind = DailyQuestKind.PERFECT_DAY,
            targetHabitId = null,
            targetCount = 3,
            completedHabitIds = setOf(1L, 2L),
            nonNegotiables = listOf(1L, 2L, 3L),
            optionalHabits = emptyList(),
            focusSessionsToday = 0,
        )
        assertEquals(2, progress)
    }

    @Test
    fun `target habit progress is one when that habit is done`() {
        val done = DailyQuests.progress(
            kind = DailyQuestKind.TARGET_HABIT,
            targetHabitId = 7L,
            targetCount = 1,
            completedHabitIds = setOf(7L),
            nonNegotiables = listOf(7L),
            optionalHabits = emptyList(),
            focusSessionsToday = 0,
        )
        assertEquals(1, done)

        val notDone = DailyQuests.progress(
            kind = DailyQuestKind.TARGET_HABIT,
            targetHabitId = 7L,
            targetCount = 1,
            completedHabitIds = setOf(8L),
            nonNegotiables = listOf(7L),
            optionalHabits = emptyList(),
            focusSessionsToday = 0,
        )
        assertEquals(0, notDone)
    }

    @Test
    fun `progress never exceeds the target`() {
        val progress = DailyQuests.progress(
            kind = DailyQuestKind.TOTAL_COUNT,
            targetHabitId = null,
            targetCount = 2,
            completedHabitIds = setOf(1L, 2L, 3L, 4L),
            nonNegotiables = listOf(1L, 2L),
            optionalHabits = listOf(3L, 4L),
            focusSessionsToday = 0,
        )
        assertEquals(2, progress)
    }

    @Test
    fun `focus session progress reads the session count`() {
        val progress = DailyQuests.progress(
            kind = DailyQuestKind.FOCUS_SESSION,
            targetHabitId = null,
            targetCount = 1,
            completedHabitIds = emptySet(),
            nonNegotiables = listOf(1L),
            optionalHabits = emptyList(),
            focusSessionsToday = 2,
        )
        assertEquals(1, progress)
    }

    @Test
    fun `quests only reference habits that exist`() {
        val nonNegotiables = listOf(1L, 2L)
        val optional = listOf(3L)
        val valid = (nonNegotiables + optional).toSet()
        (1L..60L).forEach { seed ->
            val quest = DailyQuests.generate(seed, nonNegotiables, optional, listOf(1L), ::name)
            quest?.targetHabitId?.let {
                assertTrue("quest targeted an unknown habit", it in valid)
            }
        }
    }
}
