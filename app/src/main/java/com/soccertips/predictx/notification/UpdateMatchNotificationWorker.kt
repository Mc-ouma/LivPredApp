package com.soccertips.predictx.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soccertips.predictx.data.model.FixtureResponse
import com.soccertips.predictx.network.FixtureDetailsService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class UpdateMatchNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationBuilder: NotificationBuilder
) : CoroutineWorker(context, params) {

    @AssistedFactory
    interface Factory {
        fun create(
            context: Context,
            workerParams: WorkerParameters
        ): UpdateMatchNotificationWorker
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UpdateMatchNotificationWorkerEntryPoint {
        fun fixtureDetailsService(): FixtureDetailsService
    }

    override suspend fun doWork(): Result {
        val fixtureId = inputData.getString("fixtureId") ?: return Result.failure()

        return try {
            // Fetch updated match data from the API
            val entryPoint =
                EntryPoints.get(
                    applicationContext,
                    UpdateMatchNotificationWorkerEntryPoint::class.java
                )
            val apiService = entryPoint.fixtureDetailsService()
            val fixtureResponse: FixtureResponse? = withContext(Dispatchers.IO) {
                try {
                    apiService.getFixtureDetails(fixtureId)
                } catch (e: Exception) {
                    Timber.Forest.e(e)
                    null
                }
            }

            // Update the notification
            if (fixtureResponse != null && fixtureResponse.response.isNotEmpty()) {
                updateNotification(fixtureId, fixtureResponse)
                Result.success()
            } else {
                Timber.Forest.e("Failed to fetch fixture details for fixture $fixtureId")
                Result.failure()
            }


        } catch (e: Exception) {
            Timber.Forest.e(e, "Failed to update notification for fixture $fixtureId")
            return Result.failure()
        }
    }

    private suspend fun updateNotification(fixtureId: String, fixtureResponse: FixtureResponse) {
        val notificationManager = NotificationManagerCompat.from(applicationContext)

        // Customize the notification content based on the updated match details
        val updatedNotification =
            notificationBuilder.buildMatchUpdateNotification(fixtureResponse).build()

        try {
            notificationManager.notify(fixtureId.hashCode(), updatedNotification)
        } catch (e: SecurityException) {
            Timber.Forest.e(e, "Notification permission not granted")
        }
    }
}