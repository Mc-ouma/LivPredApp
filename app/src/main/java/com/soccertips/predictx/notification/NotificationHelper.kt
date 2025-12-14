package com.soccertips.predictx.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.soccertips.predictx.R

object NotificationHelper {
    const val MATCH_REMINDER_CHANNEL_ID = "match_reminders"
    const val MATCH_UPDATES_CHANNEL_ID = "match_updates"
    const val FCM_DEFAULT_CHANNEL_ID = "fcm_default_channel"
    const val BETTING_SUCCESS_CHANNEL_ID = "betting_success_channel"
    const val DAILY_REMINDER_CHANNEL_ID = "daily_reminders"

    fun createNotificationChannels(context: Context) {
        val channels =
            listOf(
                NotificationChannel(
                    MATCH_REMINDER_CHANNEL_ID,
                    "Match Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
                    .apply {
                        description = "Notifications for upcoming matches"
                        enableVibration(true)
                        enableLights(true)
                    },
                NotificationChannel(
                    MATCH_UPDATES_CHANNEL_ID,
                    "Match Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                    .apply { description = "Live match updates and scores" },
                NotificationChannel(
                    FCM_DEFAULT_CHANNEL_ID,
                    "FCM Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                )
                    .apply {
                        description = "Notifications from Firebase Cloud Messaging"
                        enableLights(true)
                        enableVibration(true)
                    },
                NotificationChannel(
                    BETTING_SUCCESS_CHANNEL_ID,
                    "Betting Success Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                )
                    .apply {
                        description = "Notifications for successful betting days"
                        enableLights(true)
                        lightColor = android.graphics.Color.GREEN
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 500, 200, 500)
                    },
                NotificationChannel(
                    DAILY_REMINDER_CHANNEL_ID,
                    context.getString(R.string.daily_reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                    .apply {
                        description = context.getString(R.string.daily_reminder_channel_description)
                        enableLights(true)
                        enableVibration(true)
                    }
            )

        NotificationManagerCompat.from(context).createNotificationChannels(channels)
    }
}
