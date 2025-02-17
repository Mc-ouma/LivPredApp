package com.soccertips.predcompose.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.soccertips.predcompose.data.local.AppDatabase
import com.soccertips.predcompose.notification.NotificationScheduler
import kotlinx.coroutines.guava.await
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.forEach

interface WorkManagerWrapper {
    fun enqueueUniquePeriodicWork(uniqueWorkName: String, workRequest: PeriodicWorkRequest)
    fun enqueueUniqueOneTimeWork(uniqueWorkName: String, workRequest: OneTimeWorkRequest)
    fun cancelUniqueWork(uniqueWorkName: String)
    suspend fun getWorkInfosForUniqueWork(uniqueWorkName: String): List<WorkInfo>
}

class WorkManagerWrapperImpl @Inject constructor(private val workManager: WorkManager) :
    WorkManagerWrapper {
    override fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        workRequest: PeriodicWorkRequest
    ) {
        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun enqueueUniqueOneTimeWork(
        uniqueWorkName: String,
        workRequest: OneTimeWorkRequest
    ) {
        workManager.enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun cancelUniqueWork(uniqueWorkName: String) {
        workManager.cancelUniqueWork(uniqueWorkName)
    }

    override suspend fun getWorkInfosForUniqueWork(uniqueWorkName: String): List<WorkInfo> {
        return workManager.getWorkInfosForUniqueWork(uniqueWorkName).await()
    }
}

class RescheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationScheduler = NotificationScheduler(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            val tasks = AppDatabase.getDatabase(applicationContext)
                .favoriteDao()
                .getDueItem(
                    mTime = System.currentTimeMillis().toString(),
                    mDate = System.currentTimeMillis().toString()
                )

            tasks.forEach { favoriteItem ->
                try {
                    notificationScheduler.scheduleMatchNotification(favoriteItem)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to reschedule notification for match ${favoriteItem.fixtureId}")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to reschedule notifications")
            Result.failure()
        }
    }
}

