package com.soccertips.predcompose.util

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.guava.await
import javax.inject.Inject

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