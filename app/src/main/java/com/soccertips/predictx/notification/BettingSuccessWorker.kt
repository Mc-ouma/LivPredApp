package com.soccertips.predictx.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class BettingSuccessWorker
@AssistedInject
constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val bettingSuccessChecker: BettingSuccessChecker
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory {
        fun create(context: Context, workerParameters: WorkerParameters): BettingSuccessWorker
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting betting success check worker")

            // Check yesterday's results (matches should be completed by now)
            bettingSuccessChecker.checkYesterdaysBettingSuccess()

            // Also check today's results in case there are early matches
            bettingSuccessChecker.checkTodaysBettingSuccess()

            Timber.d("Betting success check completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error in betting success check worker")
            Result.retry()
        }
    }
}
