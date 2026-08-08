package com.ascend.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ascend.app.AscendApplication
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first

private fun Context.repository() = (applicationContext as AscendApplication).repository

/** One-shot worker: reads current reminder-time settings and (re)installs the
 * three daily periodic workers with correctly computed initial delays. */
class ScheduleSetupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val profile = applicationContext.repository().getHunterProfile()
        NotificationScheduler.installPeriodicWork(
            applicationContext,
            profile.morningReminderHour,
            profile.morningReminderMinute,
            profile.eveningReminderHour,
            profile.eveningReminderMinute,
        )
        return Result.success()
    }
}

class MorningNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (FocusSessionState.isActive) return Result.success()
        val repo = applicationContext.repository()
        val profile = repo.getHunterProfile()
        if (!profile.notificationsEnabled) return Result.success()

        val nonNegotiables = repo.observeActiveHabits().first().filter { it.isNonNegotiable }
        if (nonNegotiables.isEmpty()) return Result.success()

        val list = nonNegotiables.joinToString(", ") { it.name }
        NotificationHelper.notify(
            applicationContext,
            NotificationHelper.MORNING_NOTIFICATION_ID,
            "Today's non-negotiables",
            list,
        )
        return Result.success()
    }
}

class EveningNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (FocusSessionState.isActive) return Result.success()
        val repo = applicationContext.repository()
        val profile = repo.getHunterProfile()
        if (!profile.notificationsEnabled) return Result.success()

        val habits = repo.observeActiveHabits().first().filter { it.isNonNegotiable }
        val today = LocalDate.now()
        val logs = repo.observeLogsForDate(today).first()
        val completedIds = logs.filter { it.completed }.map { it.habitId }.toSet()
        val remaining = habits.filter { it.id !in completedIds }

        if (remaining.isEmpty()) return Result.success() // nothing left unchecked — no nudge

        NotificationHelper.notify(
            applicationContext,
            NotificationHelper.EVENING_NOTIFICATION_ID,
            "Still open today",
            remaining.joinToString(", ") { it.name },
        )
        return Result.success()
    }
}

/** Runs just after local midnight for the day that just ended: applies
 * penalties + queues Penalty Quests, and generates the Weekly Review on
 * Mondays for the week that just finished. */
class MidnightRolloverWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = applicationContext.repository()
        val today = LocalDate.now()
        val rolledDate = today.minusDays(1)
        repo.runMidnightRollover(rolledDate)
        // Gates settle on the same boundary as penalties, so a day counts once.
        repo.advanceGateForDay(rolledDate)

        if (today.dayOfWeek == DayOfWeek.MONDAY) {
            repo.generateWeeklyReview(today.minusDays(7))
        }
        return Result.success()
    }
}
