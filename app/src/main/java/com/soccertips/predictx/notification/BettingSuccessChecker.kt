package com.soccertips.predictx.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.data.model.ServerResponse
import com.soccertips.predictx.repository.FirebaseRepository
import com.soccertips.predictx.repository.PredictionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class BettingSuccessChecker
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val predictionRepository: PredictionRepository,
    private val notificationBuilder: NotificationBuilder,
    private val firebaseRepository: FirebaseRepository
) {

    companion object {
        private const val PREFS_NAME = "betting_success_checker"
        private const val LAST_CHECK_DATE_KEY = "last_check_date"
        private const val NOTIFIED_DATES_KEY = "notified_dates"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Get all available categories from Firebase repository */
    private suspend fun getAllCategoriesFromRepository(): List<Category> {
        return try {
            firebaseRepository.getCategories().first().getOrElse {
                Timber.e("Failed to fetch categories from Firebase, using fallback")
                getFallbackCategories()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching categories, using fallback")
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

    /**
     * Check all categories for a specific date to see if all matches have results and all outcomes
     * are wins
     */
    suspend fun checkBettingSuccessForDate(date: LocalDate) {
        withContext(Dispatchers.IO) {
            try {
                val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                // Check if we've already notified for this date
                val notifiedDates =
                    sharedPrefs.getStringSet(NOTIFIED_DATES_KEY, emptySet()) ?: emptySet()
                if (notifiedDates.contains(dateString)) {
                    Timber.d("Already notified for date: $dateString, skipping check")
                    return@withContext
                }

                // Get all available categories from Firebase
                val categories = getAllCategoriesFromRepository()
                val categoryResults = mutableListOf<CategoryResult>()

                // Check each category individually for perfect results
                for (category in categories) {
                    try {
                        val categoryData = predictionRepository.getCategoryData(category.url)

                        // Filter matches for the specific date
                        val matchesForDate =
                            categoryData.serverResponse.filter { match ->
                                match.mDate == dateString
                            }

                        if (matchesForDate.isNotEmpty()) {
                            // Remove duplicates based on fixtureId within this category
                            val uniqueMatches = matchesForDate.distinctBy { it.fixtureId }

                            // Analyze this category's results
                            val analysis = analyzeMatchResults(uniqueMatches, dateString)

                            if (analysis.shouldSendCongratulations) {
                                categoryResults.add(
                                    CategoryResult(
                                        categoryName = category.name,
                                        categoryUrl = category.url,
                                        analysis = analysis
                                    )
                                )
                                Timber.d(
                                    "Category '${category.name}' achieved perfect results for $dateString"
                                )
                            }
                        }

                        Timber.d(
                            "Category ${category.name}: Found ${matchesForDate.size} matches for $dateString"
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to fetch data from category: ${category.name}")
                    }
                }

                // Send notifications for categories with perfect results
                if (categoryResults.isNotEmpty()) {
                    for (categoryResult in categoryResults) {
                        sendCongratulationsNotification(categoryResult, dateString)
                    }
                    markDateAsNotified(dateString)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking betting success for date: $date")
            }
        }
    }

    /** Analyze match results to determine if congratulations should be sent */
    private fun analyzeMatchResults(matches: List<ServerResponse>, date: String): BettingAnalysis {
        val totalMatches = matches.size
        var matchesWithResults = 0
        var winningMatches = 0
        var losingMatches = 0
        val matchDetails = mutableListOf<String>()

        for (match in matches) {
            val hasResult =
                !match.result.isNullOrBlank() &&
                        match.result != "-" &&
                        match.result != "Unknown" &&
                        match.result != "vs"

            if (hasResult) {
                matchesWithResults++

                // Use outcome directly from server
                val outcome = (match.outcome ?: "Unknown").lowercase()

                when (outcome) {
                    "win" -> {
                        winningMatches++
                        matchDetails.add(
                            "✅ ${match.homeTeam} vs ${match.awayTeam}: ${match.result} (${match.pick}) - ${match.outcome}"
                        )
                        Timber.d(
                            "WIN: ${match.homeTeam} vs ${match.awayTeam} - Pick: ${match.pick}, Result: ${match.result}, Outcome: ${match.outcome}"
                        )
                    }

                    "lose" -> {
                        losingMatches++
                        matchDetails.add(
                            "❌ ${match.homeTeam} vs ${match.awayTeam}: ${match.result} (${match.pick}) - ${match.outcome}"
                        )
                        Timber.d(
                            "LOSE: ${match.homeTeam} vs ${match.awayTeam} - Pick: ${match.pick}, Result: ${match.result}, Outcome: ${match.outcome}"
                        )
                    }

                    else -> {
                        // Treat unknown outcomes as not completed
                        matchesWithResults--
                        Timber.d(
                            "UNKNOWN: ${match.homeTeam} vs ${match.awayTeam} - Pick: ${match.pick}, Result: ${match.result}, Outcome: ${match.outcome}"
                        )
                    }
                }
            }
        }

        val allMatchesCompleted =
            matchesWithResults >= totalMatches * 0.8 // At least 80% have results
        val allWins =
            winningMatches > 0 && losingMatches == 0 && matchesWithResults == winningMatches
        val successRate =
            if (matchesWithResults > 0) (winningMatches * 100) / matchesWithResults else 0

        Timber.d(
            "Analysis for $date: Total=$totalMatches, WithResults=$matchesWithResults, Wins=$winningMatches, Losses=$losingMatches, Rate=$successRate%"
        )

        return BettingAnalysis(
            totalMatches = totalMatches,
            matchesWithResults = matchesWithResults,
            winningMatches = winningMatches,
            losingMatches = losingMatches,
            successRate = successRate,
            shouldSendCongratulations =
                allMatchesCompleted && allWins && winningMatches >= 3, // At least 3 wins
            matchDetails = matchDetails
        )
    }

    /** Send congratulations notification for perfect betting day */
    private fun sendCongratulationsNotification(categoryResult: CategoryResult, date: String) {
        try {
            val notification =
                notificationBuilder
                    .buildBettingSuccessNotification(
                        date = date,
                        categoryName = categoryResult.categoryName,
                        categoryUrl = categoryResult.categoryUrl,
                        totalMatches = categoryResult.analysis.totalMatches,
                        winningMatches = categoryResult.analysis.winningMatches,
                        successRate = categoryResult.analysis.successRate,
                        matchDetails = categoryResult.analysis.matchDetails
                    )
                    .build()

            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = "betting_success_${categoryResult.categoryUrl}_$date".hashCode()

            if (notificationManager.areNotificationsEnabled()) {

                notificationManager.notify(notificationId, notification)

                Timber.i(
                    "Sent congratulations notification for perfect betting day in '${categoryResult.categoryName}': $date"
                )
            } else {
                Timber.w("Notification permission not granted, cannot send betting success notification for '${categoryResult.categoryName}' on date: $date")
            }

        } catch (e: SecurityException) {
            Timber.e(
                e,
                "Security exception when sending notification for category '${categoryResult.categoryName}' on date: $date"
            )
        } catch (e: Exception) {
            Timber.e(
                e,
                "Failed to send congratulations notification for category '${categoryResult.categoryName}' on date: $date"
            )
        }
    }

    /** Mark a date as already notified to avoid duplicate notifications */
    private fun markDateAsNotified(date: String) {
        val notifiedDates =
            sharedPrefs.getStringSet(NOTIFIED_DATES_KEY, emptySet())?.toMutableSet()
                ?: mutableSetOf()
        notifiedDates.add(date)

        // Keep only last 30 days to prevent unlimited growth
        val dateLimit =
            LocalDate.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        notifiedDates.removeAll { it < dateLimit }

        sharedPrefs.edit { putStringSet(NOTIFIED_DATES_KEY, notifiedDates) }
    }

    /** Check betting success for today's date */
    suspend fun checkTodaysBettingSuccess() {
        checkBettingSuccessForDate(LocalDate.now())
    }

    /** Check betting success for yesterday's date (for end-of-day checking) */
    suspend fun checkYesterdaysBettingSuccess() {
        checkBettingSuccessForDate(LocalDate.now().minusDays(1))
    }

    /** Manual trigger to check specific date (for testing or manual verification) */
    suspend fun manualCheckForDate(date: String) {
        try {
            val localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            // Force check by temporarily removing from notified dates
            val notifiedDates =
                sharedPrefs.getStringSet(NOTIFIED_DATES_KEY, emptySet())?.toMutableSet()
                    ?: mutableSetOf()
            notifiedDates.remove(date)
            sharedPrefs.edit { putStringSet(NOTIFIED_DATES_KEY, notifiedDates) }

            checkBettingSuccessForDate(localDate)
        } catch (e: Exception) {
            Timber.e(e, "Failed to manually check betting success for date: $date")
        }
    }

    /**
     * Check betting success for a specific category and date immediately Used by real-time
     * monitoring when all matches in a category are completed
     */
    suspend fun checkBettingSuccessForCategory(
        categoryUrl: String,
        date: String,
        categoryName: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if we've already notified for this category and date
                val notificationKey = "betting_success_${categoryUrl}_$date"
                val notifiedDates =
                    sharedPrefs.getStringSet(NOTIFIED_DATES_KEY, emptySet()) ?: emptySet()
                if (notifiedDates.contains(notificationKey)) {
                    Timber.d(
                        "Already notified for category $categoryName on date: $date, skipping check"
                    )
                    return@withContext false
                }

                // Get category data for the specific date
                val categoryData = predictionRepository.getCategoryData(categoryUrl)

                // Filter matches for the specific date
                val matchesForDate =
                    categoryData.serverResponse.filter { match -> match.mDate == date }

                if (matchesForDate.isEmpty()) {
                    Timber.d("No matches found for category $categoryName on date $date")
                    return@withContext false
                }

                // Remove duplicates based on fixtureId within this category
                val uniqueMatches = matchesForDate.distinctBy { it.fixtureId }

                // Analyze this category's results
                val analysis = analyzeMatchResults(uniqueMatches, date)

                if (analysis.shouldSendCongratulations) {
                    val categoryResult =
                        CategoryResult(
                            categoryName = categoryName,
                            categoryUrl = categoryUrl,
                            analysis = analysis
                        )

                    // Send immediate notification
                    sendCongratulationsNotification(categoryResult, date)

                    // Mark this specific category-date as notified
                    markCategoryDateAsNotified(notificationKey)

                    Timber.i(
                        "Sent immediate betting success notification for category '$categoryName' on $date"
                    )
                    return@withContext true
                } else {
                    Timber.d(
                        "Category '$categoryName' on $date does not meet criteria for congratulations: " +
                                "Total=${analysis.totalMatches}, WithResults=${analysis.matchesWithResults}, " +
                                "Wins=${analysis.winningMatches}, Losses=${analysis.losingMatches}"
                    )
                    return@withContext false
                }
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Error checking betting success for category $categoryName on date: $date"
                )
                return@withContext false
            }
        }
    }

    /** Mark a specific category-date combination as already notified */
    private fun markCategoryDateAsNotified(notificationKey: String) {
        val notifiedDates =
            sharedPrefs.getStringSet(NOTIFIED_DATES_KEY, emptySet())?.toMutableSet()
                ?: mutableSetOf()
        notifiedDates.add(notificationKey)

        // Keep only last 30 days to prevent unlimited growth
        val dateLimit =
            LocalDate.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        notifiedDates.removeAll { key -> key.substringAfterLast("_") < dateLimit }

        sharedPrefs.edit { putStringSet(NOTIFIED_DATES_KEY, notifiedDates) }
    }
}

/** Data class to hold betting analysis results */
data class BettingAnalysis(
    val totalMatches: Int,
    val matchesWithResults: Int,
    val winningMatches: Int,
    val losingMatches: Int,
    val successRate: Int,
    val shouldSendCongratulations: Boolean,
    val matchDetails: List<String>
)

/** Data class to hold category-specific betting results */
data class CategoryResult(
    val categoryName: String,
    val categoryUrl: String,
    val analysis: BettingAnalysis
)
