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
import com.soccertips.predictx.data.model.Fixture
import com.soccertips.predictx.data.model.Goals
import com.soccertips.predictx.data.model.League
import com.soccertips.predictx.data.model.Paging
import com.soccertips.predictx.data.model.Parameters
import com.soccertips.predictx.data.model.Periods
import com.soccertips.predictx.data.model.ResponseData
import com.soccertips.predictx.data.model.Score
import com.soccertips.predictx.data.model.Status
import com.soccertips.predictx.data.model.Teams
import com.soccertips.predictx.data.model.Venue

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

    // Match status categories
    companion object {
        // Terminal statuses - match is over or canceled
        private val TERMINAL_STATUSES = setOf(
            "FT", "PEN", "AET", // Finished statuses
            "PST", "CANC", "SUSP", "ABD", "AWD", "WO", "INT" // Inactive/canceled statuses
        )

        // Active match statuses
        private val ACTIVE_STATUSES = setOf("1H", "2H", "HT", "ET", "BT", "P", "LIVE")

        // Pre-match status
        private const val NOT_STARTED = "NS"

        // Shared preferences keys
        private const val PREFS_NAME = "match_notifications"
        private const val SCORE_PREFIX = "score_"
        private const val STATUS_PREFIX = "status_"
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
                     Timber.e(e)
                     null
                 }
             }

            // FOR TESTING: Use a simulated response
           /* val simulatedFixtureResponse = createSimulatedFixtureResponse(
                fixtureId = fixtureId,
                statusShort = "HT", // Example: Test Half-Time
                homeGoals = 1,
                awayGoals = 1,
                homeTeamName = "Home Team Test",
                awayTeamName = "Away Team Test",
                leagueName = "Test League"
            )
            val fixtureResponse: FixtureResponse? = simulatedFixtureResponse*/
            // END TESTING BLOCK

            // Update the notification
            if (fixtureResponse != null && fixtureResponse.response.isNotEmpty()) {
                updateNotification(fixtureId, fixtureResponse)
                Result.success()
            } else {
                Timber.e("Failed to fetch fixture details for fixture $fixtureId")
                Result.retry() // Retry instead of immediate failure
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to update notification for fixture $fixtureId")
            return Result.retry() // Retry on exception
        }
    }

    private suspend fun updateNotification(fixtureId: String, fixtureResponse: FixtureResponse) {
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val sharedPrefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val entryPoint = EntryPoints.get(applicationContext, UpdateMatchNotificationWorkerEntryPoint::class.java)
        val favoriteDao = entryPoint.favoriteDao()

        val fixture = fixtureResponse.response.firstOrNull() ?: return
        val homeGoals = fixture.goals?.home ?: 0
        val awayGoals = fixture.goals?.away ?: 0
        val matchStatus = fixture.fixture.status.short // API's short status code

        // Store keys for tracking changes
        val scoreKey = "${SCORE_PREFIX}${fixtureId}"
        val statusKey = "${STATUS_PREFIX}${fixtureId}"

        val lastScore = sharedPrefs.getString(scoreKey, "0-0")
        val lastStatus = sharedPrefs.getString(statusKey, NOT_STARTED)
        val currentScore = "$homeGoals-$awayGoals"

        val favoriteItem = withContext(Dispatchers.IO) {
            favoriteDao.getFavoriteItemByFixtureId(fixtureId)
        }

        // Update FavoriteItem with latest match status and score
        val updatedFavoriteItem = favoriteItem.copy(
            mStatus = when (matchStatus) {
                "1H" -> "1st Half"
                "2H" -> "2nd Half"
                "HT" -> "Half Time"
                "ET" -> "Extra Time"
                "FT" -> "Match Finished"
                "PST" -> "Postponed"
                "CANC" -> "Cancelled"
                // For other statuses, use the direct status from API
                else -> matchStatus
            },
            outcome = if (matchStatus in TERMINAL_STATUSES) {
                "$homeGoals - $awayGoals"
            } else {
                favoriteItem.outcome // Keep existing outcome if not in terminal status
            },
            completedTimestamp = if (matchStatus in TERMINAL_STATUSES && favoriteItem.completedTimestamp == 0L) {
                System.currentTimeMillis() // Set completion timestamp once when finished
            } else {
                favoriteItem.completedTimestamp // Keep existing timestamp
            }
        )

        withContext(Dispatchers.IO) {
            favoriteDao.updateFavoriteItem(updatedFavoriteItem)
        }

        val workerName = "update_notification_${fixtureId}"

        // Handle terminal states - match is finished or cancelled/postponed
        if (matchStatus in TERMINAL_STATUSES) {
            // If the match just ended (status changed to terminal), show a final notification
            if (lastStatus !in TERMINAL_STATUSES) {
                val finalNotification = notificationBuilder.buildMatchUpdateNotification(fixtureResponse)
                    .setContentText("Final Score: $homeGoals - $awayGoals")
                    .build()

                try {
                    notificationManager.notify(fixtureId.hashCode(), finalNotification)
                } catch (e: SecurityException) {
                    Timber.e(e, "Notification permission not granted for final update of $fixtureId")
                }
            }

            // Cancel periodic updates and clean up
            workManagerWrapper.cancelUniqueWork(workerName)
            sharedPrefs.edit {
                remove(scoreKey)
                remove(statusKey)
            }

            // Let the notification stay for a short time before removing it
            // (FavoriteCleanupWorker will eventually clean up the match from database)
            return
        }

        // Handle active, ongoing matches for notifications
        if (matchStatus in ACTIVE_STATUSES) {
            val statusChanged = lastStatus != matchStatus
            val scoreChanged = lastScore != currentScore

            // Show notification if score changed or status changed to a significant state
            if (scoreChanged || statusChanged) {
                val notification = notificationBuilder.buildMatchUpdateNotification(fixtureResponse).build()

                try {
                    notificationManager.notify(fixtureId.hashCode(), notification)

                    // Show summary notification if there are multiple active matches
                    val activeFavorites = withContext(Dispatchers.IO) {
                        favoriteDao.getAllFavorites().filter {
                            it.mStatus in ACTIVE_STATUSES.map { status ->
                                when(status) {
                                    "1H" -> "1st Half"
                                    "2H" -> "2nd Half"
                                    "HT" -> "Half Time"
                                    else -> status
                                }
                            }
                        }
                    }

                    if (activeFavorites.size > 1) {
                        val summaryNotification = notificationBuilder
                            .buildSummaryNotification(
                                activeFavorites.size,
                                "com.soccertips.predictx.MATCH_UPDATES"
                            )
                            .build()
                        notificationManager.notify(0, summaryNotification)
                    }

                    // Save the new state
                    sharedPrefs.edit {
                        putString(scoreKey, currentScore)
                        putString(statusKey, matchStatus)
                    }
                } catch (e: SecurityException) {
                    Timber.e(e, "Notification permission not granted for $fixtureId")
                }
            } else {
                Timber.i("No significant changes for match $fixtureId (status=$matchStatus, score=$currentScore)")
            }
        } else if (matchStatus == NOT_STARTED) {
            // Match hasn't started yet, just update status in DB
            sharedPrefs.edit { putString(statusKey, matchStatus) }
            Timber.i("Match $fixtureId hasn't started yet. Continuing monitoring.")
        } else {
            // For any other unhandled statuses, log and continue monitoring
            Timber.i("Match $fixtureId has status '$matchStatus' which isn't explicitly handled for notifications.")
        }
    }

    // Function to create a simulated FixtureResponse for testing
    private fun createSimulatedFixtureResponse(
        fixtureId: String,
        statusShort: String,
        homeGoals: Int,
        awayGoals: Int,
        homeTeamName: String,
        awayTeamName: String,
        leagueName: String,
        elapsed: Int? = null // Optional: for statuses like 1H, 2H
    ): FixtureResponse {
        return FixtureResponse(
            response = listOf(
                ResponseData(
                    fixture = Fixture(
                        id = fixtureId.toIntOrNull() ?: 0,
                        status = Status(short = statusShort, long = "Status: $statusShort", elapsed = 3, extra = null),
                        date = "2025-05-20T18:00:00+00:00", // Example date
                        referee = "Test Referee",
                        timezone = "UTC",
                        timestamp = System.currentTimeMillis() / 1000,
                        periods = Periods(
                            first = System.currentTimeMillis() / 1000,
                            second = System.currentTimeMillis() / 1000 + 3600 // Example period
                        ),
                        venue = Venue(
                            id = 1,
                            name = "Test Stadium",
                            city = "Test City"
                        )
                    ),
                    league = League(
                        id = 1,
                        name = leagueName,
                        country = "TestCountry",
                        logo = "test_league_logo.png",
                        flag = "test_league_flag.png",
                        season = 2025,
                        round = "Test Round"
                    ),
                    teams = Teams(
                        home = com.soccertips.predictx.data.model.Team(id = 10, name = homeTeamName, logo = "home_logo.png", winner = true),
                        away = com.soccertips.predictx.data.model.Team(id = 11, name = awayTeamName, logo = "away_logo.png", winner = false)
                    ),
                    goals = Goals(home = homeGoals, away = awayGoals),
                    score = Score(
                        halftime = com.soccertips.predictx.data.model.HalfTime(home = homeGoals, away = awayGoals),
                        fulltime = com.soccertips.predictx.data.model.FullTime(home = homeGoals, away = awayGoals),
                        extratime = null,
                        penalty = null,
                    ),
                    events = emptyList(), // Add if needed
                    lineups = emptyList(),
                    statistics = emptyList(), // Add if needed
                    players = emptyList() // Add if needed

                )
            ),
            results = 1,
            paging = Paging(
                current = 1,
                total = 1
            ), // Add if needed
            errors = emptyList(), // Add if needed
            get = "",
            parameters = Parameters(
                id = fixtureId,
            ) // Add if needed
        )
    }
}
