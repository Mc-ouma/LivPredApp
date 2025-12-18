package com.soccertips.predictx.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soccertips.predictx.MainActivity
import com.soccertips.predictx.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker that sends daily localized reminder notifications.
 * The notification content is pulled from string resources, ensuring
 * it displays in the user's preferred app language.
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val MORNING_NOTIFICATION_ID = 9999
        private const val AFTERNOON_NOTIFICATION_ID = 9998
        const val KEY_REMINDER_TYPE = "reminder_type"
        const val TYPE_MORNING = "morning"
        const val TYPE_AFTERNOON = "afternoon"
    }

    override suspend fun doWork(): Result {
        return try {
            val reminderType = inputData.getString(KEY_REMINDER_TYPE) ?: TYPE_MORNING
            showDailyReminderNotification(reminderType)
            Timber.d("Daily reminder notification ($reminderType) sent successfully")
            Result.success()
        } catch (e: SecurityException) {
            Timber.e(e, "Notification permission not granted for daily reminder")
            Result.failure()
        } catch (e: Exception) {
            Timber.e(e, "Failed to show daily reminder notification")
            Result.retry()
        }
    }

    private fun showDailyReminderNotification(reminderType: String) {
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
