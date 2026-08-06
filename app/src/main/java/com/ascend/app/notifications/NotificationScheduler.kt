package com.ascend.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules ASCEND's 3 daily local jobs: the morning list, the evening nudge,
 * and the just-after-midnight rollover (penalties + streak + weekly review).
 * All local, no network. See design spec §7 and §4.
 */
object NotificationScheduler {
    const val MORNING_WORK = "ascend_morning_notification"
    const val EVENING_WORK = "ascend_evening_notification"
    const val ROLLOVER_WORK = "ascend_midnight_rollover"

    /** Fixed, not user-configurable — a few minutes after midnight is early
     * enough that "today" hasn't really started yet for anyone. */
    private const val ROLLOVER_HOUR = 0
    private const val ROLLOVER_MINUTE = 5

    /** Re-reads current settings from the DB and (re)installs all periodic work.
     * Safe to call repeatedly (e.g. after Settings changes, or on boot). */
    fun scheduleAll(context: Context) {
        NotificationHelper.ensureChannel(context)
        val request = OneTimeWorkRequestBuilder<ScheduleSetupWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    internal fun installPeriodicWork(
        context: Context,
        morningHour: Int,
        morningMinute: Int,
        eveningHour: Int,
        eveningMinute: Int,
    ) {
        val wm = WorkManager.getInstance(context)

        wm.enqueueUniquePeriodicWork(
            MORNING_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MorningNotificationWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(computeInitialDelayMillis(morningHour, morningMinute), TimeUnit.MILLISECONDS)
                .build(),
        )

        wm.enqueueUniquePeriodicWork(
            EVENING_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<EveningNotificationWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(computeInitialDelayMillis(eveningHour, eveningMinute), TimeUnit.MILLISECONDS)
                .build(),
        )

        wm.enqueueUniquePeriodicWork(
            ROLLOVER_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MidnightRolloverWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(computeInitialDelayMillis(ROLLOVER_HOUR, ROLLOVER_MINUTE), TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    /** Millis from now until the next occurrence of [hour]:[minute] local time. */
    fun computeInitialDelayMillis(hour: Int, minute: Int, now: LocalDateTime = LocalDateTime.now()): Long {
        var target = now.toLocalDate().atTime(hour, minute)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis()
    }
}
