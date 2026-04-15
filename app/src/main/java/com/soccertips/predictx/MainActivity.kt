package com.soccertips.predictx

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import com.google.firebase.messaging.FirebaseMessaging
import com.soccertips.predictx.admob.AdStateManager
import com.soccertips.predictx.admob.AppOpenAdManager
import com.soccertips.predictx.admob.InterstitialAdManager
import com.soccertips.predictx.ui.theme.PredictXTheme
import com.soccertips.predictx.update.CustomAppUpdateManager
import com.soccertips.predictx.update.UpdateHandler
import com.soccertips.predictx.util.DevicePerformanceManager
import com.soccertips.predictx.util.StartupTimeTracker
import com.soccertips.predictx.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.checkerframework.checker.initialization.qual.Initialized
import timber.log.Timber

/**
 * Composable that ensures ad managers are properly initialized with Activity context only after the
 * Compose UI has been fully rendered and is stable. This prevents "Window couldn't find content
 * container view" errors.
 *
 * On low-memory devices, ad preloading is deferred to reduce cold start time.
 */
@Composable
private fun AdInitializedContent(
    interstitialAdManager: InterstitialAdManager,
    devicePerformanceManager: DevicePerformanceManager,
    isMobileAdsInitialized: Boolean,
    content: @Composable () -> Unit
) {
    // Get the current activity context in the composable context
    val activity = LocalContext.current as? AppCompatActivity

    // Use LaunchedEffect to initialize ad managers after first composition
    LaunchedEffect(Unit) {
        if (!isMobileAdsInitialized) return@LaunchedEffect
        // Device-aware delay - longer on low-memory devices to prioritize UI
        val isLowMemory = devicePerformanceManager.isLowMemoryDevice()
        val initDelay = if (isLowMemory) {
            devicePerformanceManager.getStartupConfig().deferAdInitializationMs
        } else {
            100L
        }

        delay(initDelay)

        // Initialize ad managers with Activity context
        activity?.let {
            // Set up ad managers with Activity context
            interstitialAdManager.setActivityContext(it)

            // On low-memory devices, skip aggressive preloading during startup
            // Ads will be loaded when needed
            if (!isLowMemory) {
                // Preload interstitial ad for faster availability
                interstitialAdManager.loadAdIfNeeded()
                Timber.d("Ad managers initialized - preloading interstitial ad")
            } else {
                Timber.d("Ad managers initialized - skipping preload on low-memory device")
                // Defer preloading on low-memory devices
                delay(devicePerformanceManager.getStartupConfig().deferPreloadingMs)
                interstitialAdManager.loadAdIfNeeded()
            }
        }
    }

    // Render the content
    content()
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // ViewModels
    private val splashViewModel: SplashViewModel by viewModels()

    // Admob
    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager

    @Inject
    lateinit var startupTimeTracker: StartupTimeTracker

    @Inject
    lateinit var adStateManager: AdStateManager

    @Inject
    lateinit var interstitialAdManager: InterstitialAdManager

    @Inject
    lateinit var devicePerformanceManager: DevicePerformanceManager

    // Custom Update Manager
    @Inject
    lateinit var customAppUpdateManager: CustomAppUpdateManager

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

        // Get FCM token for testing
        getFCMToken()

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
                    devicePerformanceManager = devicePerformanceManager,
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
        Timber.d("Checking permissions...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (!splashViewModel.canScheduleExactAlarms()) {
                    Timber.d("Exact alarm permission not granted, showing dialog")
                    showExactAlarmPermissionDialog()
                } else {
                    Timber.d("Exact alarm permission already granted")
                }
            } catch (e: Exception) {
                Timber.e("Error checking alarm permission: ${e.message}")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!splashViewModel.hasNotificationPermission()) {
                Timber.d("Notification permission not granted, checking rationale...")

                // Check if we should show rationale (user denied before but didn't select "Don't ask again")
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                    // User denied before - show explanation dialog
                    Timber.d("Showing notification permission rationale dialog")
                    showNotificationPermissionRationale()
                } else {
                    // First time asking OR user selected "Don't ask again"
                    // Check if we've asked before
                    val hasAskedBefore = sharedPrefs.getBoolean("notification_permission_asked", false)

                    if (!hasAskedBefore) {
                        // First time - just request
                        Timber.d("First time requesting notification permission")
                        sharedPrefs.edit { putBoolean("notification_permission_asked", true) }
                        requestPermissions(
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                        )
                    } else {
                        // User selected "Don't ask again" - show settings dialog
                        Timber.d("User previously denied with 'Don't ask again', showing settings dialog")
                        showNotificationSettingsDialog()
                    }
                }
            } else {
                Timber.d("Notification permission already granted")
            }
        }
    }

    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.notification_permission_title))
            .setMessage(getString(R.string.notification_permission_message))
            .setPositiveButton(getString(R.string.notification_permission_enable)) { _, _ ->
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton(getString(R.string.notification_permission_not_now), null)
            .show()
    }

    private fun showNotificationSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.notification_disabled_title))
            .setMessage(getString(R.string.notification_disabled_message))
            .setPositiveButton(getString(R.string.notification_open_settings)) { _, _ ->
                openNotificationSettings()
            }
            .setNegativeButton(getString(R.string.notification_permission_not_now), null)
            .show()
    }

    private fun openNotificationSettings() {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Could not open notification settings")
            // Fallback to app settings
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (e2: Exception) {
                Timber.e(e2, "Could not open app settings")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Notification permission granted by user")
                    Toast.makeText(this, getString(R.string.notification_enabled_toast), Toast.LENGTH_SHORT).show()
                } else {
                    Timber.w("Notification permission denied by user")
                    Toast.makeText(this, getString(R.string.notification_disabled_toast), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
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
            .setTitle(getString(R.string.exact_alarm_permission_title))
            .setMessage(getString(R.string.exact_alarm_permission_message))
            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                try {
                    startActivity(intent)
                } catch (e: android.content.ActivityNotFoundException) {
                    Timber.e(e, "No activity found to handle intent: $intent")
                    Toast.makeText(this, getString(R.string.unable_to_open_settings), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
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
            "handleNotificationIntent called with action: ${intent.action}, fromNotification: ${
                intent.getBooleanExtra(
                    "fromNotification",
                    false
                )
            }"
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

    /**
     * Get FCM token for testing Remote Config and Firebase features The token will be logged to
     * Logcat with tag "FCM_TOKEN"
     */
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.tag("FCM_TOKEN").w(task.exception, "Failed to get FCM token")
                return@addOnCompleteListener
            }

            // Get the FCM token
            val token = task.result

            // Log token with special tag for easy filtering
            Timber.tag("FCM_TOKEN").d("═══════════════════════════════════════════════════════")
            Timber.tag("FCM_TOKEN").d("FCM Token: $token")
            Timber.tag("FCM_TOKEN").d("═══════════════════════════════════════════════════════")
        }
    }
}
