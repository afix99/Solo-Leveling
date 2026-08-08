package com.ascend.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms the daily notification + midnight-rollover work after a device reboot,
 * since WorkManager's periodic jobs don't survive a reboot on their own. */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationScheduler.scheduleAll(context)
        }
    }
}
