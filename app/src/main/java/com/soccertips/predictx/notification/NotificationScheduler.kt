package com.soccertips.predictx.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.soccertips.predictx.data.local.entities.FavoriteItem
import com.soccertips.predictx.util.DelayedNotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class NotificationScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    // Testing mode - set to false to disable test notifications
    private val testingMode = false

    fun scheduleMatchNotification(item: FavoriteItem) {
        try {
            val notificationTime = calculateNotificationTime(item)

            if (notificationTime > System.currentTimeMillis()) {
                Timber.d("Scheduling notification for match ${item.fixtureId} at time: ${LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(notificationTime), ZoneId.systemDefault())}")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    scheduleWithAlarmManager(item, notificationTime)
                } else {
                    scheduleWithWorkManager(item, notificationTime)
                }

                // For testing: also schedule a notification after a short delay if in testing mode
                if (testingMode) {
                    val testTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15) // 15 seconds for quick testing
                    Timber.d("TEST MODE: Also scheduling a test notification to appear in 15 seconds")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        scheduleWithAlarmManager(item, testTime)
                    } else {
                        scheduleWithWorkManager(item, testTime)
                    }
                }
            } else {
                Timber.w("Cannot schedule notification for ${item.fixtureId}: notification time is in the past")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule notification for match ${item.fixtureId}")
        }
    }

    private fun calculateNotificationTime(item: FavoriteItem): Long {
        try {
            // Parse the match date and time
            val dateTime = LocalDateTime.parse(
                "${item.mDate} ${item.mTime}",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            )

            // Schedule notification 15 minutes before match
            val notificationTime = dateTime.minusMinutes(15)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            Timber.d("Calculated notification time for match ${item.fixtureId}: ${LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(notificationTime), ZoneId.systemDefault())}")
            return notificationTime

        } catch (e: Exception) {
            Timber.e(e, "Error parsing date/time for match ${item.fixtureId}, using fallback time")
            // Fallback to 15 seconds from now for testing (reduced from 1 minute for quicker testing)
            return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleWithAlarmManager(item: FavoriteItem, notificationTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                // Adding action to comply with Android 15 intent security requirements
                action = "com.soccertips.predictx.ACTION_MATCH_REMINDER"
                putExtra("fixtureId", item.fixtureId)
                putExtra("homeTeam", item.homeTeam)
                putExtra("awayTeam", item.awayTeam)
                putExtra("mDate", item.mDate)
                putExtra("mTime", item.mTime)
                putExtra("hLogoPath", item.hLogoPath)
                putExtra("aLogoPath", item.aLogoPath)
                putExtra("league", item.league)
                putExtra("mStatus", item.mStatus)
                putExtra("outcome", item.outcome)
                putExtra("pick", item.pick)
                putExtra("color", item.color)
                putExtra("leagueLogo", item.leagueLogo)
                putExtra("notification_type", "match_reminder")
            }

            // Create a unique request code based on fixture ID and notification time
            // This ensures that both regular and test notifications can be scheduled
            val requestCode = (item.fixtureId.hashCode() + (notificationTime % 10000)).toInt()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(notificationTime, pendingIntent),
                    pendingIntent
                )
                Timber.d("Successfully scheduled AlarmManager notification for ${item.fixtureId} with requestCode $requestCode")
            } catch (e: Exception) {
                Timber.e(e, "Failed to schedule AlarmManager notification - falling back to WorkManager")
                scheduleWithWorkManager(item, notificationTime)
            }
        } else {
            // Fall back to WorkManager if exact alarms are not available
            Timber.d("Cannot schedule exact alarms, falling back to WorkManager for ${item.fixtureId}")
            scheduleWithWorkManager(item, notificationTime)
        }
    }

    private fun scheduleWithWorkManager(item: FavoriteItem, notificationTime: Long) {
        val delay = notificationTime - System.currentTimeMillis()

        if (delay <= 0) {
            Timber.w("Attempted to schedule notification in the past for ${item.fixtureId}. Skipping.")
            return
        }

        val inputData = Data.Builder()
            .putString("fixtureId", item.fixtureId)
            .putString("homeTeam", item.homeTeam)
            .putString("awayTeam", item.awayTeam)
            .putString("mTime", item.mTime)
            .putString("mDate", item.mDate)
            .putString("league", item.league)
            .putString("mStatus", item.mStatus)
            .putString("outcome", item.outcome)
            .putString("pick", item.pick)
            .putInt("color", item.color)
            .putString("hLogoPath", item.hLogoPath)
            .putString("aLogoPath", item.aLogoPath)
            .putString("leagueLogo", item.leagueLogo)
            .putLong("completedTimestamp", item.completedTimestamp)
            .build()

        // Create a unique tag based on fixture ID and notification time
        // This ensures that both regular and test notifications can be scheduled
        val tag = "match_notification_${item.fixtureId}_${notificationTime % 10000}"

        val notificationWork = OneTimeWorkRequestBuilder<DelayedNotificationWorker>()
            .setInputData(inputData)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(tag)
            .build()

        WorkManager.getInstance(context)
            .enqueue(notificationWork)
        Timber.d("Scheduled notification for ${item.fixtureId} with WorkManager (DelayedNotificationWorker) with delay: $delay ms, tag: $tag")
    }

    fun cancelNotification(fixtureId: String) {
        // Cancel AlarmManager notification
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.soccertips.predictx.ACTION_MATCH_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            fixtureId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        // Cancel WorkManager notification
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("match_notification_$fixtureId")

        Timber.d("Cancelled all notifications for fixture $fixtureId")
    }
}

