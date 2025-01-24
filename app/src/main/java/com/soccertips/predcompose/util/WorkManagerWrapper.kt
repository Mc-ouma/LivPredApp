package com.soccertips.predcompose.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.soccertips.predcompose.NotificationReceiver
import com.soccertips.predcompose.data.local.AppDatabase
import com.soccertips.predcompose.data.local.entities.FavoriteItem
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

class RescheduleWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tasks = AppDatabase.getDatabase(applicationContext).favoriteDao().getDueItem(
            mTime = System.currentTimeMillis()
                .toString(), mDate = System.currentTimeMillis().toString()
        )
        val currentTime = System.currentTimeMillis()

        tasks.forEach { task ->
            val triggerTime = task.mTime?.toLong()?.minus((15 * 60 * 1000))
            if (triggerTime != null) {
                if (triggerTime > currentTime) { // Only future alarms
                    scheduleNotification(task, triggerTime, applicationContext)
                }
            }
        }
        return Result.success()
    }

    private fun scheduleNotification(
        task: FavoriteItem,
        triggerTime: Long,
        context: Context
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("fixtureId", task.fixtureId)
                putExtra("currentTime", task.mTime)
                putExtra("currentDate", task.mDate)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.fixtureId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Timber.e("Cannot Schedule exact alarm: ${e.message}")
        }
    }
    // Same implementation as in ViewModel
}

