package com.ascend.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * History analytics, computed on-device from the logs the app already holds.
 *
 * Deliberately duplicated in spirit with the server's /api/report rather than
 * depending on it: the History screen has to work on a fresh install, on a
 * plane, and for anyone who never sets up a backup. The server's version
 * exists to reach further back than the phone keeps handy and to feed the
 * coach — not to make the UI depend on a network round trip.
 *
 * Pure Kotlin, no Android imports, so every rule below is unit-testable.
 */
object History {

    /** One day's roll-up, as the heatmap and trend charts consume it. */
    data class DaySummary(
        val date: LocalDate,
        val nonNegotiableTotal: Int,
        val nonNegotiableDone: Int,
        val optionalDone: Int,
        val xp: Int,
        val gold: Int,
    ) {
        val isPerfect: Boolean
            get() = nonNegotiableTotal > 0 && nonNegotiableDone >= nonNegotiableTotal

        /** 0f..1f share of the day's non-negotiables cleared. */
        val intensity: Float
            get() = if (nonNegotiableTotal <= 0) 0f
            else (nonNegotiableDone.toFloat() / nonNegotiableTotal).coerceIn(0f, 1f)

        /** True when the day has no record at all — drawn differently from a
         * day that was logged and failed, because "didn't use the app" and
         * "used it and missed" are not the same story. */
        val isUntracked: Boolean
            get() = nonNegotiableTotal == 0 && optionalDone == 0 && xp == 0
    }

    /** Per-habit reliability, plus whether it is drifting. */
    data class HabitTrend(
        val habitId: Long,
        val name: String,
        val stat: Stat,
        val isNonNegotiable: Boolean,
        val completionRate: Int,
        val recentRate: Int,
        val priorRate: Int,
        val currentStreak: Int,
        val bestStreak: Int,
    ) {
        /** Positive improving, negative slipping. */
        val trendPoints: Int get() = recentRate - priorRate

        /** Big enough to be worth surfacing rather than noise. */
        val isSlipping: Boolean get() = trendPoints <= -SLIP_THRESHOLD
        val isRising: Boolean get() = trendPoints >= SLIP_THRESHOLD
    }

    data class WeekdayStat(val day: DayOfWeek, val rate: Int, val daysRecorded: Int)

    /** Movement in the last window against the window before it. */
    data class Momentum(val recentRate: Int, val priorRate: Int) {
        val changePoints: Int get() = recentRate - priorRate
    }

    const val SLIP_THRESHOLD = 15
    const val TREND_WINDOW_DAYS = 14

