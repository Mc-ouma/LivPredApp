package com.soccertips.predictx.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Helper class to properly initialize Firebase services */
@Singleton
class FirebaseInitializer @Inject constructor() {


    /** Initialize Firebase with proper configuration check */
    fun initializeFirebase(context: Context): Boolean {
        return try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                Timber.d("Firebase already initialized")
                return validateFirebaseConfiguration()
            }

            // Initialize Firebase with default configuration
            FirebaseApp.initializeApp(context)
            Timber.d("Firebase initialized successfully")

            validateFirebaseConfiguration()

        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase")
            false
        }
    }

    /** Validate Firebase configuration and services */
    private fun validateFirebaseConfiguration(): Boolean {
        return try {
            val firebaseApp = FirebaseApp.getInstance()
            val options = firebaseApp.options

            // Validate essential configuration
            if (options.projectId.isNullOrBlank()) {
                Timber.e("Firebase project ID is missing")
                return false
            }

            if (options.applicationId.isNullOrBlank()) {
                Timber.e("Firebase application ID is missing")
                return false
            }

            if (options.apiKey.isNullOrBlank()) {
                Timber.e("Firebase API key is missing")
                return false
            }

            Timber.d("Firebase configuration validated successfully")
            Timber.d("Project ID: ${options.projectId}")
            Timber.d("Application ID: ${options.applicationId}")
            Timber.d("Firebase API Key: ${options.apiKey}")

            // Test FCM service availability
            testFcmService()

            true
        } catch (e: Exception) {
            Timber.e(e, "Firebase configuration validation failed")
            false
        }
    }

    /** Test FCM service to ensure it's properly configured */
    private fun testFcmService() {
        try {
            val messaging = FirebaseMessaging.getInstance()

            // Check if auto-init is enabled
            if (!messaging.isAutoInitEnabled) {
                Timber.w("FCM auto-init is disabled, enabling it")
                messaging.isAutoInitEnabled = true
            }

            Timber.d("FCM service is available and auto-init enabled")
        } catch (e: Exception) {
            Timber.e(e, "FCM service test failed")
        }
    }

}
