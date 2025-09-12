package com.soccertips.predictx.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soccertips.predictx.data.local.dao.FavoriteDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import timber.log.Timber

@HiltWorker
class RescheduleWorker
@AssistedInject
constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val favoriteDao: FavoriteDao,
        private val notificationScheduler: NotificationScheduler
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory {
        fun create(context: Context, workerParameters: WorkerParameters): RescheduleWorker
    }

    override suspend fun doWork(): Result {
        return try {
            val now = LocalDateTime.now()
            val currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            val tasks = favoriteDao.getDueItem(mTime = currentTime, mDate = currentDate)

            tasks.forEach { favoriteItem ->
                try {
                    notificationScheduler.scheduleMatchNotification(favoriteItem)
                } catch (e: Exception) {
                    Timber.e(
                            e,
                            "Failed to reschedule notification for match ${favoriteItem.fixtureId}"
                    )
                }
            }

            Timber.d("Successfully rescheduled ${tasks.size} notifications")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to reschedule notifications")
            Result.failure()
        }
    }
}
