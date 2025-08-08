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
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.SnackbarHost
    import androidx.compose.material3.SnackbarHostState
    import androidx.compose.material3.SnackbarResult
    import androidx.compose.material3.Surface
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.DisposableEffect
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.rememberCoroutineScope
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.unit.dp
    import androidx.core.content.edit
    import androidx.lifecycle.lifecycleScope
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
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import timber.log.Timber
    import java.util.concurrent.TimeUnit
    import javax.inject.Inject

    @AndroidEntryPoint
    class MainActivity : ComponentActivity() {

        // Admob
        @Inject
        lateinit var interstitialAdManager: InterstitialAdManager

        @Inject
        lateinit var rewardedAdManager: RewardedAdManager

        @Inject
        lateinit var adStateManager: AdStateManager

        // Lazy initialize Firebase Analytics to prevent main thread blocking
        private val analytics: FirebaseAnalytics by lazy {
            FirebaseAnalytics.getInstance(this)
        }

        // In-app update manager
        private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

        // In-app review manager
        private val reviewManager: ReviewManager by lazy { ReviewManagerFactory.create(this) }
        private val fixtureId = mutableStateOf<String?>(null)

        // Cache review info
        private var cachedReviewInfo: ReviewInfo? = null

        val sharedPrefs by lazy {
            getSharedPreferences("app_prefs", MODE_PRIVATE)
        }

        companion object {
            private const val UPDATE_REQUEST_CODE = 100
            private const val UPDATE_TYPE = AppUpdateType.FLEXIBLE
            private const val MIN_DAYS_BETWEEN_REVIEWS = 7
            private const val MIN_LAUNCHES_FOR_REVIEW = 3
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun onCreate(savedInstanceState: Bundle?) {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)

            // Configure ad managers - lightweight operation
            setupAdManagers()

            // Set content first to improve perceived performance
            setContent {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                PredictXTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding( top = innerPadding.calculateTopPadding(),
                                    bottom = 0.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            AppNavigation(
                                fixtureId = fixtureId.value,
                                interstitialAdManager = interstitialAdManager,
                                rewardedAdManager = rewardedAdManager,
                            )
                        }
                    }

                    // Handle update notifications in the UI
                    UpdateSnackbarHandler(snackbarHostState, scope)
                }
            }

            // Handle intent that launched the app - keep this lightweight
            handleInitialIntent(intent)

            // Move heavy operations to background
            lifecycleScope.launch(Dispatchers.Default) {
                delay(500) // Ensure UI is responsive first

                // Check permissions on main thread after UI is shown
                withContext(Dispatchers.Main) {
                    checkPermissions()
                }

                // Background operations
                prefetchReviewInfoAsync()

                // Lower priority operations
                delay(1000)
                checkForUpdatesIfNeeded()
            }
        }

        private fun setupAdManagers() {
            interstitialAdManager.setActivityContext(this)
            interstitialAdManager.useActivityContextForAdLoading(true)
            rewardedAdManager.setActivityContext(this)
            rewardedAdManager.useActivityContextForAdLoading(true)
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

        private fun checkPermissions() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                    if (!alarmManager.canScheduleExactAlarms()) {
                        showExactAlarmPermissionDialog()
                    }
                } catch (e: Exception) {
                    Timber.e("Error checking alarm permission: ${e.message}")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        1
                    )
                }
            }
        }

        private fun checkForUpdatesIfNeeded() {
            if (shouldCheckForUpdates()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    checkForAppUpdates()
                }
            }
        }

       @Composable
       private fun UpdateSnackbarHandler(
           snackbarHostState: SnackbarHostState,
           scope: kotlinx.coroutines.CoroutineScope
       ) {
           val updateListener = remember {
               InstallStateUpdatedListener { state ->
                   if (state.installStatus() == InstallStatus.DOWNLOADED) {
                       scope.launch {
                           val result = snackbarHostState.showSnackbar(
                               message = "An update has just been downloaded.",
                               actionLabel = "RESTART",
                           )
                           if (result == SnackbarResult.ActionPerformed) {
                               appUpdateManager.completeUpdate()
                           }
                       }
                   }
               }
           }

           DisposableEffect(appUpdateManager) {
               if (UPDATE_TYPE == AppUpdateType.FLEXIBLE) {
                   appUpdateManager.registerListener(updateListener)
               }

               onDispose {
                   if (UPDATE_TYPE == AppUpdateType.FLEXIBLE) {
                       appUpdateManager.unregisterListener(updateListener)
                   }
               }
           }
       }
        private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> {
                    // Handled by the UpdateSnackbarHandler composable
                    Timber.d("Update downloaded")
                }
                InstallStatus.FAILED -> {
                    Timber.e("Update failed: ${state.installErrorCode()}")
                    sharedPrefs.edit { putLong("last_update_check", 0) }
                }
                else -> Timber.d("Update status: ${state.installStatus()}")
            }
        }

        private fun shouldCheckForUpdates(): Boolean {
            val lastCheck = sharedPrefs.getLong("last_update_check", 0)
            val now = System.currentTimeMillis()
            return now - lastCheck > TimeUnit.DAYS.toMillis(1)
        }

        private fun checkForAppUpdates() {
            try {
                appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                    sharedPrefs.edit { putLong("last_update_check", System.currentTimeMillis()) }

                    val isUpdateAvailable =
                        appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    val isUpdateTypeAllowed = appUpdateInfo.isUpdateTypeAllowed(UPDATE_TYPE)
                    val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0

                    val updateOptions = AppUpdateOptions.newBuilder(
                        if (stalenessDays > 5) AppUpdateType.IMMEDIATE else UPDATE_TYPE
                    ).setAllowAssetPackDeletion(true).build()

                    if (isUpdateAvailable && isUpdateTypeAllowed) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    this@MainActivity,
                                    updateOptions,
                                    UPDATE_REQUEST_CODE
                                )
                                logAnalyticsEvent("update_flow_started", stalenessDays)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to start update flow")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during update check")
            }
        }

        private fun maybeShowReview() {
            if (!shouldShowReview()) return

            lifecycleScope.launch(Dispatchers.Main) {
                val reviewInfo = cachedReviewInfo
                if (reviewInfo != null) {
                    launchReviewFlow(reviewInfo)
                } else {
                    reviewManager.requestReviewFlow()
                        .addOnSuccessListener { launchReviewFlow(it) }
                        .addOnFailureListener { e -> Timber.e(e, "Review flow request failed") }
                }
            }
        }

        private fun shouldShowReview(): Boolean {
            val lastReviewTime = sharedPrefs.getLong("last_review_time", 0)
            val appLaunchCount = sharedPrefs.getInt("app_launch_count", 0) + 1
            sharedPrefs.edit { putInt("app_launch_count", appLaunchCount) }

            val now = System.currentTimeMillis()
            val daysSinceLastReview = TimeUnit.MILLISECONDS.toDays(now - lastReviewTime)

            return (appLaunchCount >= MIN_LAUNCHES_FOR_REVIEW &&
                    (lastReviewTime == 0L || daysSinceLastReview >= MIN_DAYS_BETWEEN_REVIEWS))
        }

        private fun launchReviewFlow(reviewInfo: ReviewInfo) {
            try {
                reviewManager.launchReviewFlow(this, reviewInfo)
                    .addOnCompleteListener {
                        sharedPrefs.edit { putLong("last_review_time", System.currentTimeMillis()) }
                        logAnalyticsEvent("review_flow_completed")
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error launching review flow")
            }
        }

        private fun logAnalyticsEvent(name: String, stalenessDays: Int = 0) {
            try {
                val bundle = Bundle().apply {
                    if (stalenessDays > 0) putInt("staleness_days", stalenessDays)
                }
                analytics.logEvent(name, bundle)
            } catch (e: Exception) {
                Timber.e(e, "Failed to log analytics event: $name")
            }
        }

        override fun onResume() {
            super.onResume()

            // Update ad managers with current activity context
            interstitialAdManager.setActivityContext(this)
            rewardedAdManager.setActivityContext(this)

            // For IMMEDIATE updates that were interrupted
            if (UPDATE_TYPE == AppUpdateType.IMMEDIATE) {
                lifecycleScope.launch(Dispatchers.IO) {
                    appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                        if (appUpdateInfo.updateAvailability() ==
                            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                        ) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                try {
                                    appUpdateManager.startUpdateFlowForResult(
                                        appUpdateInfo,
                                        this@MainActivity,
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
                }
            }

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

            lifecycleScope.launch(Dispatchers.Default) {
                intent?.let { handleNotificationIntent(it) }
            }
        }

        private fun handleNotificationIntent(intent: Intent) {
            if (!intent.getBooleanExtra("fromNotification", false)) return

            val action = intent.action
            val notificationType = intent.getStringExtra("notificationType")

            lifecycleScope.launch(Dispatchers.Main) {
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
            val date = intent.getStringExtra("betting_date") ?: ""
            val matchCount = intent.getStringExtra("match_count") ?: "0"
            val winCount = intent.getStringExtra("win_count") ?: "0"
            val successRate = intent.getStringExtra("success_rate") ?: "0"
            val matchesDetails = intent.getStringExtra("matches_details") ?: ""
            val summary = intent.getStringExtra("summary") ?: ""

            lifecycleScope.launch(Dispatchers.IO) {
                logBettingSuccessEvent(date, matchCount, winCount, successRate)
            }

            showBettingSuccessDialog(date, matchCount, winCount, successRate, matchesDetails, summary)
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

        private fun showBettingSuccessDialog(
            date: String, matchCount: String, winCount: String,
            successRate: String, matchesDetails: String, summary: String
        ) {
            AlertDialog.Builder(this)
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
                .show()
        }

        private fun shareSuccess(matchCount: String, successRate: String, date: String) {
            val shareText = """
                🎉 Perfect Betting Day! 🎉
    
                📅 Date: $date
                🏆 All $matchCount matches won!
                🎯 Success Rate: $successRate%
    
                #BettingSuccess #PerfectDay #SoccerTips
            """.trimIndent()

            startActivity(Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }, "Share Betting Success"))
        }

        private fun logBettingSuccessEvent(
            date: String, matchCount: String, winCount: String, successRate: String
        ) {
            try {
                analytics.logEvent("betting_success_notification", Bundle().apply {
                    putString("date", date)
                    putLong("match_count", matchCount.toLongOrNull() ?: 0L)
                    putLong("win_count", winCount.toLongOrNull() ?: 0L)
                    putDouble("success_rate", successRate.toDoubleOrNull() ?: 0.0)
                })
            } catch (e: Exception) {
                Timber.e(e, "Failed to log betting success event")
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                if (UPDATE_TYPE == AppUpdateType.FLEXIBLE) {
                    appUpdateManager.unregisterListener(installStateUpdatedListener)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error unregistering update listener")
            }
        }
    }