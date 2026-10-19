package com.soccertips.predictx.notification

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/** Repository for managing FCM tokens */
@Singleton
class TokenRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(FCM_PREFS, Context.MODE_PRIVATE)
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val tokensRef = firebaseDatabase.getReference("fcm_tokens")

    private val supportedLanguages = listOf("en", "pt", "fr", "es", "de", "it", "ar", "tr", "sw")
    private fun getSupportedLanguage(): String {
        val systemLang = Locale.getDefault().language.lowercase()
        return if (supportedLanguages.contains(systemLang)) {
            systemLang
        } else {
            "en"
        }
    }

    private val _userLanguage: String by lazy { getSupportedLanguage() }
    val userLanguage: String
        get() = _userLanguage

    // Get or generate a unique device ID
    private val deviceId: String
        get() {
            var id = sharedPreferences.getString(KEY_DEVICE_ID, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                sharedPreferences.edit { putString(KEY_DEVICE_ID, id) }
            }
            return id
        }

    /**
     * Save FCM token to SharedPreferences and send to Firebase
     * @param token The FCM token to save
     * @param isPlaceholder Whether this is a placeholder token (when FCM is unavailable)
     */
    suspend fun saveToken(token: String, isPlaceholder: Boolean = false) {
        Timber.d("Saving FCM token: $token (placeholder: $isPlaceholder)")

        // Save locally
        sharedPreferences.edit {
            putString(KEY_FCM_TOKEN, token)
            putBoolean(KEY_IS_PLACEHOLDER, isPlaceholder)
        }

        // Only send real tokens to Firebase
        if (!isPlaceholder) {
            try {
                val deviceInfo =
                    mapOf(
                        "token" to token,
                        "deviceId" to deviceId,
                        "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                        "osVersion" to "Android ${Build.VERSION.RELEASE}",
                        "appVersion" to getAppVersion(),
                        "language" to userLanguage,
                        "lastUpdated" to System.currentTimeMillis()
                    )

                tokensRef.child(deviceId).setValue(deviceInfo).await()
                Timber.d("Token successfully sent to Firebase")

                // Clear any previous Firebase error flag on success
                clearFirebaseError()
            } catch (e: Exception) {
                // Store that we had a Firebase error, but keep the token locally
                sharedPreferences.edit { putBoolean(KEY_FIREBASE_ERROR, true) }

                // Log different types of errors differently
                when {
                    e.message?.contains("AUTHENTICATION_FAILED") == true -> {
                        Timber.e(
                            e,
                            "FCM token save failed due to authentication - check Firebase configuration"
                        )
                    }

                    e.message?.contains("PERMISSION_DENIED") == true -> {
                        Timber.e(
                            e,
                            "FCM token save failed due to permissions - check Firebase rules"
                        )
                    }

                    e.message?.contains("NetworkException") == true -> {
                        Timber.w(
                            e,
                            "FCM token save failed due to network issues - will retry later"
                        )
                    }

                    else -> {
                        Timber.e(e, "Failed to send token to Firebase: ${e.message}")
                    }
                }
            }
        } else {
            Timber.d("Not sending placeholder token to Firebase")
            // Mark that we're using a placeholder
            sharedPreferences.edit { putBoolean(KEY_FIREBASE_ERROR, true) }
        }
    }

    /** Retry sending a stored token to Firebase (useful after network/auth issues are resolved) */
    suspend fun retryTokenSync() {
        val token = getToken()
        val isPlaceholder = isPlaceholderToken()

        if (token != null && !isPlaceholder && hadFirebaseError()) {
            Timber.d("Retrying FCM token sync to Firebase")
            saveToken(token, false)
        } else if (token != null && isPlaceholder) {
            // If we have a placeholder token, try to get a real FCM token
            Timber.d("Attempting to replace placeholder token with real FCM token")
            try {
                val realToken =
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                saveToken(realToken, false)
            } catch (e: Exception) {
                Timber.w(e, "Failed to get real FCM token, keeping placeholder")
            }
        } else {
            Timber.d(
                "No token retry needed - token: ${token != null}, placeholder: $isPlaceholder, error: ${hadFirebaseError()}"
            )
        }
    }

    /** Get the current FCM token if available */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_FCM_TOKEN, null)
    }

    /** Check if the current token is a placeholder */
    fun isPlaceholderToken(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_PLACEHOLDER, false)
    }

    /** Check if we had an error with Firebase */
    fun hadFirebaseError(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIREBASE_ERROR, false)
    }

    /** Clear the Firebase error flag (after successful reconnection) */
    fun clearFirebaseError() {
        sharedPreferences.edit { putBoolean(KEY_FIREBASE_ERROR, false) }
    }

    /** Observe FCM token as Flow */
    fun getTokenAsFlow(): Flow<String?> = flow { emit(getToken()) }

    /** Delete token (e.g., on user logout) */
    suspend fun deleteToken() {
        val token = getToken()
        val isPlaceholder = isPlaceholderToken()

        // Remove from local storage
        sharedPreferences.edit {
            remove(KEY_FCM_TOKEN)
            remove(KEY_IS_PLACEHOLDER)
        }

        // Only try to remove from Firebase if it's not a placeholder
        if (token != null && !isPlaceholder && !hadFirebaseError()) {
            try {
                tokensRef.child(deviceId).removeValue().await()
                Timber.d("Token successfully removed from Firebase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove token from Firebase")
            }
        }
    }

    /** Get the app version name */
    private fun getAppVersion(): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "unknown"
        }
    }

    companion object {
        private const val FCM_PREFS = "fcm_preferences"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_IS_PLACEHOLDER = "is_placeholder_token"
        private const val KEY_FIREBASE_ERROR = "had_firebase_error"
        private const val KEY_SUBSCRIBED_TOPICS = "subscribed_topics"

        // FCM Topics
        const val TOPIC_BETTING_SUCCESS = "betting_success"
        const val TOPIC_DAILY_TIPS = "daily_tips"
        const val TOPIC_MATCH_UPDATES = "match_updates"
    }

    /**
     * Subscribe to FCM topics for server-side notifications.
     * Call this during app initialization or after user grants notification permission.
     */
    suspend fun subscribeToDefaultTopics() {
        subscribeToTopic(TOPIC_BETTING_SUCCESS)
        subscribeToTopic(TOPIC_DAILY_TIPS)
    }

    /**
     * Subscribe to a specific FCM topic.
     * @param topic The topic name to subscribe to
     */
    suspend fun subscribeToTopic(topic: String) {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .subscribeToTopic(topic)
                .await()

            // Track subscribed topics locally
            val subscribedTopics = getSubscribedTopics().toMutableSet()
            subscribedTopics.add(topic)
            sharedPreferences.edit {
                putStringSet(KEY_SUBSCRIBED_TOPICS, subscribedTopics)
            }

            Timber.d("Successfully subscribed to FCM topic: $topic")
        } catch (e: Exception) {
            Timber.e(e, "Failed to subscribe to FCM topic: $topic")
        }
    }

    /**
     * Unsubscribe from a specific FCM topic.
     * @param topic The topic name to unsubscribe from
     */
    suspend fun unsubscribeFromTopic(topic: String) {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .unsubscribeFromTopic(topic)
                .await()

            // Update local tracking
            val subscribedTopics = getSubscribedTopics().toMutableSet()
            subscribedTopics.remove(topic)
            sharedPreferences.edit {
                putStringSet(KEY_SUBSCRIBED_TOPICS, subscribedTopics)
            }

            Timber.d("Successfully unsubscribed from FCM topic: $topic")
        } catch (e: Exception) {
            Timber.e(e, "Failed to unsubscribe from FCM topic: $topic")
        }
    }

    /**
     * Get the set of currently subscribed topics.
     */
    fun getSubscribedTopics(): Set<String> {
        return sharedPreferences.getStringSet(KEY_SUBSCRIBED_TOPICS, emptySet()) ?: emptySet()
    }

    /**
     * Check if subscribed to a specific topic.
     */
    fun isSubscribedToTopic(topic: String): Boolean {
        return getSubscribedTopics().contains(topic)
    }
}
