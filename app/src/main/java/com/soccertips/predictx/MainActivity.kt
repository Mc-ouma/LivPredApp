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
import com.soccertips.predictx.admob.AdStateManager
import com.soccertips.predictx.admob.InterstitialAdManager
import com.soccertips.predictx.admob.RewardedAdManager
import com.soccertips.predictx.ui.theme.PredictXTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    //Admob
    @Inject
    lateinit var interstitialAdManager: InterstitialAdManager

    @Inject
    lateinit var rewardedAdManager: RewardedAdManager

    @Inject
    lateinit var adStateManager: AdStateManager

    // Initialize Firebase Analytics
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
        // Removed setTheme call to prevent theme switching delay

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Use the modern edge-to-edge API


        // Note: we're no longer manually initializing ad managers here since they're injected
        // The AdStateManager is now also properly injected as a dependency

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
                    AppNavigation(
                        fixtureId = fixtureId.value,
                        interstitialAdManager = interstitialAdManager,
                        rewardedAdManager = rewardedAdManager,

                        )
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
        setIntent(intent)
        intent?.let { handleNotificationIntent(it) }
    }

    private fun handleNotificationIntent(intent: Intent) {
        val fromNotification = intent.getBooleanExtra("fromNotification", false)

        if (fromNotification) {
            val action = intent.action
            val notificationType = intent.getStringExtra("notificationType")

            when {
                action == "com.soccertips.predictx.ACTION_VIEW_BETTING_SUCCESS" ||
                        notificationType == "betting_success" -> {
                    handleBettingSuccessIntent(intent)
                }

                action == "com.soccertips.predictx.ACTION_VIEW_BETTING_HISTORY" -> {
                    handleBettingHistoryIntent(intent)
                }

                action == "com.soccertips.predictx.ACTION_VIEW_MATCH" -> {
                    handleMatchIntent(intent)
                }
            }
        }
    }

    private fun handleBettingSuccessIntent(intent: Intent) {
        // Extract betting success data
        val date = intent.getStringExtra("betting_date") ?: ""
        val matchCount = intent.getStringExtra("match_count") ?: "0"
        val winCount = intent.getStringExtra("win_count") ?: "0"
        val successRate = intent.getStringExtra("success_rate") ?: "0"
        val matchesDetails = intent.getStringExtra("matches_details") ?: ""
        val summary = intent.getStringExtra("summary") ?: ""

        // Log the success for analytics
        logBettingSuccessEvent(date, matchCount, winCount, successRate)

        // Show celebration dialog or navigate to success screen
        showBettingSuccessDialog(date, matchCount, winCount, successRate, matchesDetails, summary)
    }

    private fun handleBettingHistoryIntent(intent: Intent) {
        val filterDate = intent.getStringExtra("filter_date")
        Timber.d("Navigating to betting history with date filter: $filterDate")
        // TODO: Implement navigation to your betting history screen/composable
        // You might need to use a navigation controller to navigate to the correct destination
        // and pass the filterDate as an argument.
    }

    private fun handleMatchIntent(intent: Intent) {
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
                        AppNavigation(
                            fixtureId = idFromIntent,
                            forceNavigate = true,
                            interstitialAdManager,
                            rewardedAdManager
                        )
                    }
                }
            }
        }
    }

    private fun showBettingSuccessDialog(
        date: String,
        matchCount: String,
        winCount: String,
        successRate: String,
        matchesDetails: String,
        summary: String
    ) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("🎉 Perfect Betting Day!")
            .setMessage(
                """
                              📅 Date: $date
                              🏆 Matches Won: $winCount/$matchCount
                              🎯 Success Rate: $successRate%
                              
                              📊 Summary: $summary
                              
                              📋 Match Results:
                              $matchesDetails
                          """.trimIndent()
            )
            .setPositiveButton("View History") { _, _ ->
                handleBettingHistoryIntent(Intent().apply {
                    putExtra("filter_date", date)
                })
            }
            .setNegativeButton("Share Success") { _, _ ->
                shareSuccess(matchCount, successRate, date)
            }
            .setNeutralButton("Close", null)
            .create()

        dialog.show()
    }

    private fun shareSuccess(matchCount: String, successRate: String, date: String) {
        val shareText = """
                          🎉 Perfect Betting Day! 🎉
                          
                          📅 Date: $date
                          🏆 All $matchCount matches won!
                          🎯 Success Rate: $successRate%
                          
                          #BettingSuccess #PerfectDay #SoccerTips
                      """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Betting Success"))
    }

    private fun logBettingSuccessEvent(
        date: String,
        matchCount: String,
        winCount: String,
        successRate: String
    ) {
        val bundle = Bundle().apply {
            putString("date", date)
            putLong("match_count", matchCount.toLongOrNull() ?: 0L)
            putLong("win_count", winCount.toLongOrNull() ?: 0L)
            putDouble("success_rate", successRate.toDoubleOrNull() ?: 0.0)
        }
        analytics.logEvent("betting_success_notification", bundle)

        // Log to Timber for debugging
        Timber.i("Betting Success Event - Date: $date, Matches: $matchCount, Wins: $winCount, Rate: $successRate%")
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