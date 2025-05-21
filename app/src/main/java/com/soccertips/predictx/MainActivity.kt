package com.soccertips.predictx

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.soccertips.predictx.ui.theme.PredictXTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var analytics: FirebaseAnalytics

    // In-app update manager
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    // In-app review manager
    private val reviewManager: ReviewManager by lazy { ReviewManagerFactory.create(this) }
    private val fixtureId = mutableStateOf<String?>(null)

    //Cache review info to avoid repeated requests
    private var cachedReviewInfo: ReviewInfo? = null

    //Track update/review request status with preferences
     val sharedPrefs by lazy {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
    }

    //Constants for update and review configurations
    companion object {
        private const val UPDATE_REQUEST_CODE = 100
        private const val UPDATE_TYPE = AppUpdateType.FLEXIBLE
        private const val MIN_DAYS_BETWEEN_REVIEWS = 7
        private const val MIN_LAUNCHES_FOR_REVIEW = 3
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use the modern edge-to-edge API
        enableEdgeToEdge()

        // Note: setDecorFitsSystemWindows is deprecated in Android 15
        // WindowCompat.setDecorFitsSystemWindows(window, false)

        // Instead, let the Activity handle edge-to-edge automatically

        // Initialize AppUpdateManager
        if (UPDATE_TYPE == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateUpdatedListener)
        }

        //Request exact alarm permission - only on Android 12 (S) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            try {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    showExactAlarmPermissionDialog()
                }
            } catch (e: Exception) {
                Timber.e("Error checking exact alarm permission: ${e.message}")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        prefetchReviewInfo()

        // Handle notification intent that launched the app
        handleNotificationIntent(intent)

        setContent {
            PredictXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    AppNavigation(fixtureId = fixtureId.value)
                }
            }
        }

        // Initialize Firebase Analytics
        analytics = FirebaseAnalytics.getInstance(this)

        if (shouldCheckForUpdates()) {
            checkForAppUpdates()
        }

        // Check for app updates when the activity is created
         checkForAppUpdates()
         requestReview()
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> showSnackbarForCompleteUpdate()
            InstallStatus.FAILED -> {
                Timber.e("Update failed: ${state.installErrorCode()}")
                sharedPrefs.edit { putLong("last_update_check", 0) } // Allow retry
            }

            else -> Timber.d("Update status: ${state.installStatus()}")
        }
    }

    private fun shouldCheckForUpdates(): Boolean {
        val lastCheck = sharedPrefs.getLong("last_update_check", 0)
        val now = System.currentTimeMillis()
        // Check once per day maximum
        return now - lastCheck > TimeUnit.DAYS.toMillis(1)
    }

    private fun checkForAppUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            // Mark that we checked for updates
            sharedPrefs.edit { putLong("last_update_check", System.currentTimeMillis()) }

            val isUpdateAvailable =
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isUpdateTypeAllowed = appUpdateInfo.isUpdateTypeAllowed(UPDATE_TYPE)
            val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0

            // Prioritize important updates (older than 5 days)
            val updateOptions = AppUpdateOptions.newBuilder(
                if (stalenessDays > 5) AppUpdateType.IMMEDIATE else UPDATE_TYPE
            ).setAllowAssetPackDeletion(true).build()

            if (isUpdateAvailable && isUpdateTypeAllowed) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        this,
                        updateOptions,
                        UPDATE_REQUEST_CODE
                    )
                    // Log analytics event
                    logAnalyticsEvent("update_flow_started", stalenessDays)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start update flow")
                }
            }
        }.addOnFailureListener { e ->
            Timber.e(e, "Failed to check for app updates")
        }
    }

    // Optimize review flow with smarter triggers
    private fun prefetchReviewInfo() {
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cachedReviewInfo = task.result
                Timber.d("Review info prefetched successfully")
            } else {
                Timber.e(task.exception, "Failed to prefetch review info")
            }
        }
    }

    fun maybeShowReview() {
        if (!shouldShowReview()) return

        // Use cached info or request new one
        val reviewInfo = cachedReviewInfo
        if (reviewInfo != null) {
            launchReviewFlow(reviewInfo)
        } else {
            reviewManager.requestReviewFlow()
                .addOnSuccessListener { launchReviewFlow(it) }
                .addOnFailureListener { e -> Timber.e(e, "Review flow request failed") }
        }
    }

    private fun shouldShowReview(): Boolean {
        val lastReviewTime = sharedPrefs.getLong("last_review_time", 0)
        val appLaunchCount = sharedPrefs.getInt("app_launch_count", 0) + 1
        sharedPrefs.edit { putInt("app_launch_count", appLaunchCount) }

        val now = System.currentTimeMillis()
        val daysSinceLastReview = TimeUnit.MILLISECONDS.toDays(now - lastReviewTime)

        // Show review if:
        // 1. User has launched app enough times
        // 2. Enough time has passed since last review
        return (appLaunchCount >= MIN_LAUNCHES_FOR_REVIEW &&
                (lastReviewTime == 0L || daysSinceLastReview >= MIN_DAYS_BETWEEN_REVIEWS))
    }

    private fun launchReviewFlow(reviewInfo: ReviewInfo) {
        reviewManager.launchReviewFlow(this, reviewInfo)
            .addOnCompleteListener {
                // Save that we showed a review
                sharedPrefs.edit { putLong("last_review_time", System.currentTimeMillis()) }
                Timber.d("Review flow completed")
                logAnalyticsEvent("review_flow_completed")
            }
    }

    private fun logAnalyticsEvent(name: String, stalenessDays: Int = 0) {
        val bundle = Bundle().apply {
            if (stalenessDays > 0) putInt("staleness_days", stalenessDays)
        }
        analytics.logEvent(name, bundle)
    }

    override fun onResume() {
        super.onResume()

        // For IMMEDIATE updates that were interrupted
        if (UPDATE_TYPE == AppUpdateType.IMMEDIATE) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            this,
                            AppUpdateOptions.newBuilder(UPDATE_TYPE)
                                .setAllowAssetPackDeletion(true)
                                .build(),
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to resume update")
                    }
                }
            }
        }

        // Good time to potentially show a review (user is engaged)
        if (Math.random() < 0.3) { // 30% chance when resuming
            maybeShowReview()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exact Alarm Permission Required")
            .setMessage("This app requires permission to schedule exact alarms. Please grant the permission in the settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Set the new intent
        setIntent(intent)

        // Process the notification intent
        if (intent.action == "com.soccertips.predictx.ACTION_VIEW_MATCH" ||
            intent.getBooleanExtra("fromNotification", false)) {

            // Get the fixture ID from the intent
            val idFromIntent = intent.getStringExtra("fixtureId")

            if (!idFromIntent.isNullOrEmpty()) {
                // Update the fixture ID state
                fixtureId.value = idFromIntent

                // Store the ID for navigation handling
                sharedPrefs.edit {
                    putString("pending_navigation_fixture_id", idFromIntent)
                    putLong("notification_open_timestamp", System.currentTimeMillis())
                    // Flag specifically for foreground navigation
                    putBoolean("force_navigate_from_foreground", true)
                }

                Timber.d("Foreground notification click for fixture ID: $idFromIntent - triggering immediate navigation")

                // Force immediate navigation by recreating the content
                setContent {
                    PredictXTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            AppNavigation(fixtureId = idFromIntent, forceNavigate = true)
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.action == "com.soccertips.predictx.ACTION_VIEW_MATCH" || intent?.getBooleanExtra("fromNotification", false) == true) {
            val idFromIntent = intent.getStringExtra("fixtureId")

            if (!idFromIntent.isNullOrEmpty()) {
                // Set the fixture ID that should be displayed
                fixtureId.value = idFromIntent

                // Store the ID for navigation handling
                sharedPrefs.edit {
                    putString("pending_navigation_fixture_id", idFromIntent)
                    putLong("notification_open_timestamp", System.currentTimeMillis())
                    // Mark that this came from a notification for special handling
                    putBoolean("from_notification_click", true)
                }

                Timber.d("Notification click detected for fixture ID: $idFromIntent")
            }
        }
    }

    @Composable
    fun ShowSnackbarForCompleteUpdateWrapper(appUpdateManager: AppUpdateManager) {
        ShowSnackbarForCompleteUpdate(appUpdateManager)
    }

    private fun showSnackbarForCompleteUpdate() {
        setContent {
            ShowSnackbarForCompleteUpdateWrapper(appUpdateManager)
        }
    }

    // In-app review logic
    private fun requestReview() {
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo: ReviewInfo = task.result
                launchReviewFlow(reviewInfo)
            } else {
                val exception = task.exception
                exception?.printStackTrace()
                Timber.e(exception)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (UPDATE_TYPE == AppUpdateType.FLEXIBLE) {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        }
    }
}

@Composable
fun ShowSnackbarForCompleteUpdate(appUpdateManager: AppUpdateManager) {
    val snackbarHostState = remember { SnackbarHostState() }

    SnackbarHost(hostState = snackbarHostState)

    LaunchedEffect(Unit) { // Show snackbar when the update is downloaded
        val result = snackbarHostState.showSnackbar(
            message = "An update has just been downloaded.",
            actionLabel = "RESTART",
        )
        if (result == SnackbarResult.ActionPerformed) {
            appUpdateManager.completeUpdate()
        }
    }
}

