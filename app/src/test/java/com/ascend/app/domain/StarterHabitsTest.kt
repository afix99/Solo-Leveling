package com.ascend.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterHabitsTest {

    @Test
    fun `suggests mind for reading and deep work`() {
        assertEquals(Stat.INT, StarterHabits.suggestStat("Read 20 minutes"))
        assertEquals(Stat.INT, StarterHabits.suggestStat("2+ hr deep work"))
        assertEquals(Stat.INT, StarterHabits.suggestStat("Study Japanese"))
    }

    @Test
    fun `suggests body for training habits`() {
        assertEquals(Stat.STR, StarterHabits.suggestStat("Workout"))
        assertEquals(Stat.STR, StarterHabits.suggestStat("Cold shower"))
        assertEquals(Stat.STR, StarterHabits.suggestStat("Gym session"))
    }

    @Test
    fun `suggests focus for attention habits`() {
        assertEquals(Stat.PER, StarterHabits.suggestStat("Meditate"))
        assertEquals(Stat.PER, StarterHabits.suggestStat("No phone until 11am"))
        assertEquals(Stat.PER, StarterHabits.suggestStat("Journal"))
    }

    @Test
    fun `suggests movement for step and cardio habits`() {
        assertEquals(Stat.AGI, StarterHabits.suggestStat("10k steps"))
        assertEquals(Stat.AGI, StarterHabits.suggestStat("Cycle to work"))
    }

    @Test
    fun `suggests health for sleep and food habits`() {
        assertEquals(Stat.VIT, StarterHabits.suggestStat("Bed before 11:30"))
        assertEquals(Stat.VIT, StarterHabits.suggestStat("Drink 2L water"))
        assertEquals(Stat.VIT, StarterHabits.suggestStat("No sugar"))
    }

    @Test
    fun `suggestion is case insensitive`() {
        assertEquals(Stat.INT, StarterHabits.suggestStat("READ A BOOK"))
        assertEquals(Stat.STR, StarterHabits.suggestStat("workout"))
    }

    @Test
    fun `falls back to health for unrecognised habits`() {
        assertEquals(Stat.VIT, StarterHabits.suggestStat("Xyzzy"))
        assertEquals(Stat.VIT, StarterHabits.suggestStat(""))
    }

    @Test
    fun `every starter pack has habits and a non-blank title`() {
        assertTrue(StarterHabits.all.isNotEmpty())
        StarterHabits.all.forEach { pack ->
            assertTrue("${pack.id} should have habits", pack.habits.isNotEmpty())
            assertTrue("${pack.id} should have a title", pack.title.isNotBlank())
            assertTrue("${pack.id} should have a subtitle", pack.subtitle.isNotBlank())
        }
    }

    @Test
    fun `starter pack ids are unique`() {
        val ids = StarterHabits.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `no starter pack contains duplicate habit names`() {
        StarterHabits.all.forEach { pack ->
            val names = pack.habits.map { it.name.lowercase() }
            assertEquals("${pack.id} has duplicate habit names", names.size, names.toSet().size)
        }
    }

    @Test
    fun `focus-enabled templates always carry a positive duration`() {
        StarterHabits.all.flatMap { it.habits }.forEach { template ->
            template.focusMinutes?.let {
                assertTrue("${template.name} should have a positive focus duration", it > 0)
            }
        }
    }

    @Test
    fun `every pack has at least one non-negotiable so penalties can apply`() {
        StarterHabits.all.forEach { pack ->
            assertTrue(
                "${pack.id} should include at least one non-negotiable",
                pack.habits.any { it.isNonNegotiable },
            )
        }
    }
}
