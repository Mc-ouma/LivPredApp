package com.soccertips.predictx.notification

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing FCM tokens
 */
@Singleton
class TokenRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences = context.getSharedPreferences(FCM_PREFS, Context.MODE_PRIVATE)
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val tokensRef = firebaseDatabase.getReference("fcm_tokens")

    // Get or generate a unique device ID
    private val deviceId: String
        get() {
            var id = sharedPreferences.getString(KEY_DEVICE_ID, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                sharedPreferences.edit {
                    putString(KEY_DEVICE_ID, id)
                }
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
                val deviceInfo = mapOf(
                    "token" to token,
                    "deviceId" to deviceId,
                    "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "osVersion" to "Android ${Build.VERSION.RELEASE}",
                    "appVersion" to getAppVersion(),
                    "lastUpdated" to System.currentTimeMillis()
                )

                tokensRef.child(deviceId).setValue(deviceInfo).await()
                Timber.d("Token successfully sent to Firebase")
            } catch (e: Exception) {
                // Store that we had a Firebase error, but keep the token locally
                sharedPreferences.edit {
                    putBoolean(KEY_FIREBASE_ERROR, true)
                }
                Timber.e(e, "Failed to send token to Firebase")
            }
        } else {
            Timber.d("Not sending placeholder token to Firebase")
            // Mark that we're using a placeholder
            sharedPreferences.edit {
                putBoolean(KEY_FIREBASE_ERROR, true)
            }
        }
    }

    /**
     * Get the current FCM token if available
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Check if the current token is a placeholder
     */
    fun isPlaceholderToken(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_PLACEHOLDER, false)
    }

    /**
     * Check if we had an error with Firebase
     */
    fun hadFirebaseError(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIREBASE_ERROR, false)
    }

    /**
     * Clear the Firebase error flag (after successful reconnection)
     */
    fun clearFirebaseError() {
        sharedPreferences.edit {
            putBoolean(KEY_FIREBASE_ERROR, false)
        }
    }

    /**
     * Observe FCM token as Flow
     */
    fun getTokenAsFlow(): Flow<String?> = flow {
        emit(getToken())
    }

    /**
     * Delete token (e.g., on user logout)
     */
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

    /**
     * Get the app version name
     */
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
    }
}
