package com.soccertips.predictx.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules daily reminder notifications using AlarmManager for exact timing.
 *
 * Unlike WorkManager, AlarmManager with setExactAndAllowWhileIdle() ensures
 * notifications are delivered at the exact scheduled time, even when the
 * device is in Doze mode.
 *
 * Important: On Android 12+, the user must grant the SCHEDULE_EXACT_ALARM permission.
 */
@Singleton
class DailyReminderAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MORNING_REQUEST_CODE = 1001
        private const val AFTERNOON_REQUEST_CODE = 1002
        private const val MORNING_HOUR = 9    // 9:00 AM
        private const val MORNING_MINUTE = 0
        private const val AFTERNOON_HOUR = 15 // 3:30 PM
        private const val AFTERNOON_MINUTE = 30
    }

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    /**
     * Check if exact alarms are allowed (Android 12+)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Get the intent to open exact alarm settings (Android 12+)
     */
    fun getExactAlarmSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            null
        }
    }

    /**
     * Schedule both morning and afternoon reminders
     */
    fun scheduleDailyReminders() {
        scheduleMorningReminder()
        scheduleAfternoonReminder()
    }

    /**
     * Schedule the morning reminder at 9:00 AM
     */
    fun scheduleMorningReminder() {
        scheduleReminder(
            hour = MORNING_HOUR,
            minute = MORNING_MINUTE,
            requestCode = MORNING_REQUEST_CODE,
            reminderType = DailyReminderReceiver.TYPE_MORNING
        )
    }

    /**
     * Schedule the afternoon reminder at 3:30 PM
     */
    fun scheduleAfternoonReminder() {
        scheduleReminder(
            hour = AFTERNOON_HOUR,
            minute = AFTERNOON_MINUTE,
            requestCode = AFTERNOON_REQUEST_CODE,
            reminderType = DailyReminderReceiver.TYPE_AFTERNOON
        )
    }

    /**
     * Schedule a reminder at the specified time
     */
    private fun scheduleReminder(
        hour: Int,
        minute: Int,
        requestCode: Int,
        reminderType: String
    ) {
        if (!canScheduleExactAlarms()) {
            Timber.w("Cannot schedule exact alarms - permission not granted")
            return
        }

        val triggerTime = calculateNextTriggerTime(hour, minute)
        val pendingIntent = createPendingIntent(requestCode, reminderType)

        try {
            // Use setExactAndAllowWhileIdle to ensure delivery even in Doze mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
            Timber.d("Scheduled $reminderType reminder for ${calendar.time}")
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException scheduling $reminderType alarm - permission likely revoked")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule $reminderType alarm")
        }
    }

    /**
     * Cancel all scheduled reminders
     */
    fun cancelAllReminders() {
        cancelReminder(MORNING_REQUEST_CODE, DailyReminderReceiver.TYPE_MORNING)
        cancelReminder(AFTERNOON_REQUEST_CODE, DailyReminderReceiver.TYPE_AFTERNOON)
        Timber.d("All daily reminders cancelled")
    }

    /**
     * Cancel a specific reminder
     */
    private fun cancelReminder(requestCode: Int, reminderType: String) {
        val pendingIntent = createPendingIntent(requestCode, reminderType)
        alarmManager.cancel(pendingIntent)
    }

    private fun createPendingIntent(requestCode: Int, reminderType: String): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_DAILY_REMINDER
            putExtra(DailyReminderReceiver.EXTRA_REMINDER_TYPE, reminderType)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Calculate the next trigger time for the given hour and minute.
     * If the time has already passed today, it will be scheduled for tomorrow.
     */
    private fun calculateNextTriggerTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the target time has already passed today, schedule for tomorrow
        if (target.before(now) || target == now) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis
    }
}
