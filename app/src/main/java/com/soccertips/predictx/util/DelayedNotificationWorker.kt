package com.soccertips.predictx.util

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soccertips.predictx.data.local.entities.FavoriteItem
import com.soccertips.predictx.notification.NotificationBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class DelayedNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationBuilder: NotificationBuilder // Assuming NotificationBuilder is injectable via Hilt
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val fixtureId = inputData.getString("fixtureId") ?: return Result.failure()

        // Reconstruct FavoriteItem from inputData
        // Ensure all necessary fields used by NotificationBuilder.buildMatchNotification are passed
        val favoriteItem = FavoriteItem(
            fixtureId = fixtureId,
            homeTeam = inputData.getString("homeTeam"),
            awayTeam = inputData.getString("awayTeam"),
            league = inputData.getString("league"),
            mDate = inputData.getString("mDate"),
            mTime = inputData.getString("mTime"),
            mStatus = inputData.getString("mStatus"), // May not be strictly needed for initial reminder
            outcome = inputData.getString("outcome"), // May not be strictly needed for initial reminder
            pick = inputData.getString("pick"),       // May not be strictly needed for initial reminder
            color = inputData.getInt("color", 0),
            hLogoPath = inputData.getString("hLogoPath"),
            aLogoPath = inputData.getString("aLogoPath"),
            leagueLogo = inputData.getString("leagueLogo"),
            completedTimestamp = inputData.getLong(
                "completedTimestamp",
                0L
            ) // May not be strictly needed
        )

        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Assuming buildMatchNotification is suitable for this worker's purpose
            val notification = notificationBuilder.buildMatchNotification(favoriteItem).build()
            notificationManager.notify(favoriteItem.fixtureId.hashCode(), notification)
            Result.success()
        } catch (e: SecurityException) {
            Timber.Forest.e(e, "Notification permission not granted for fixture $fixtureId in DelayedNotificationWorker")
            Result.failure()
        } catch (e: Exception) {
            Timber.Forest.e(e, "Failed to show notification via DelayedNotificationWorker for $fixtureId")
            Result.retry()
        }
    }
}