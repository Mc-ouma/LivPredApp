package com.soccertips.predcompose.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import com.soccertips.predcompose.util.RescheduleWorker
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    fun scheduleMatchNotification(item: FavoriteItem) {
        try {
            val notificationTime = calculateNotificationTime(item)

            if (notificationTime > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    scheduleWithAlarmManager(item, notificationTime)
                } else {
                    scheduleWithWorkManager(item, notificationTime)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule notification for match ${item.fixtureId}")
        }
    }

    private fun calculateNotificationTime(item: FavoriteItem): Long {
        val dateTime = LocalDateTime.parse(
            "${item.mDate} ${item.mTime}",
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        )
        return dateTime.minusMinutes(15)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleWithAlarmManager(item: FavoriteItem, notificationTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("fixtureId", item.fixtureId)
                putExtra("notification_type", "match_reminder")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.fixtureId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(notificationTime, pendingIntent),
                pendingIntent
            )
        } else {
            // Fall back to WorkManager if exact alarms are not available
            scheduleWithWorkManager(item, notificationTime)
        }
    }

    private fun scheduleWithWorkManager(item: FavoriteItem, notificationTime: Long) {
        val delay = notificationTime - System.currentTimeMillis()

        val inputData = Data.Builder()
            .putString("fixtureId", item.fixtureId)
            .putString("homeTeam", item.homeTeam)
            .putString("awayTeam", item.awayTeam)
            .putString("mTime", item.mTime)
            .putString("mDate", item.mDate)
            .putString("league", item.league)
            .build()

        val notificationWork = OneTimeWorkRequestBuilder<RescheduleWorker>()
            .setInputData(inputData)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("match_notification_${item.fixtureId}")
            .build()

        WorkManager.getInstance(context)
            .enqueue(notificationWork)
    }

    fun cancelNotification(fixtureId: String) {
        // Cancel AlarmManager notification
        val intent = Intent(context, NotificationReceiver::class.java)
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
    }
}