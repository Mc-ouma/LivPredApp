package com.soccertips.predictx.notification

import android.content.Context
import android.widget.Toast
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug utility for testing betting success notifications.
 *
 * Use this class during development to:
 * - Check FCM topic subscriptions
 * - View/clear notified dates
 * - Trigger manual notification checks
 * - Verify the notification system is working correctly
 *
 * Example usage in a debug Fragment:
 * ```kotlin
 * @Inject
 * lateinit var debugHelper: BettingSuccessDebugHelper
 *
 * // Check subscription status
 * debugHelper.logTopicSubscriptions()
 *
 * // Clear and retest
 * debugHelper.clearAllNotifiedDates()
 * bettingSuccessScheduler.checkImmediately("2026-02-05")
 * ```
 */
@Singleton
class BettingSuccessDebugHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bettingSuccessChecker: BettingSuccessChecker,
    private val bettingSuccessScheduler: BettingSuccessScheduler,
    private val tokenRepository: TokenRepository
) {

    companion object {
        private const val BETTING_PREFS = "betting_success_checker"
        private const val FCM_PREFS = "fcm_preferences"
        private const val NOTIFIED_DATES_KEY = "notified_dates"
        private const val SUBSCRIBED_TOPICS_KEY = "subscribed_topics"
    }

    /**
     * Log current FCM topic subscriptions
     */
    fun logTopicSubscriptions(): Set<String> {
        val prefs = context.getSharedPreferences(FCM_PREFS, Context.MODE_PRIVATE)
        val topics = prefs.getStringSet(SUBSCRIBED_TOPICS_KEY, emptySet()) ?: emptySet()

        Timber.d("═══════════════════════════════════════════════")
        Timber.d("FCM Topic Subscriptions:")
        topics.forEach { topic ->
            Timber.d("  ✅ $topic")
        }
        if (topics.isEmpty()) {
            Timber.d("  ⚠️ No topics subscribed")
        }
        Timber.d("═══════════════════════════════════════════════")

        return topics
    }

    /**
     * Check if subscribed to betting_success topic
     */
    fun isSubscribedToBettingSuccess(): Boolean {
        val topics = logTopicSubscriptions()
        val isSubscribed = topics.contains(TokenRepository.TOPIC_BETTING_SUCCESS)
        Timber.d("Subscribed to betting_success: $isSubscribed")
        return isSubscribed
    }

    /**
     * Log all notified dates
     */
    fun logNotifiedDates(): Set<String> {
        val prefs = context.getSharedPreferences(BETTING_PREFS, Context.MODE_PRIVATE)
        val notifiedDates = prefs.getStringSet(NOTIFIED_DATES_KEY, emptySet()) ?: emptySet()

        Timber.d("═══════════════════════════════════════════════")
        Timber.d("Notified Dates (${notifiedDates.size} total):")
        notifiedDates.sortedDescending().forEach { date ->
            Timber.d("  📅 $date")
        }
        if (notifiedDates.isEmpty()) {
            Timber.d("  (none)")
        }
        Timber.d("═══════════════════════════════════════════════")

        return notifiedDates
    }

    /**
     * Check if a specific date has been notified
     */
    fun isDateNotified(date: String): Boolean {
        val isNotified = bettingSuccessChecker.isDateAlreadyNotified(date)
        Timber.d("Date $date notified: $isNotified")
        return isNotified
    }

    /**
     * Clear all notified dates (for retesting)
     */
    fun clearAllNotifiedDates() {
        val prefs = context.getSharedPreferences(BETTING_PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            remove(NOTIFIED_DATES_KEY)
        }
        Timber.d("✅ Cleared all notified dates")
    }

    /**
     * Clear a specific date from notified list
     */
    fun clearNotifiedDate(date: String) {
        val prefs = context.getSharedPreferences(BETTING_PREFS, Context.MODE_PRIVATE)
        val notifiedDates = prefs.getStringSet(NOTIFIED_DATES_KEY, emptySet())?.toMutableSet()
            ?: mutableSetOf()

        notifiedDates.remove(date)

        prefs.edit {
            putStringSet(NOTIFIED_DATES_KEY, notifiedDates)
        }
        Timber.d("✅ Cleared notified date: $date")
    }

    /**
     * Trigger immediate check for a specific date
     */
    suspend fun triggerCheck(date: String) {
        Timber.d("═══════════════════════════════════════════════")
        Timber.d("Triggering manual check for date: $date")
        Timber.d("═══════════════════════════════════════════════")

        bettingSuccessScheduler.checkImmediately(date)
    }

    /**
     * Trigger check for today and yesterday
     */
    suspend fun triggerTodayAndYesterdayCheck() {
        Timber.d("═══════════════════════════════════════════════")
        Timber.d("Triggering check for today and yesterday")
        Timber.d("═══════════════════════════════════════════════")

        bettingSuccessScheduler.checkImmediately()
    }

    /**
     * Force subscribe to betting_success topic
     */
    suspend fun forceSubscribeToBettingSuccess() {
        Timber.d("Force subscribing to betting_success topic...")
        tokenRepository.subscribeToDefaultTopics()
    }

    /**
     * Run complete diagnostics
     */
    suspend fun runDiagnostics() {
        Timber.d("╔═══════════════════════════════════════════════╗")
        Timber.d("║     BETTING SUCCESS DIAGNOSTICS               ║")
        Timber.d("╚═══════════════════════════════════════════════╝")

        // 1. Check topic subscriptions
        val topics = logTopicSubscriptions()
        val isSubscribed = topics.contains(TokenRepository.TOPIC_BETTING_SUCCESS)

        // 2. Check notified dates
        val notifiedDates = logNotifiedDates()

        // 3. Log summary
        Timber.d("═══════════════════════════════════════════════")
        Timber.d("SUMMARY:")
        Timber.d("  FCM betting_success subscription: ${if (isSubscribed) "✅ YES" else "❌ NO"}")
        Timber.d("  Notified dates count: ${notifiedDates.size}")
        Timber.d("═══════════════════════════════════════════════")

        if (!isSubscribed) {
            Timber.w("⚠️ Not subscribed to betting_success topic! Subscribing now...")
            forceSubscribeToBettingSuccess()
        }
    }

    /**
     * Show toast with subscription status (for UI testing)
     */
    fun showSubscriptionStatus() {
        val isSubscribed = isSubscribedToBettingSuccess()
        val message = if (isSubscribed) {
            "✅ Subscribed to betting_success"
        } else {
            "❌ NOT subscribed to betting_success"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Show toast with notified dates (for UI testing)
     */
    fun showNotifiedDates() {
        val dates = logNotifiedDates()
        val message = if (dates.isEmpty()) {
            "No dates notified yet"
        } else {
            "Notified: ${dates.take(3).joinToString(", ")}${if (dates.size > 3) "..." else ""}"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

