package com.soccertips.predictx.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.soccertips.predictx.MainActivity
import com.soccertips.predictx.R
import timber.log.Timber

/**
 * BroadcastReceiver for handling daily reminder alarms.
 *
 * This is triggered by AlarmManager at exact times to ensure
 * notifications are delivered promptly, even when the app is not running.
 */
class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_REMINDER = "com.soccertips.predictx.ACTION_DAILY_REMINDER"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        const val TYPE_MORNING = "morning"
        const val TYPE_AFTERNOON = "afternoon"

        private const val MORNING_NOTIFICATION_ID = 9999
        private const val AFTERNOON_NOTIFICATION_ID = 9998
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DAILY_REMINDER) {
            Timber.w("DailyReminderReceiver: Unexpected action ${intent.action}")
            return
        }

        val reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: TYPE_MORNING
        Timber.d("DailyReminderReceiver: Received $reminderType reminder alarm")

        showDailyReminderNotification(context, reminderType)

        // Reschedule the next alarm for tomorrow
        rescheduleNextAlarm(context, reminderType)
    }

    private fun showDailyReminderNotification(context: Context, reminderType: String) {
        // Check for notification permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        // Intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "com.soccertips.predictx.ACTION_MAIN"
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get localized strings based on reminder type
        val (title, body, notificationId) = when (reminderType) {
            TYPE_AFTERNOON -> Triple(
                context.getString(R.string.afternoon_reminder_title),
                context.getString(R.string.afternoon_reminder_body),
                AFTERNOON_NOTIFICATION_ID
            )

            else -> Triple(
                context.getString(R.string.daily_reminder_title),
                context.getString(R.string.daily_reminder_body),
                MORNING_NOTIFICATION_ID
            )
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.DAILY_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Higher priority to wake screen
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Timber.d("Daily reminder notification ($reminderType) shown successfully")
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to show notification - permission denied")
        }
    }

    private fun rescheduleNextAlarm(context: Context, reminderType: String) {
        try {
            val scheduler = DailyReminderAlarmScheduler(context)
            when (reminderType) {
                TYPE_MORNING -> scheduler.scheduleMorningReminder()
                TYPE_AFTERNOON -> scheduler.scheduleAfternoonReminder()
            }
            Timber.d("Rescheduled next $reminderType alarm")
        } catch (e: Exception) {
            Timber.e(e, "Failed to reschedule $reminderType alarm")
        }
    }
}
