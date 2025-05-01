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

    // Check if the fixture status is valid for displaying notifications
    val fixture = fixtureResponse.response.firstOrNull() ?: return
    val homeGoals = fixture.goals?.home ?: 0
    val awayGoals = fixture.goals?.away ?: 0
    val matchStatus = fixture.fixture.status.short

    // Create score key to track this fixture's last score
    val scoreKey = "score_${fixtureId}"
    val lastScore = sharedPrefs.getString(scoreKey, null)
    val currentScore = "$homeGoals-$awayGoals"

    // Fetch current favorite item from the database
    val favoriteItem = withContext(Dispatchers.IO) {
        favoriteDao.getFavoriteItemByFixtureId(fixtureId)
    } ?: return

    // Update match status and outcome based on fixture status
    val updatedFavoriteItem = when (matchStatus) {
        "FT", "PEN", "AET" -> {

            // For finished matches, set score as outcome and update status
            val scoreOutcome = "$homeGoals - $awayGoals"
            // Also determine match result for easier filtering/display
            val matchResult = when {
                homeGoals > awayGoals -> "HOME_WIN"
                awayGoals > homeGoals -> "AWAY_WIN"
                else -> "DRAW"
            }

            favoriteItem.copy(
                mStatus = matchStatus,
                outcome = scoreOutcome,
                completedTimestamp = System.currentTimeMillis()
            )
        }
        "1H", "2H", "HT" -> {
            favoriteItem.copy(
                mStatus = when (matchStatus) {
                    "1H" -> "1st Half"
                    "2H" -> "2nd Half"
                    "HT" -> "Half Time"
                    else -> matchStatus
                }
            )
        }
        else -> favoriteItem
    }

    // Update the item in database
    withContext(Dispatchers.IO) {
        favoriteDao.updateFavoriteItem(updatedFavoriteItem)
    }

    // Handle match completion - CRITICAL: Cancel worker outside string assignment
    if (matchStatus == "FT") {
        // Cancel the periodic worker when match is finished
        val workerName = "update_notification_${fixtureId}"
        workManagerWrapper.cancelUniqueWork(workerName)
        Timber.Forest.d("Cancelled update notifications for finished match: $fixtureId")
    }

    // Only show notification if score has changed or match has finished
    if (lastScore != currentScore || matchStatus == "FT") {
        val notificationText = if (matchStatus == "FT") {
            "Final Score: ${fixture.goals?.home} - ${fixture.goals?.away}"
        } else {
            "Score: ${fixture.goals?.home} - ${fixture.goals?.away}"
        }

        val updatedNotification = notificationBuilder.buildMatchUpdateNotification(fixtureResponse)
            .setContentText(notificationText)
            .build()

        try {
            notificationManager.notify(fixtureId.hashCode(), updatedNotification)
            // Save the new score
            sharedPrefs.edit { putString(scoreKey, currentScore) }
        } catch (e: SecurityException) {
            Timber.Forest.e(e, "Notification permission not granted")
        }
    } else {
        Timber.Forest.i("Skipping notification for $fixtureId - score unchanged: $currentScore")
    }
}}