    /**
     * Builds one summary per day across [days] ending at [endDate], including
     * days with no logs so the heatmap keeps a continuous calendar.
     *
     * [nonNegotiableCountOn] is a function rather than a constant because the
     * number of non-negotiables changes as the user edits their habits; using
     * today's count for a month ago would silently rewrite history.
     */
    fun dailySummaries(
        endDate: LocalDate,
        days: Int,
        logsByDate: Map<LocalDate, List<LogFact>>,
        nonNegotiableCountOn: (LocalDate) -> Int,
    ): List<DaySummary> {
        val start = endDate.minusDays((days - 1).toLong())
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endDate) }
            .map { date ->
                val logs = logsByDate[date].orEmpty()
                DaySummary(
                    date = date,
                    nonNegotiableTotal = nonNegotiableCountOn(date),
                    nonNegotiableDone = logs.count { it.completed && it.isNonNegotiable },
                    optionalDone = logs.count { it.completed && !it.isNonNegotiable },
                    xp = logs.sumOf { it.xp },
                    gold = logs.sumOf { it.gold },
                )
            }
            .toList()
    }

    /** The minimum a log needs to expose for history maths. */
    data class LogFact(
        val habitId: Long,
        val date: LocalDate,
        val completed: Boolean,
        val isNonNegotiable: Boolean,
        val xp: Int,
        val gold: Int,
    )

    /**
     * Completion rate per weekday over the supplied summaries.
     *
     * Untracked days are excluded: counting days before a habit existed as
     * failures would invent a pattern that never happened.
     */
    fun weekdayPattern(summaries: List<DaySummary>): List<WeekdayStat> {
        val tracked = summaries.filterNot { it.isUntracked }
        return DayOfWeek.entries.map { day ->
            val forDay = tracked.filter { it.date.dayOfWeek == day }
            val total = forDay.sumOf { it.nonNegotiableTotal }
            val done = forDay.sumOf { it.nonNegotiableDone }
            WeekdayStat(
                day = day,
                rate = if (total <= 0) 0 else ((done * 100) / total),
                daysRecorded = forDay.size,
            )
        }
    }

    /** Adherence in the last [TREND_WINDOW_DAYS] against the window before. */
    fun momentum(summaries: List<DaySummary>, endDate: LocalDate): Momentum {
        fun rate(from: LocalDate, to: LocalDate): Int {
            val window = summaries.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
            val total = window.sumOf { it.nonNegotiableTotal }
            val done = window.sumOf { it.nonNegotiableDone }
            return if (total <= 0) 0 else (done * 100) / total
        }

        val recentFrom = endDate.minusDays((TREND_WINDOW_DAYS - 1).toLong())
        val priorTo = recentFrom.minusDays(1)
        val priorFrom = priorTo.minusDays((TREND_WINDOW_DAYS - 1).toLong())
        return Momentum(rate(recentFrom, endDate), rate(priorFrom, priorTo))
    }

    /**
     * Longest run of consecutive perfect days, and the current live run.
     *
     * The current run counts backwards from the end and stops at the first
     * non-perfect day — so a streak stays alive through today even before
     * today is finished, which is what the user expects to see.
     */
    fun perfectRuns(summaries: List<DaySummary>): Pair<Int, Int> {
        var longest = 0
        var running = 0
        for (day in summaries) {
            if (day.isPerfect) {
                running += 1
                if (running > longest) longest = running
            } else {
                running = 0
            }
        }

        var current = 0
        for (day in summaries.asReversed()) {
            if (day.isPerfect) current += 1 else break
        }
        return longest to current
    }

    /** Per-habit rates over the full window and the two trend windows. */
    fun habitTrends(
        habits: List<HabitFacts>,
        endDate: LocalDate,
    ): List<HabitTrend> {
        val recentFrom = endDate.minusDays((TREND_WINDOW_DAYS - 1).toLong())
        val priorTo = recentFrom.minusDays(1)
        val priorFrom = priorTo.minusDays((TREND_WINDOW_DAYS - 1).toLong())

        return habits.map { habit ->
            fun rate(from: LocalDate, to: LocalDate): Int {
                val window = habit.logs.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
                if (window.isEmpty()) return 0
                return (window.count { it.completed } * 100) / window.size
            }

            HabitTrend(
                habitId = habit.habitId,
                name = habit.name,
                stat = habit.stat,
                isNonNegotiable = habit.isNonNegotiable,
                completionRate = if (habit.logs.isEmpty()) 0
                else (habit.logs.count { it.completed } * 100) / habit.logs.size,
                recentRate = rate(recentFrom, endDate),
                priorRate = rate(priorFrom, priorTo),
                currentStreak = currentStreakOf(habit.logs, endDate),
                bestStreak = bestStreakOf(habit.logs),
            )
        }
    }

    data class HabitFacts(
        val habitId: Long,
        val name: String,
        val stat: Stat,
        val isNonNegotiable: Boolean,
        val logs: List<LogFact>,
    )

    /**
     * Live streak, counted back from [endDate].
     *
     * Today not being done yet does not break the streak — only a *past* day
     * does. Otherwise every streak would appear broken every morning, which
     * is both wrong and demoralising.
     */
    internal fun currentStreakOf(logs: List<LogFact>, endDate: LocalDate): Int {
        val byDate = logs.associateBy { it.date }
        var streak = 0
        var cursor = endDate
        if (byDate[cursor]?.completed != true) cursor = cursor.minusDays(1)
        while (byDate[cursor]?.completed == true) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    internal fun bestStreakOf(logs: List<LogFact>): Int {
        val done = logs.filter { it.completed }.map { it.date }.sorted()
        if (done.isEmpty()) return 0
        var best = 1
        var run = 1
        for (i in 1 until done.size) {
            run = if (done[i] == done[i - 1].plusDays(1)) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }
}
