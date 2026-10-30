package com.soccertips.predictx.notification

import android.content.Context
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.repository.FirebaseRepository
import com.soccertips.predictx.repository.PredictionRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Real-time monitor for match result updates that triggers immediate betting success notifications
 * when all matches for a date are completed with results in any category
 */
@Singleton
class RealTimeResultMonitor
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val firebaseRepository: FirebaseRepository,
        private val predictionRepository: PredictionRepository,
        private val bettingSuccessChecker: BettingSuccessChecker
) {

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Flow to emit date completion events
    private val _dateCompletionEvents = MutableSharedFlow<DateCompletionEvent>()
    val dateCompletionEvents: Flow<DateCompletionEvent> = _dateCompletionEvents.asSharedFlow()

    // Track categories and dates being monitored to avoid duplicate checks
    private val monitoredCategoryDates = mutableSetOf<String>()
    private val completedCategoryDates = mutableSetOf<String>()

    data class DateCompletionEvent(
            val date: String,
            val categoryUrl: String,
            val categoryName: String,
            val totalMatches: Int,
            val completedMatches: Int
    )

    /** Start monitoring all categories for result updates */
    fun startMonitoring() {
        monitorScope.launch {
            try {
                Timber.d("Starting real-time result monitoring for all categories")

                // Get today's and yesterday's dates
                val today = dateFormat.format(Date())
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time
                val yesterdayStr = dateFormat.format(yesterday)

                // Get all available categories
                val categories = getAllCategoriesFromRepository()

                // Monitor each category for each date
                listOf(today, yesterdayStr).forEach { date ->
                    categories.forEach { category ->
                        val categoryDateKey = "${category.url}_$date"
                        if (categoryDateKey !in monitoredCategoryDates) {
                            monitoredCategoryDates.add(categoryDateKey)
                            monitorCategoryDateMatches(category, date)
                        }
                    }
                }

                Timber.d(
                        "Monitoring ${categories.size} categories for dates: $today, $yesterdayStr"
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to start real-time monitoring")
            }
        }
    }

    /** Monitor matches for a specific category and date */
    private fun monitorCategoryDateMatches(category: Category, date: String) {
        monitorScope.launch {
            try {
                val categoryDateKey = "${category.url}_$date"

                // Continuously monitor matches for this category and date
                while (categoryDateKey !in completedCategoryDates) {
                    try {
                        // Get category data from the repository (not favorites)
                        val categoryData = predictionRepository.getCategoryData(category.url)

                        // Filter matches for the specific date
                        val matchesForDate =
                                categoryData.serverResponse.filter { match -> match.mDate == date }

                        if (matchesForDate.isEmpty()) {
                            Timber.d("No matches found for category ${category.name} on date $date")
                            break
                        }

                        // Remove duplicates based on fixtureId
                        val uniqueMatches = matchesForDate.distinctBy { it.fixtureId }

                        val matchesWithResults =
                                uniqueMatches.filter { match ->
                                    !match.result.isNullOrBlank() &&
                                            match.result != "-" &&
                                            match.result != "Unknown" &&
                                            match.result != "vs" &&
                                            !match.outcome.isNullOrBlank() &&
                                            match.outcome!!.lowercase() in listOf("win", "lose")
                                }

                        val totalMatches = uniqueMatches.size
                        val completedMatches = matchesWithResults.size

                        Timber.d(
                                "Category ${category.name} on $date: $completedMatches/$totalMatches matches completed"
                        )

                        // Check if all matches for this category and date are completed
                        if (completedMatches >= totalMatches * 0.8 && totalMatches > 0
                        ) { // At least 80% completed
                            completedCategoryDates.add(categoryDateKey)

                            // Emit completion event
                            _dateCompletionEvents.emit(
                                    DateCompletionEvent(
                                            date = date,
                                            categoryUrl = category.url,
                                            categoryName = category.name,
                                            totalMatches = totalMatches,
                                            completedMatches = completedMatches
                                    )
                            )

                            // Trigger immediate betting success check for this specific category
                            val success =
                                    bettingSuccessChecker.checkBettingSuccessForCategory(
                                            categoryUrl = category.url,
                                            date = date,
                                            categoryName = category.name
                                    )

                            if (success) {
                                Timber.i(
                                        "Betting success notification sent for category '${category.name}' on $date"
                                )
                            } else {
                                Timber.d(
                                        "No betting success notification needed for category '${category.name}' on $date"
                                )
                            }
                            break
                        }

                        // Wait before next check (every 30 seconds for active monitoring)
                        kotlinx.coroutines.delay(30_000)
                    } catch (e: Exception) {
                        Timber.e(e, "Error monitoring category ${category.name} on date $date")
                        kotlinx.coroutines.delay(60_000) // Wait longer on error
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to monitor matches for category ${category.name} on date $date")
            }
        }
    }

    /** Get all available categories from Firebase repository */
    private suspend fun getAllCategoriesFromRepository(): List<Category> {
        return try {
            val result =
                    withTimeoutOrNull(10_000) {
                        firebaseRepository.getCategories().first()
                    }

            if (result == null) {
                Timber.w("Timed out fetching categories from Firebase, using fallback")
                return getFallbackCategories()
            }

            result.fold(
                    onSuccess = { cats -> cats },
                    onFailure = { error ->
                        Timber.e(error, "Failed to get categories from Firebase")
                        getFallbackCategories()
                    }
            )
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
                Timber.d("Monitoring match result for fixture $fixtureId in all categories")

                // Get all categories and check which ones contain this fixture
                val categories = getAllCategoriesFromRepository()
                val today = dateFormat.format(Date())

                categories.forEach { category ->
                    try {
                        val categoryData = predictionRepository.getCategoryData(category.url)
                        val match = categoryData.serverResponse.find { it.fixtureId == fixtureId }

                        if (match != null) {
                            val date = match.mDate ?: today
                            val categoryDateKey = "${category.url}_$date"

                            // Add this category-date to monitoring if not already monitored
                            if (categoryDateKey !in monitoredCategoryDates) {
                                monitoredCategoryDates.add(categoryDateKey)
                                monitorCategoryDateMatches(category, date)
                                Timber.d(
                                        "Started monitoring category ${category.name} for fixture $fixtureId on date $date"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(
                                e,
                                "Error checking category ${category.name} for fixture $fixtureId"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to monitor match result for fixture $fixtureId")
            }
        }
    }

    /** Force check for today's completed categories */
    fun checkTodayCompletedCategories() {
        monitorScope.launch {
            try {
                val today = dateFormat.format(Date())
                val categories = getAllCategoriesFromRepository()

                categories.forEach { category ->
                    try {
                        val categoryData = predictionRepository.getCategoryData(category.url)
                        val todayMatches = categoryData.serverResponse.filter { it.mDate == today }

                        if (todayMatches.isNotEmpty()) {
                            val uniqueMatches = todayMatches.distinctBy { it.fixtureId }
                            val matchesWithResults =
                                    uniqueMatches.filter { match ->
                                        !match.result.isNullOrBlank() &&
                                                match.result != "-" &&
                                                match.result != "Unknown" &&
                                                match.result != "vs" &&
                                                !match.outcome.isNullOrBlank() &&
                                                match.outcome!!.lowercase() in listOf("win", "lose")
                                    }

                            val totalMatches = uniqueMatches.size
                            val completedMatches = matchesWithResults.size

                            // Check if all matches are completed
                            if (completedMatches >= totalMatches * 0.8 && totalMatches > 0) {
                                bettingSuccessChecker.checkBettingSuccessForCategory(
                                        categoryUrl = category.url,
                                        date = today,
                                        categoryName = category.name
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(
                                e,
                                "Error checking today's completed matches for category ${category.name}"
                        )
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
            monitoredCategoryDates.clear()
            completedCategoryDates.clear()
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

                // Remove yesterday's completed category-dates to free up memory
                completedCategoryDates.removeAll { it.endsWith("_$yesterdayStr") }
                monitoredCategoryDates.removeAll { it.endsWith("_$yesterdayStr") }

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
