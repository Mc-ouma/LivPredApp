package com.soccertips.predcompose.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    const val MATCH_REMINDER_CHANNEL_ID = "match_reminders"
    const val MATCH_UPDATES_CHANNEL_ID = "match_updates"

    fun createNotificationChannels(context: Context) {
        val channels = listOf(
            NotificationChannel(
                MATCH_REMINDER_CHANNEL_ID,
                "Match Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming matches"
                enableVibration(true)
                enableLights(true)
            },
            NotificationChannel(
                MATCH_UPDATES_CHANNEL_ID,
                "Match Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Live match updates and scores"
            }
        )

        NotificationManagerCompat.from(context).createNotificationChannels(channels)
    }
}