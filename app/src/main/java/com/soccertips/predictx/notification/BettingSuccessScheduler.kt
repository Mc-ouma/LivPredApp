package com.soccertips.predictx.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class BettingSuccessScheduler
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val bettingSuccessChecker: BettingSuccessChecker
) {

    companion object {
        private const val BETTING_SUCCESS_WORK_NAME = "betting_success_check"
        private const val MANUAL_CHECK_WORK_PREFIX = "manual_betting_check"
    }

    /**
     * Schedule periodic betting success checks This will run twice daily to check for perfect
     * betting days
     */
    fun scheduleBettingSuccessChecks() {
        try {
            val constraints =
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            val bettingCheckWork =
                    PeriodicWorkRequestBuilder<BettingSuccessWorker>(
                                    12,
                                    TimeUnit.HOURS // Check every 12 hours
                            )
                            .setConstraints(constraints)
                            .addTag("betting_success")
                            .build()

            WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                            BETTING_SUCCESS_WORK_NAME,
                            ExistingPeriodicWorkPolicy.UPDATE,
                            bettingCheckWork
                    )

            Timber.d("Scheduled periodic betting success checks")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule betting success checks")
        }
    }

    /**
     * Schedule a one-time check for a specific date Useful for manual triggers or checking specific
     * dates
     */
    fun scheduleManualCheck(date: String, delayMinutes: Long = 0) {
        try {
            val constraints =
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            val manualCheckWork =
                    OneTimeWorkRequestBuilder<BettingSuccessWorker>()
                            .setConstraints(constraints)
                            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                            .addTag("manual_betting_check")
                            .addTag("date_$date")
                            .build()

            WorkManager.getInstance(context).enqueue(manualCheckWork)

            Timber.d(
                    "Scheduled manual betting success check for date: $date with delay: $delayMinutes minutes"
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule manual betting success check for date: $date")
        }
    }

    /** Check betting success immediately for testing purposes */
    suspend fun checkImmediately(date: String? = null) {
        try {
            if (date != null) {
                bettingSuccessChecker.manualCheckForDate(date)
            } else {
                bettingSuccessChecker.checkTodaysBettingSuccess()
                bettingSuccessChecker.checkYesterdaysBettingSuccess()
            }
            Timber.d("Immediate betting success check completed")
        } catch (e: Exception) {
            Timber.e(e, "Failed to perform immediate betting success check")
        }
    }

    /**
     * Schedule end-of-day checks for better accuracy This schedules checks for late evening when
     * most matches should be completed
     */
    fun scheduleEndOfDayCheck() {
        try {
            val now = LocalDateTime.now()
            val endOfDay = now.withHour(23).withMinute(30).withSecond(0).withNano(0)

            // If it's already past 23:30, schedule for tomorrow
            val targetTime =
                    if (now.isAfter(endOfDay)) {
                        endOfDay.plusDays(1)
                    } else {
                        endOfDay
                    }

            val delayMillis =
                    targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                            System.currentTimeMillis()

            if (delayMillis > 0) {
                val constraints =
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

                val endOfDayWork =
                        OneTimeWorkRequestBuilder<BettingSuccessWorker>()
                                .setConstraints(constraints)
                                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                                .addTag("end_of_day_check")
                                .build()

                WorkManager.getInstance(context).enqueue(endOfDayWork)

                Timber.d("Scheduled end-of-day betting success check for: $targetTime")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule end-of-day betting success check")
        }
    }

    /** Cancel all betting success checks */
    fun cancelAllChecks() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(BETTING_SUCCESS_WORK_NAME)

            WorkManager.getInstance(context).cancelAllWorkByTag("betting_success")

            WorkManager.getInstance(context).cancelAllWorkByTag("manual_betting_check")

            WorkManager.getInstance(context).cancelAllWorkByTag("end_of_day_check")

            Timber.d("Cancelled all betting success checks")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel betting success checks")
        }
    }

    /** Initialize betting success checking system Call this during app startup */
    fun initialize() {
        scheduleBettingSuccessChecks()
        scheduleEndOfDayCheck()
    }
}
