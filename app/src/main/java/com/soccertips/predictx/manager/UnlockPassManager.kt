package com.soccertips.predictx.manager

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Manages unlock passes that users earn by watching rewarded ads. Each pass can be used to unlock
 * any category for 24 hours.
 */
@Singleton
class UnlockPassManager @Inject constructor(private val sharedPreferences: SharedPreferences) {
    companion object {
        private const val PASS_BALANCE_KEY = "unlock_passes"
        private const val MAX_PASSES = 20
        private const val UNLOCK_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val UNLOCK_PREFIX = "unlock_"
    }

    private val _passBalance = MutableStateFlow(getPassBalance())
    val passBalance: StateFlow<Int> = _passBalance.asStateFlow()

    /** Get the current number of passes the user has */
    private fun getPassBalance(): Int {
        return sharedPreferences.getInt(PASS_BALANCE_KEY, 0).coerceIn(0, MAX_PASSES)
    }

    /**
     * Add a pass to the user's balance (earned from watching an ad)
     * @return true if pass was added, false if already at max
     */
    fun addPass(): Boolean {
        val current = getPassBalance()
        if (current < MAX_PASSES) {
            val newBalance = current + 1
            sharedPreferences.edit { putInt(PASS_BALANCE_KEY, newBalance) }
            _passBalance.value = newBalance
            Timber.d("Pass added. New balance: $newBalance/$MAX_PASSES")
            return true
        }
        Timber.w("Cannot add pass - already at maximum ($MAX_PASSES)")
        return false
    }

    /**
     * Use a pass to unlock a category for 24 hours
     * @param categoryUrl The URL/ID of the category to unlock
     * @return true if pass was used successfully, false if no passes available
     */
    fun usePass(categoryUrl: String): Boolean {
        val current = getPassBalance()
        if (current > 0) {
            sharedPreferences.edit {
                // Deduct pass
                putInt(PASS_BALANCE_KEY, current - 1)
                // Unlock category by storing current timestamp
                putLong(UNLOCK_PREFIX + categoryUrl, System.currentTimeMillis())
            }
            _passBalance.value = current - 1
            Timber.d("Pass used for category: $categoryUrl. Remaining passes: ${current - 1}")
            return true
        }
        Timber.w("Cannot use pass - no passes available")
        return false
    }

    /**
     * Check if a category is currently unlocked
     * @param categoryUrl The URL/ID of the category to check
     * @return true if unlocked and not expired, false otherwise
     */
    fun isCategoryUnlocked(categoryUrl: String): Boolean {
        val unlockTimestamp = sharedPreferences.getLong(UNLOCK_PREFIX + categoryUrl, 0L)
        if (unlockTimestamp == 0L) {
            return false
        }

        val currentTime = System.currentTimeMillis()
        val isUnlocked = (currentTime - unlockTimestamp) < UNLOCK_DURATION_MS

        if (!isUnlocked) {
            // Clean up expired unlock
            sharedPreferences.edit { remove(UNLOCK_PREFIX + categoryUrl) }
            Timber.d("Category $categoryUrl unlock expired")
        }

        return isUnlocked
    }

    /**
     * Get the time remaining (in milliseconds) until a category locks again
     * @return milliseconds remaining, or 0 if not unlocked
     */
    fun getTimeRemainingMs(categoryUrl: String): Long {
        val unlockTimestamp = sharedPreferences.getLong(UNLOCK_PREFIX + categoryUrl, 0L)
        if (unlockTimestamp == 0L) return 0L

        val currentTime = System.currentTimeMillis()
        val expiryTime = unlockTimestamp + UNLOCK_DURATION_MS
        val remaining = expiryTime - currentTime

        return if (remaining > 0) remaining else 0L
    }

    /** Check if user has at least one pass */
    fun hasPass(): Boolean = getPassBalance() > 0

    /** Check if user can earn more passes */
    fun canEarnMore(): Boolean = getPassBalance() < MAX_PASSES

    /** Get the maximum number of passes allowed */
    fun getMaxPasses(): Int = MAX_PASSES

    /** Refresh the pass balance (useful after returning from background) */
    fun refreshBalance() {
        _passBalance.value = getPassBalance()
    }

    /** Clear all unlocks and reset pass balance (for testing/debugging) */
    fun reset() {
        val keys = sharedPreferences.all.keys.filter { it.startsWith(UNLOCK_PREFIX) }
        sharedPreferences.edit {
            keys.forEach { remove(it) }
            remove(PASS_BALANCE_KEY)
        }
        _passBalance.value = 0
        Timber.d("UnlockPassManager reset")
    }

    /** Get formatted pass balance string for display (e.g., "3/5") */
    fun getFormattedBalance(): String {
        return "${getPassBalance()}/$MAX_PASSES"
    }
}
