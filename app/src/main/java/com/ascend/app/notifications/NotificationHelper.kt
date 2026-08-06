package com.ascend.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ascend.app.MainActivity
import com.ascend.app.R

object NotificationHelper {
    const val CHANNEL_ID = "ascend_reminders"
    const val MORNING_NOTIFICATION_ID = 1001
    const val EVENING_NOTIFICATION_ID = 1002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Morning non-negotiables list and evening nudge."
        }
        manager?.createNotificationChannel(channel)
    }

    fun notify(context: Context, id: Int, title: String, text: String) {
        ensureChannel(context)
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            id,
            openAppIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
        // SecurityException is possible if POST_NOTIFICATIONS was never granted —
        // fully offline app, so simply skip the notification rather than crash.
    }
}
