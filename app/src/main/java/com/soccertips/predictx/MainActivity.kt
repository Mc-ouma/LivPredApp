package com.soccertips.predictx

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.soccertips.predictx.admob.AdStateManager
import com.soccertips.predictx.admob.AppOpenAdManager
import com.soccertips.predictx.admob.InterstitialAdManager
import com.soccertips.predictx.admob.RewardedAdManager
import com.soccertips.predictx.ui.theme.PredictXTheme
import com.soccertips.predictx.update.CustomAppUpdateManager
import com.soccertips.predictx.update.UpdateHandler
import com.soccertips.predictx.util.StartupTimeTracker
import com.soccertips.predictx.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Composable that ensures ad managers are properly initialized with Activity context only after the
 * Compose UI has been fully rendered and is stable. This prevents "Window couldn't find content
 * container view" errors.
 */
@Composable
private fun AdInitializedContent(
        interstitialAdManager: InterstitialAdManager,
        rewardedAdManager: RewardedAdManager,
        content: @Composable () -> Unit
) {
    // Get the current activity context in the composable context
    val activity = LocalContext.current as? ComponentActivity

    // Use LaunchedEffect to initialize ad managers after first composition
    LaunchedEffect(Unit) {
        // Delay to ensure the Compose UI has fully rendered its first frame
        delay(100)

        // Initialize ad managers with Activity context
        activity?.let {
            // Set up ad managers with Activity context
            interstitialAdManager.setActivityContext(it)
            interstitialAdManager.useActivityContextForAdLoading(true)
            rewardedAdManager.setActivityContext(it)
            rewardedAdManager.useActivityContextForAdLoading(true)

            // Load initial ads once after app startup (not on every screen open)
            interstitialAdManager.loadAdIfNeeded()

            Timber.d("Ad managers initialized after UI rendering completed")
        }
    }

    // Render the content
    content()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ViewModels
    private val splashViewModel: SplashViewModel by viewModels()

    // Admob
    @Inject lateinit var appOpenAdManager: AppOpenAdManager

    @Inject lateinit var startupTimeTracker: StartupTimeTracker

    @Inject lateinit var adStateManager: AdStateManager
    @Inject lateinit var interstitialAdManager: InterstitialAdManager
    @Inject lateinit var rewardedAdManager: RewardedAdManager

    // Custom Update Manager
    @Inject lateinit var customAppUpdateManager: CustomAppUpdateManager

    // Lazy initialize for review functionality
    private val analytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    // In-app review manager
    private val reviewManager: ReviewManager by lazy { ReviewManagerFactory.create(this) }
    private val fixtureId = mutableStateOf<String?>(null)

    val sharedPrefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {

        // window.decorView
        // Install splash screen before super.onCreate()
        val splashScreen = installSplashScreen()

        // Keep splash visible while initialization is happening
        splashScreen.setKeepOnScreenCondition { !splashViewModel.isReady.value }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Handle notification intents on app launch
        intent?.let { handleNotificationIntent(it) }

        // Add lifecycle observation for custom update manager
        lifecycle.addObserver(customAppUpdateManager)

        // Start initialization process
        splashViewModel.initialize()

        // Set content and observe the splash state
        setContent {
            val isReady by splashViewModel.isReady.collectAsState()

            // Only show content when ready
            if (isReady) {
                // Initialize ad managers after UI is ready
                AdInitializedContent(
                        interstitialAdManager = interstitialAdManager,
                        rewardedAdManager = rewardedAdManager,
                ) {
                    PredictXTheme {
                        Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surface
                        ) {
                            AppNavigation(
                                    fixtureId = fixtureId.value,
                            )
                        }

                        // Handle update notifications in the UI with new custom update manager
                        UpdateHandler(updateManager = customAppUpdateManager)
                    }
                }
            }
        }

        // Handle intent quickly without heavy processing
        handleInitialIntent(intent)

        // Check permissions after splash initialization
        lifecycleScope.launch {
            splashViewModel.isReady.collect { ready ->
                if (ready && splashViewModel.shouldCheckPermissions()) {
                    checkPermissions()
                }
            }
        }

        // Defer heavy initialization to background after UI is ready
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000) // Wait for splash to complete
            initializeBackgroundComponents()
        }
    }

    private suspend fun initializeBackgroundComponents() {
        withContext(Dispatchers.IO) {
            // Check for updates using the custom update manager with retry logic after a delay
            delay(1000)
            customAppUpdateManager.checkForUpdatesWithRetry()
        }
    }

    private fun handleInitialIntent(intent: Intent?) {
        // Just extract fixtureId - defer heavy processing
        if (intent?.getBooleanExtra("fromNotification", false) == true) {
            intent.getStringExtra("fixtureId")?.let { id ->
                fixtureId.value = id
                sharedPrefs.edit {
                    putString("pending_navigation_fixture_id", id)
                    putLong("notification_open_timestamp", System.currentTimeMillis())
                }
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (!splashViewModel.canScheduleExactAlarms()) {
                    showExactAlarmPermissionDialog()
                }
            } catch (e: Exception) {
                Timber.e("Error checking alarm permission: ${e.message}")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!splashViewModel.hasNotificationPermission()) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    private fun maybeShowReview() {
        if (!splashViewModel.shouldShowReview()) return

        lifecycleScope.launch(Dispatchers.Main) {
            val reviewInfo = splashViewModel.getCachedReviewInfo()
            if (reviewInfo != null) {
                launchReviewFlow(reviewInfo)
            } else {
                reviewManager
                        .requestReviewFlow()
                        .addOnSuccessListener { launchReviewFlow(it) }
                        .addOnFailureListener { e -> Timber.e(e, "Review flow request failed") }
            }
        }
    }

    private fun launchReviewFlow(reviewInfo: ReviewInfo) {
        try {
            reviewManager.launchReviewFlow(this, reviewInfo).addOnCompleteListener {
                sharedPrefs.edit { putLong("last_review_time", System.currentTimeMillis()) }
                logAnalyticsEvent("review_flow_completed")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error launching review flow")
        }
    }

    private fun logAnalyticsEvent(name: String, stalenessDays: Int = 0) {
        splashViewModel.logAnalyticsEvent(name, stalenessDays)
    }

    private fun logAnalyticsEventWithResultCode(name: String, resultCode: Int) {
        try {
            analytics.logEvent(name, Bundle().apply { putInt("result_code", resultCode) })
        } catch (e: Exception) {
            Timber.e(e, "Failed to log analytics event: $name")
        }
    }

    override fun onResume() {
        super.onResume()

        // For IMMEDIATE updates that were interrupted - use custom update manager
        customAppUpdateManager.resumeUpdateIfNeeded(this)

        // Show review with low probability
        if (Math.random() < 0.2) { // 20% chance
            lifecycleScope.launch(Dispatchers.Default) {
                delay(1500) // Wait for UI to settle
                maybeShowReview()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(this)
                .setTitle("Exact Alarm Permission Required")
                .setMessage(
                        "This app requires permission to schedule exact alarms. Please grant the permission in the settings."
                )
                .setPositiveButton("Go to Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    try {
                        startActivity(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        android.util.Log.e(
                                "MainActivity",
                                "No activity found to handle intent: $intent",
                                e
                        )
                        Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        Timber.d("MainActivity.onNewIntent called with action: ${intent.action}")
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent) {
        Timber.d(
                "handleNotificationIntent called with action: ${intent.action}, fromNotification: ${intent.getBooleanExtra("fromNotification", false)}"
        )

        if (!intent.getBooleanExtra("fromNotification", false)) return

        val action = intent.action
        val notificationType = intent.getStringExtra("notificationType")

        lifecycleScope.launch(Dispatchers.Main) {
            when {
                action == "com.soccertips.predictx.ACTION_VIEW_BETTING_SUCCESS" ||
                        notificationType == "betting_success" -> {
                    Timber.d("Handling betting success intent")
                    handleBettingSuccessIntent(intent)
                }
                action == "com.soccertips.predictx.ACTION_VIEW_BETTING_HISTORY" -> {
                    Timber.d("Handling betting history intent")
                    handleBettingHistoryIntent(intent)
                }
                action == "com.soccertips.predictx.ACTION_VIEW_MATCH" -> {
                    Timber.d("Handling match intent")
                    handleMatchIntent(intent)
                }
            }
        }
    }

    private fun handleBettingSuccessIntent(intent: Intent) {
        val date = intent.getStringExtra("betting_date") ?: ""
        val categoryUrl = intent.getStringExtra("category_url") ?: ""
        val matchCount = intent.getStringExtra("match_count") ?: "0"
        val winCount = intent.getStringExtra("win_count") ?: "0"
        val successRate = intent.getStringExtra("success_rate") ?: "0"
        intent.getStringExtra("matches_details") ?: ""
        intent.getStringExtra("summary") ?: ""

        Timber.d("handleBettingSuccessIntent: date=$date, categoryUrl=$categoryUrl")

        lifecycleScope.launch(Dispatchers.IO) {
            logBettingSuccessEvent(date, matchCount, winCount, successRate)
        }

        // Store the category URL for navigation
        if (categoryUrl.isNotEmpty()) {
            Timber.d("Storing navigation data: categoryUrl=$categoryUrl, date=$date")
            sharedPrefs.edit {
                putString("pending_navigation_category_url", categoryUrl)
                putString("pending_navigation_date", date)
                putBoolean("pending_navigation_from_betting_success", true)
            }
        }

        // Note: Navigation will be handled by AppNavigation compose function
        // which will check for pending navigation and route to the ItemsListScreen
    }

    private fun handleBettingHistoryIntent(intent: Intent) {
        val filterDate = intent.getStringExtra("filter_date")
        Timber.d("Navigating to betting history with date: $filterDate")
    }

    private fun handleMatchIntent(intent: Intent) {
        val idFromIntent = intent.getStringExtra("fixtureId")

        if (!idFromIntent.isNullOrEmpty()) {
            fixtureId.value = idFromIntent

            sharedPrefs.edit {
                putString("pending_navigation_fixture_id", idFromIntent)
                putLong("notification_open_timestamp", System.currentTimeMillis())
                putBoolean("force_navigate_from_foreground", true)
            }
        }
    }

    private fun shareSuccess(matchCount: String, successRate: String, date: String) {
        val shareText =
                """
                🎉 Perfect Betting Day! 🎉
    
                📅 Date: $date
                🏆 All $matchCount matches won!
                🎯 Success Rate: $successRate%
    
                #BettingSuccess #PerfectDay #SoccerTips
            """.trimIndent()

        startActivity(
                Intent.createChooser(
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        },
                        "Share Betting Success"
                )
        )
    }

    private fun logBettingSuccessEvent(
            date: String,
            matchCount: String,
            winCount: String,
            successRate: String
    ) {
        try {
            analytics.logEvent(
                    "betting_success_notification",
                    Bundle().apply {
                        putString("date", date)
                        putLong("match_count", matchCount.toLongOrNull() ?: 0L)
                        putLong("win_count", winCount.toLongOrNull() ?: 0L)
                        putDouble("success_rate", successRate.toDoubleOrNull() ?: 0.0)
                    }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to log betting success event")
        }
    }

    @Deprecated("Using onActivityResult is deprecated in favor of ActivityResultContracts")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Custom update manager (handle flexible update downloads with new approach)
        when (resultCode) {
            RESULT_OK -> {
                Timber.d("Update flow completed successfully")
                logAnalyticsEvent("update_flow_completed")
            }
            RESULT_CANCELED -> {
                Timber.d("Update flow cancelled by user")
                logAnalyticsEvent("update_flow_cancelled")
            }
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                Timber.e("Update flow failed")
                logAnalyticsEvent("update_flow_failed")
            }
            else -> {
                Timber.d("Update flow result: $resultCode")
                logAnalyticsEventWithResultCode("update_flow_unknown_result", resultCode)
            }
        }
    }
}
