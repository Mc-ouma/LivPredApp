package com.soccertips.predictx.viewmodel

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.soccertips.predictx.util.DevicePerformanceManager
import com.soccertips.predictx.util.StartupTimeTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SplashViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val reviewManager: ReviewManager,
    private val sharedPrefs: SharedPreferences,
    private val devicePerformanceManager: DevicePerformanceManager
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _initializationState =
        MutableStateFlow<InitializationState>(InitializationState.Starting)

    // Cache review info
    private var cachedReviewInfo: ReviewInfo? = null

    // Lazy initialize Firebase Analytics to prevent main thread blocking
    private val analytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }

    fun initialize() {
        viewModelScope.launch {
            try {
                _initializationState.value = InitializationState.InitializingEssentials

                // Record process start time for cold start tracking
                StartupTimeTracker.recordProcessStart()

                // Initialize device performance manager if not already done
                devicePerformanceManager.initialize(context)
                val isLowMemory = devicePerformanceManager.isLowMemoryDevice()

                Timber.d("SplashViewModel: isLowMemory=$isLowMemory, tier=${devicePerformanceManager.getPerformanceTier()}")

                // Essential initialization on main thread - keep minimal
                withContext(Dispatchers.Main) {
                    // Only essential operations on main thread
                    updateAppLaunchCount()

                    // On low-memory devices, skip non-essential setup on main thread
                    if (!isLowMemory) {
                        setupAdManagers()
                    }
                }

                _initializationState.value = InitializationState.InitializingBackground

                // Background initialization - device-aware delays
                withContext(Dispatchers.IO) {
                    // On low-memory devices, use minimal delay to release splash faster
                    val initDelay = if (isLowMemory) 50L else 100L
                    delay(initDelay)
                    initializeBackgroundComponents(isLowMemory)
                }

                _initializationState.value = InitializationState.Complete

                // Mark as ready to dismiss splash screen
                _isReady.value = true

                Timber.d("Splash initialization completed successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error during splash initialization")
                _initializationState.value = InitializationState.Error(e.message ?: "Unknown error")
                // Still mark as ready to prevent infinite splash
                _isReady.value = true
            }
        }
    }

    fun shouldCheckPermissions(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } catch (e: Exception) {
                Timber.e("Error checking alarm permission: ${e.message}")
                true // Assume permission is granted if we can't check
            }
        } else {
            true // Permission not required for older versions
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required for older versions
        }
    }

    fun shouldShowReview(): Boolean {
        val lastReviewTime = sharedPrefs.getLong("last_review_time", 0)
        val appLaunchCount = sharedPrefs.getInt("app_launch_count", 0)

        val now = System.currentTimeMillis()
        val daysSinceLastReview =
            java.util.concurrent.TimeUnit.MILLISECONDS.toDays(now - lastReviewTime)

        return (appLaunchCount >= MIN_LAUNCHES_FOR_REVIEW &&
                (lastReviewTime == 0L || daysSinceLastReview >= MIN_DAYS_BETWEEN_REVIEWS))
    }

    fun getCachedReviewInfo(): ReviewInfo? = cachedReviewInfo

    fun logAnalyticsEvent(name: String, stalenessDays: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bundle =
                    android.os.Bundle().apply {
                        if (stalenessDays > 0) putInt("staleness_days", stalenessDays)
                    }
                analytics.logEvent(name, bundle)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log analytics event: $name")
            }
        }
    }

    private fun setupAdManagers() {
        try {
            // Initial setup without activity context
            // Activity context will be set when setActivityContext is called
            Timber.d("Ad managers setup initiated")
        } catch (e: Exception) {
            Timber.e(e, "Error setting up ad managers")
        }
    }

    private fun updateAppLaunchCount() {
        try {
            val launches = sharedPrefs.getInt("app_launch_count", 0) + 1
            sharedPrefs.edit { putInt("app_launch_count", launches) }
            Timber.d("App launch count updated: $launches")
        } catch (e: Exception) {
            Timber.w(e, "Failed to update app launch count")
        }
    }

    private suspend fun initializeBackgroundComponents(isLowMemory: Boolean = false) {
        try {
            // On low-memory devices, skip review prefetch during startup
            // It will be fetched when actually needed
            if (!isLowMemory) {
                prefetchReviewInfoAsync()
                // Lower priority operations with delays
                delay(500)
            } else {
                Timber.d("Skipping review prefetch on low-memory device")
            }
            // Additional background initialization can be added here

        } catch (e: Exception) {
            Timber.e(e, "Error during background initialization")
        }
    }

    private suspend fun prefetchReviewInfoAsync() {
        withContext(Dispatchers.IO) {
            try {
                reviewManager.requestReviewFlow().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        cachedReviewInfo = task.result
                        Timber.d("Review info prefetched successfully")
                    } else {
                        Timber.e(task.exception, "Failed to prefetch review info")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error prefetching review info")
            }
        }
    }

    sealed class InitializationState {
        object Starting : InitializationState()
        object InitializingEssentials : InitializationState()
        object InitializingBackground : InitializationState()
        object Complete : InitializationState()
        data class Error(val message: String) : InitializationState()
    }

    companion object {
        private const val MIN_DAYS_BETWEEN_REVIEWS = 7
        private const val MIN_LAUNCHES_FOR_REVIEW = 3
    }
}
