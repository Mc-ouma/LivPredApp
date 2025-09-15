package com.soccertips.predictx.update

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow
import timber.log.Timber
import androidx.core.content.edit

@Singleton
class UpdateRetryManager @Inject constructor(private val sharedPrefs: SharedPreferences) {
    companion object {
        private const val RETRY_COUNT_KEY = "update_retry_count"
        private const val LAST_RETRY_TIME_KEY = "last_update_retry_time"
        private const val MAX_RETRY_COUNT = 3
        private const val BASE_DELAY_MS = 30_000L // 30 seconds
        private const val MAX_DELAY_MS = 300_000L // 5 minutes
    }

    fun shouldRetry(): Boolean {
        val retryCount = getRetryCount()
        val lastRetryTime = getLastRetryTime()
        val currentTime = System.currentTimeMillis()

        if (retryCount >= MAX_RETRY_COUNT) {
            Timber.d("Max retry count reached: $retryCount")
            return false
        }

        val timeSinceLastRetry = currentTime - lastRetryTime
        val requiredDelay = calculateBackoffDelay(retryCount)

        return timeSinceLastRetry >= requiredDelay
    }

    suspend fun executeWithRetry(operation: suspend () -> Boolean): Boolean {
        getRetryCount()

        if (!shouldRetry()) {
            Timber.d("Retry not allowed yet or max retries exceeded")
            return false
        }

        return try {
            val success = operation()
            if (success) {
                resetRetryState()
                Timber.d("Update operation succeeded")
            } else {
                incrementRetryCount()
                Timber.d("Update operation failed, retry count: ${getRetryCount()}")
            }
            success
        } catch (e: Exception) {
            incrementRetryCount()
            Timber.e(e, "Update operation failed with exception, retry count: ${getRetryCount()}")
            false
        }
    }

    private fun calculateBackoffDelay(retryCount: Int): Long {
        val exponentialDelay = BASE_DELAY_MS * (2.0.pow(retryCount.toDouble())).toLong()
        return min(exponentialDelay, MAX_DELAY_MS)
    }

    private fun getRetryCount(): Int {
        return sharedPrefs.getInt(RETRY_COUNT_KEY, 0)
    }

    private fun getLastRetryTime(): Long {
        return sharedPrefs.getLong(LAST_RETRY_TIME_KEY, 0)
    }

    private fun incrementRetryCount() {
        val currentCount = getRetryCount()
        sharedPrefs
            .edit {
                putInt(RETRY_COUNT_KEY, currentCount + 1)
                    .putLong(LAST_RETRY_TIME_KEY, System.currentTimeMillis())
            }
    }

    private fun resetRetryState() {
        sharedPrefs.edit { remove(RETRY_COUNT_KEY).remove(LAST_RETRY_TIME_KEY) }
    }

    fun getNextRetryTime(): Long? {
        val retryCount = getRetryCount()
        if (retryCount >= MAX_RETRY_COUNT) return null

        val lastRetryTime = getLastRetryTime()
        val requiredDelay = calculateBackoffDelay(retryCount)

        return lastRetryTime + requiredDelay
    }
}
