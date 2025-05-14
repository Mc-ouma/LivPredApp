package com.soccertips.predictx.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
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
import androidx.core.content.edit
import com.soccertips.predictx.data.local.dao.FavoriteDao
import com.soccertips.predictx.util.WorkManagerWrapper

@HiltWorker
class UpdateMatchNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationBuilder: NotificationBuilder,
    private val workManagerWrapper: WorkManagerWrapper,
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
        fun favoriteDao(): FavoriteDao
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
        val sharedPrefs = applicationContext.getSharedPreferences("match_notifications", Context.MODE_PRIVATE)
        val entryPoint = EntryPoints.get(applicationContext, UpdateMatchNotificationWorkerEntryPoint::class.java)
        val favoriteDao = entryPoint.favoriteDao()

        val fixture = fixtureResponse.response.firstOrNull() ?: return
        val homeGoals = fixture.goals?.home ?: 0
        val awayGoals = fixture.goals?.away ?: 0
        val matchStatus = fixture.fixture.status.short // API's short status code, e.g., "NS", "1H", "FT", "PST"

        val scoreKey = "score_${fixtureId}"
        val lastScore = sharedPrefs.getString(scoreKey, null)
        val currentScore = "$homeGoals-$awayGoals"

        val favoriteItem = withContext(Dispatchers.IO) {
            favoriteDao.getFavoriteItemByFixtureId(fixtureId)
        } ?: return

        // Define status categories
        val finishedStatuses = setOf("FT", "PEN", "AET")
        val inactiveCancelStatuses = setOf("PST", "CANC", "SUSP", "ABD", "AWD", "WO", "INT") // Postponed, Cancelled, Suspended, Abandoned, Technical Loss, WalkOver, Interrupted
        val activeOngoingStatuses = setOf("1H", "2H", "HT") // Add other live statuses like "LIVE" if your API uses them

        // Update FavoriteItem: mStatus, outcome, and completedTimestamp
        val updatedFavoriteItem = favoriteItem.copy(
            mStatus = when (matchStatus) { // Update our internal mStatus representation
                "1H" -> "1st Half"
                "2H" -> "2nd Half"
                "HT" -> "Half Time"
                // For other statuses (finished, inactive, NS, etc.), use the direct short status from the API.
                // This ensures mStatus in DB reflects the latest from API if not specially mapped.
                else -> matchStatus
            },
            outcome = if (matchStatus in finishedStatuses) {
                "$homeGoals - $awayGoals"
            } else {
                favoriteItem.outcome // Keep existing outcome if not finished
            },
            completedTimestamp = if (matchStatus in finishedStatuses && favoriteItem.completedTimestamp == null) {
                System.currentTimeMillis() // Set completion timestamp once when finished
            } else {
                favoriteItem.completedTimestamp // Keep existing or null
            }
        )

        withContext(Dispatchers.IO) {
            favoriteDao.updateFavoriteItem(updatedFavoriteItem)
        }

        val workerName = "update_notification_${fixtureId}"

        // Handle terminal states: finished, or cancelled/postponed etc.
        if (matchStatus in finishedStatuses || matchStatus in inactiveCancelStatuses) {
            workManagerWrapper.cancelUniqueWork(workerName)
            notificationManager.cancel(fixtureId.hashCode()) // Remove notification from tray
            sharedPrefs.edit { remove(scoreKey) } // Clean up stored score for this fixture
            Timber.Forest.d("Worker and notification cancelled for fixture $fixtureId due to terminal status: $matchStatus")
            return // Stop further processing for these states
        }

        // Handle active, ongoing matches for notifications
        if (matchStatus in activeOngoingStatuses) {
            if (lastScore != currentScore) {
                val notificationText = "Score: $homeGoals - $awayGoals"
                val notification = notificationBuilder.buildMatchUpdateNotification(fixtureResponse)
                    .setContentText(notificationText)
                    .build()
                try {
                    notificationManager.notify(fixtureId.hashCode(), notification)
                    sharedPrefs.edit { putString(scoreKey, currentScore) } // Save the new score
                } catch (e: SecurityException) {
                    Timber.Forest.e(e, "Notification permission not granted for $fixtureId")
                }
            } else {
                Timber.Forest.i("Score unchanged for active match $fixtureId ($currentScore). No notification update.")
            }
        } else {
            // For other statuses not explicitly handled for notification (e.g., "NS" - Not Started),
            // do nothing regarding notifications. The worker will continue for future updates.
            // This prevents initial "Score: 0-0" notifications for "NS" matches.
            Timber.Forest.i("Match $fixtureId status '$matchStatus' is not an active ongoing status for notifications, or no score change if it were. No notification action.")
        }
    }
}

