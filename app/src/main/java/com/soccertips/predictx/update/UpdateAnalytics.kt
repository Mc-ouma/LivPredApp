package com.soccertips.predictx.update

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class UpdateAnalytics @Inject constructor(private val firebaseAnalytics: FirebaseAnalytics) {

    fun logUpdateAvailable(stalenessDays: Int, updateType: String, version: String? = null) {
        val bundle =
            Bundle().apply {
                putInt("staleness_days", stalenessDays)
                putString("update_type", updateType)
                version?.let { putString("target_version", it) }
            }
        logEvent("update_available", bundle)
    }

    fun logUpdateStarted(updateType: String, userInitiated: Boolean = false) {
        val bundle =
            Bundle().apply {
                putString("update_type", updateType)
                putBoolean("user_initiated", userInitiated)
            }
        logEvent("update_started", bundle)
    }

    fun logUpdateCompleted(updateType: String, duration: Long) {
        val bundle =
            Bundle().apply {
                putString("update_type", updateType)
                putLong("duration_ms", duration)
            }
        logEvent("update_completed", bundle)
    }

    fun logUpdateFailed(updateType: String, errorCode: Int?, reason: String?) {
        val bundle =
            Bundle().apply {
                putString("update_type", updateType)
                errorCode?.let { putInt("error_code", it) }
                reason?.let { putString("failure_reason", it) }
            }
        logEvent("update_failed", bundle)
    }

    fun logUpdateCancelled(updateType: String, stage: String) {
        val bundle =
            Bundle().apply {
                putString("update_type", updateType)
                putString("cancelled_at", stage)
            }
        logEvent("update_cancelled", bundle)
    }

    fun logUpdateRetry(retryCount: Int, updateType: String) {
        val bundle =
            Bundle().apply {
                putInt("retry_count", retryCount)
                putString("update_type", updateType)
            }
        logEvent("update_retry", bundle)
    }

    private fun logEvent(eventName: String, bundle: Bundle) {
        try {
            firebaseAnalytics.logEvent(eventName, bundle)
            Timber.d("Logged update event: $eventName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to log update event: $eventName")
        }
    }
}
