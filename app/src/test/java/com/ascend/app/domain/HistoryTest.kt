package com.ascend.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTest {

    private val today = LocalDate.of(2026, 8, 9) // a Sunday

    private fun log(
        date: LocalDate,
        completed: Boolean = true,
        habitId: Long = 1,
        nonNegotiable: Boolean = true,
        xp: Int = 20,
    ) = History.LogFact(habitId, date, completed, nonNegotiable, xp, gold = 10)

    // ---- Day summaries -----------------------------------------------------

    @Test
    fun `summaries cover every day in the window even with no logs`() {
        val summaries = History.dailySummaries(today, 30, emptyMap()) { 2 }
        assertEquals(30, summaries.size)
        assertEquals(today.minusDays(29), summaries.first().date)
        assertEquals(today, summaries.last().date)
    }

    @Test
    fun `a day is perfect only when every non-negotiable is done`() {
        val logs = mapOf(today to listOf(log(today, habitId = 1), log(today, habitId = 2)))
        val perfect = History.dailySummaries(today, 1, logs) { 2 }.single()
        assertTrue(perfect.isPerfect)

        val partial = History.dailySummaries(today, 1, logs) { 3 }.single()
        assertFalse("2 of 3 must not count as perfect", partial.isPerfect)
    }

    @Test
    fun `a day with no non-negotiables is never perfect`() {
        // Otherwise a user with zero non-negotiables would show a perfect
        // streak for doing nothing at all.
        val summary = History.dailySummaries(today, 1, emptyMap()) { 0 }.single()
        assertFalse(summary.isPerfect)
    }

    @Test
    fun `intensity reflects the share of the day cleared`() {
        val logs = mapOf(today to listOf(log(today, habitId = 1)))
        assertEquals(0.25f, History.dailySummaries(today, 1, logs) { 4 }.single().intensity, 0.001f)
    }

    @Test
    fun `an untracked day is distinguishable from a failed one`() {
        // The heatmap draws these differently on purpose: "wasn't using the
        // app" and "used it and missed" are not the same story.
        val untracked = History.dailySummaries(today, 1, emptyMap()) { 0 }.single()
        assertTrue(untracked.isUntracked)

        val failed = History.dailySummaries(today, 1, emptyMap()) { 3 }.single()
        assertFalse("a logged miss is not untracked", failed.isUntracked)
    }

    // ---- Streaks -----------------------------------------------------------

    @Test
    fun `today being unfinished does not break a live streak`() {
        // The streak must survive the morning, or every day starts by telling
        // the user they have lost their run.
        val logs = (1..5).map { log(today.minusDays(it.toLong())) }
        assertEquals(5, History.currentStreakOf(logs, today))
    }

    @Test
    fun `a missed yesterday does break the streak`() {
        val logs = listOf(log(today.minusDays(2)), log(today.minusDays(1), completed = false))
        assertEquals(0, History.currentStreakOf(logs, today))
    }

    @Test
    fun `today counts toward the streak once it is done`() {
        val logs = listOf(log(today.minusDays(1)), log(today))
        assertEquals(2, History.currentStreakOf(logs, today))
    }

    @Test
    fun `best streak finds the longest consecutive run, not the total`() {
        val logs = listOf(
            log(today.minusDays(10)), log(today.minusDays(9)), log(today.minusDays(8)),
            // gap
            log(today.minusDays(5)), log(today.minusDays(4)),
        )
        assertEquals(3, History.bestStreakOf(logs))
    }

    @Test
    fun `perfect runs report both the best ever and the live one`() {
        val summaries = List(10) { i ->
            val date = today.minusDays((9 - i).toLong())
            // Perfect on days 0-3 and 7-9, so best = 4 and current = 3.
            val perfect = i <= 3 || i >= 7
            History.DaySummary(date, 1, if (perfect) 1 else 0, 0, 20, 10)
        }
        val (longest, current) = History.perfectRuns(summaries)
        assertEquals(4, longest)
        assertEquals(3, current)
    }

    // ---- Weekday pattern ---------------------------------------------------

    @Test
    fun `weekday pattern ignores untracked days`() {
        // Counting days before the habit existed as failures would invent a
        // pattern that never happened.
        val summaries = listOf(
            History.DaySummary(LocalDate.of(2026, 8, 2), 0, 0, 0, 0, 0), // Sun, untracked
            History.DaySummary(LocalDate.of(2026, 8, 9), 2, 2, 0, 40, 20), // Sun, perfect
        )
        val sunday = History.weekdayPattern(summaries).first { it.day == DayOfWeek.SUNDAY }
        assertEquals(100, sunday.rate)
        assertEquals("only the tracked Sunday should count", 1, sunday.daysRecorded)
    }

    @Test
    fun `weekday pattern always returns all seven days`() {
        val pattern = History.weekdayPattern(emptyList())
        assertEquals(7, pattern.size)
        assertTrue("empty data must not report false rates", pattern.all { it.rate == 0 })
    }

    // ---- Momentum ----------------------------------------------------------

    @Test
    fun `momentum compares the recent window against the one before it`() {
        val summaries = (0 until 28).map { i ->
            val date = today.minusDays(i.toLong())
            // Recent 14 days perfect, prior 14 days all missed.
            val done = if (i < History.TREND_WINDOW_DAYS) 1 else 0
            History.DaySummary(date, 1, done, 0, done * 20, done * 10)
        }
        val momentum = History.momentum(summaries, today)
        assertEquals(100, momentum.recentRate)
        assertEquals(0, momentum.priorRate)
        assertEquals(100, momentum.changePoints)
    }

    @Test
    fun `momentum on an empty history reports zero rather than dividing by zero`() {
        val momentum = History.momentum(emptyList(), today)
        assertEquals(0, momentum.recentRate)
        assertEquals(0, momentum.changePoints)
    }

    // ---- Habit trends ------------------------------------------------------

    @Test
    fun `a habit that decayed in the last fortnight is flagged as slipping`() {
        val logs = (0 until 28).map { i ->
            log(today.minusDays(i.toLong()), completed = i >= History.TREND_WINDOW_DAYS)
        }
        val trend = History.habitTrends(
            listOf(History.HabitFacts(1, "Gym", Stat.STR, true, logs)),
            today,
        ).single()

        assertTrue("expected a slip to be detected", trend.isSlipping)
        assertTrue(trend.trendPoints < 0)
        assertEquals(0, trend.recentRate)
        assertEquals(100, trend.priorRate)
    }

    @Test
    fun `an improving habit is not reported as slipping`() {
        val logs = (0 until 28).map { i ->
            log(today.minusDays(i.toLong()), completed = i < History.TREND_WINDOW_DAYS)
        }
        val trend = History.habitTrends(
            listOf(History.HabitFacts(1, "Read", Stat.INT, true, logs)),
            today,
        ).single()

        assertFalse(trend.isSlipping)
        assertTrue(trend.isRising)
    }

    @Test
    fun `a habit with no logs reports zero rather than crashing`() {
        val trend = History.habitTrends(
            listOf(History.HabitFacts(1, "New habit", Stat.AGI, false, emptyList())),
            today,
        ).single()

        assertEquals(0, trend.completionRate)
        assertEquals(0, trend.currentStreak)
        assertEquals(0, trend.bestStreak)
        assertFalse("no data must not read as a slip", trend.isSlipping)
    }

    @Test
    fun `small wobbles are not reported as slipping`() {
        // The threshold exists so normal week-to-week variation doesn't get
        // dressed up as a decline the user needs to act on.
        val logs = (0 until 28).map { i ->
            // One miss in fourteen: ~92% against 100%, an 8-point dip.
            val completed = if (i < History.TREND_WINDOW_DAYS) i != 0 else true
            log(today.minusDays(i.toLong()), completed = completed)
        }
        val trend = History.habitTrends(
            listOf(History.HabitFacts(1, "Walk", Stat.AGI, false, logs)),
            today,
        ).single()

        assertTrue("an 8-point dip should stay under the threshold", trend.trendPoints > -History.SLIP_THRESHOLD)
        assertFalse(trend.isSlipping)
    }
}
