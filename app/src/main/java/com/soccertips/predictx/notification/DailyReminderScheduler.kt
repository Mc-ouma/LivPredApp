package com.soccertips.predictx.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Schedules daily reminder notifications using WorkManager.
 * Notifications are localized based on the user's app language preference.
 * 
 * Uses KEEP policy to avoid resetting schedules on app restart.
 * WorkManager persists the work across app restarts automatically.
 */
@Singleton
class DailyReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MORNING_REMINDER_WORK_NAME = "daily_reminder_morning"
        private const val AFTERNOON_REMINDER_WORK_NAME = "daily_reminder_afternoon"
        private const val MORNING_HOUR = 9   // 9:00 AM
        private const val MORNING_MINUTE = 0
        private const val AFTERNOON_HOUR = 15  // 3:30 PM
        private const val AFTERNOON_MINUTE = 30
    }

    /**
     * Schedule both morning (9 AM) and afternoon (3:30 PM) daily reminder notifications.
     * Uses KEEP policy - if work already exists, it won't be replaced (prevents reset on restart).
     */
    fun scheduleDailyReminder() {
        scheduleMorningReminder()
        scheduleAfternoonReminder()
    }

    /**
     * Schedule morning reminder at 9:00 AM
     */
    private fun scheduleMorningReminder() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val initialDelay = calculateInitialDelay(MORNING_HOUR, MORNING_MINUTE)

            val inputData = Data.Builder()
                .putString(DailyReminderWorker.KEY_REMINDER_TYPE, DailyReminderWorker.TYPE_MORNING)
                .build()

            val morningReminderWork = PeriodicWorkRequestBuilder<DailyReminderWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("daily_reminder")
                .addTag("morning_reminder")
                .build()

            // KEEP policy: only schedule if not already scheduled (persists across restarts)
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    MORNING_REMINDER_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    morningReminderWork
                )

            Timber.d("Morning reminder scheduled/kept at $MORNING_HOUR:$MORNING_MINUTE")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule morning reminder")
        }
    }

    /**
     * Schedule afternoon reminder at 3:30 PM
     */
    private fun scheduleAfternoonReminder() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val initialDelay = calculateInitialDelay(AFTERNOON_HOUR, AFTERNOON_MINUTE)

            val inputData = Data.Builder()
                .putString(DailyReminderWorker.KEY_REMINDER_TYPE, DailyReminderWorker.TYPE_AFTERNOON)
                .build()

            val afternoonReminderWork = PeriodicWorkRequestBuilder<DailyReminderWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("daily_reminder")
                .addTag("afternoon_reminder")
                .build()

            // KEEP policy: only schedule if not already scheduled (persists across restarts)
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    AFTERNOON_REMINDER_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    afternoonReminderWork
                )

            Timber.d("Afternoon reminder scheduled/kept at $AFTERNOON_HOUR:$AFTERNOON_MINUTE")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule afternoon reminder")
        }
    }

    /**
     * Schedule daily reminder at a custom time.
     * @param hour Hour of day (0-23)
     * @param minute Minute (0-59)
     * @param reminderType Type of reminder (morning or afternoon)
     */
    fun scheduleDailyReminderAt(hour: Int, minute: Int, reminderType: String = DailyReminderWorker.TYPE_MORNING) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val initialDelay = calculateInitialDelay(hour, minute)

            val inputData = Data.Builder()
                .putString(DailyReminderWorker.KEY_REMINDER_TYPE, reminderType)
                .build()

            val workName = if (reminderType == DailyReminderWorker.TYPE_AFTERNOON)
                AFTERNOON_REMINDER_WORK_NAME else MORNING_REMINDER_WORK_NAME

            val dailyReminderWork = PeriodicWorkRequestBuilder<DailyReminderWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("daily_reminder")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    workName,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    dailyReminderWork
                )

            Timber.d("Scheduled $reminderType reminder at $hour:$minute, initial delay: ${initialDelay / 1000 / 60} minutes")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule daily reminder")
        }
    }

    /**
     * Cancel all scheduled daily reminders.
     */
    fun cancelDailyReminder() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(MORNING_REMINDER_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(AFTERNOON_REMINDER_WORK_NAME)
            Timber.d("All daily reminders cancelled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel daily reminders")
        }
    }

    /**
     * Calculate the delay until the next occurrence of the specified time.
     */
    private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the target time has already passed today, schedule for tomorrow
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
