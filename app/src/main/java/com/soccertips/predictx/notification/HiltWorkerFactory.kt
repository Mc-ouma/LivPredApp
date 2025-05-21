package com.soccertips.predictx.notification

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.soccertips.predictx.util.DelayedNotificationWorker
import com.soccertips.predictx.util.FavoriteCleanupWorker
import com.soccertips.predictx.util.RescheduleWorker
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import javax.inject.Provider

@ActivityRetainedScoped
class HiltWorkerFactory @Inject constructor(
    private val updateMatchNotificationWorkerFactory: Provider<UpdateMatchNotificationWorker.Factory>,
    private val notificationBuilder: Provider<NotificationBuilder>,
    private val favoriteDao: Provider<com.soccertips.predictx.data.local.dao.FavoriteDao>,
    private val rescheduleWorkerFactory: Provider<RescheduleWorker.Factory> // Added RescheduleWorker Factory
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            UpdateMatchNotificationWorker::class.java.name -> {
                updateMatchNotificationWorkerFactory.get().create(appContext, workerParameters)
            }
            DelayedNotificationWorker::class.java.name -> {
                DelayedNotificationWorker(
                    appContext,
                    workerParameters,
                    notificationBuilder.get()
                )
            }
            FavoriteCleanupWorker::class.java.name -> {
                FavoriteCleanupWorker(
                    appContext,
                    workerParameters,
                    favoriteDao.get()
                )
            }
            RescheduleWorker::class.java.name -> {
                // Use the factory to create RescheduleWorker
                rescheduleWorkerFactory.get().create(appContext, workerParameters)
            }
            else -> null
        }
    }
}

