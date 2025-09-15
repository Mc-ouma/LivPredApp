package com.soccertips.predictx.notification

import android.content.Context
import com.soccertips.predictx.R
import com.soccertips.predictx.data.local.dao.FavoriteDao
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.repository.FirebaseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Real-time monitor for match result updates that triggers immediate betting success notifications
 * when all matches for a date are completed with results
 */
@Singleton
class RealTimeResultMonitor
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val favoriteDao: FavoriteDao,
        private val firebaseRepository: FirebaseRepository,
        private val bettingSuccessChecker: BettingSuccessChecker
) {

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Flow to emit date completion events
    private val _dateCompletionEvents = MutableSharedFlow<DateCompletionEvent>()
    val dateCompletionEvents: Flow<DateCompletionEvent> = _dateCompletionEvents.asSharedFlow()

    // Track dates being monitored to avoid duplicate checks
    private val monitoredDates = mutableSetOf<String>()
    private val completedDates = mutableSetOf<String>()

    data class DateCompletionEvent(
            val date: String,
            val totalMatches: Int,
            val completedMatches: Int
    )

    /** Start monitoring all active favorites for result updates */
    fun startMonitoring() {
        monitorScope.launch {
            try {
                Timber.d("Starting real-time result monitoring")

                // Get today's and yesterday's favorites and monitor them
                val today = dateFormat.format(Date())
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time
                val yesterdayStr = dateFormat.format(yesterday)

                listOf(today, yesterdayStr).forEach { date ->
                    if (date !in monitoredDates) {
                        monitoredDates.add(date)
                        monitorDateMatches(date)
                    }
                }

                Timber.d("Monitoring matches for dates: $today, $yesterdayStr")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start real-time monitoring")
            }
        }
    }

    /** Monitor matches for a specific date */
    private fun monitorDateMatches(date: String) {
        monitorScope.launch {
            try {
                // Continuously monitor matches for this date
                while (date !in completedDates) {
                    val dateMatches = favoriteDao.getFavoritesByDate(date)

                    val matchesWithResults =
                            dateMatches.filter {
                                !it.outcome.isNullOrBlank() && it.outcome != "0 - 0"
                            }

                    val totalMatches = dateMatches.size
                    val completedMatches = matchesWithResults.size

                    Timber.d("Date $date: $completedMatches/$totalMatches matches completed")

                    // Check if all matches for this date are completed
                    if (completedMatches == totalMatches && totalMatches > 0) {
                        completedDates.add(date)

                        // Emit completion event
                        _dateCompletionEvents.emit(
                                DateCompletionEvent(
                                        date = date,
                                        totalMatches = totalMatches,
                                        completedMatches = completedMatches
                                )
                        )

                        // Trigger immediate betting success check for all categories
                        checkAllCategoriesBettingSuccess(date)

                        Timber.i(
                                "All matches completed for date $date - triggered immediate betting success check"
                        )
                        break
                    }

                    // Wait before next check (every 30 seconds for active monitoring)
                    kotlinx.coroutines.delay(30_000)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to monitor matches for date $date")
            }
        }
    }

    /** Check betting success for all categories on a specific date */
    private suspend fun checkAllCategoriesBettingSuccess(date: String) {
        try {
            Timber.d("Checking betting success for all categories on $date")

            // Get all available categories from Firebase
            val categories = getAllCategoriesFromRepository()

            // Check each category for perfect results on this date
            categories.forEach { category ->
                try {
                    val success =
                            bettingSuccessChecker.checkBettingSuccessForCategory(
                                    categoryUrl = category.url,
                                    date = date,
                                    categoryName = category.name
                            )

                    if (success) {
                        Timber.i(
                                "Betting success notification sent immediately for category '${category.name}' on $date"
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(
                            e,
                            "Failed to check betting success for category ${category.name} on $date"
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check betting success for all categories on $date")
        }
    }

    /** Get all available categories from Firebase repository */
    private suspend fun getAllCategoriesFromRepository(): List<Category> {
        return try {
            var categories: List<Category> = emptyList()
            firebaseRepository.getCategories().collect { result ->
                result.fold(
                        onSuccess = { cats -> categories = cats },
                        onFailure = { error ->
                            Timber.e(error, "Failed to get categories from Firebase")
                            categories = getFallbackCategories()
                        }
                )
            }
            categories
        } catch (e: Exception) {
            Timber.e(e, "Failed to get categories from Firebase, using fallback")
            getFallbackCategories()
        }
    }

    /** Fallback categories if Firebase is unavailable */
    private fun getFallbackCategories(): List<Category> {
        return listOf(
                Category(url = "today", name = context.getString(R.string.category_today_tips)),
                Category(url = "1", name = context.getString(R.string.category_home_win)),
                Category(url = "2", name = context.getString(R.string.category_away_win)),
                Category(url = "x", name = context.getString(R.string.category_draw)),
                Category(url = "1x", name = context.getString(R.string.category_home_win_or_draw)),
                Category(url = "2x", name = context.getString(R.string.category_away_win_or_draw)),
                Category(url = "over", name = context.getString(R.string.category_over_goals)),
                Category(url = "under", name = context.getString(R.string.category_under_goals)),
                Category(url = "gg", name = context.getString(R.string.category_both_teams_score)),
                Category(url = "ng", name = context.getString(R.string.category_clean_sheet))
        )
    }

    /** Monitor a specific match for result updates */
    fun monitorMatchResult(fixtureId: String) {
        monitorScope.launch {
            try {
                val match = favoriteDao.getFavoriteItemByFixtureIdOrNull(fixtureId)
                if (match == null) {
                    Timber.w("Fixture $fixtureId is no longer in favorites database, skipping real-time monitoring")
                    return@launch
                }

                val date = match.mDate ?: return@launch

                // Add this match's date to monitoring if not already monitored
                if (date !in monitoredDates) {
                    monitoredDates.add(date)
                    monitorDateMatches(date)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to monitor match result for fixture $fixtureId")
            }
        }
    }

    /** Force check for today's completed matches */
    fun checkTodayCompletedCategories() {
        monitorScope.launch {
            try {
                val today = dateFormat.format(Date())
                val todayMatches = favoriteDao.getFavoritesByDate(today)

                if (todayMatches.isNotEmpty()) {
                    val matchesWithResults =
                            todayMatches.filter {
                                !it.outcome.isNullOrBlank() && it.outcome != "0 - 0"
                            }

                    val totalMatches = todayMatches.size
                    val completedMatches = matchesWithResults.size

                    // Check if all matches are completed
                    if (completedMatches == totalMatches && totalMatches > 0) {
                        checkAllCategoriesBettingSuccess(today)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to check today's completed categories")
            }
        }
    }

    /** Stop monitoring and cleanup resources */
    fun stopMonitoring() {
        try {
            monitoredDates.clear()
            completedDates.clear()
            Timber.d("Stopped real-time result monitoring")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop monitoring")
        }
    }

    /** Reset monitoring for a new day */
    fun resetDailyMonitoring() {
        monitorScope.launch {
            try {
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time
                val yesterdayStr = dateFormat.format(yesterday)

                // Remove yesterday's completed dates to free up memory
                completedDates.removeAll { it == yesterdayStr }
                monitoredDates.removeAll { it == yesterdayStr }

                // Start monitoring today's matches
                startMonitoring()

                Timber.d(
                        "Reset daily monitoring - cleared yesterday's data and started monitoring today"
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to reset daily monitoring")
            }
        }
    }
}
