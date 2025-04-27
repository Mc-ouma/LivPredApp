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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.ui.categories.CategoriesScreen
import com.soccertips.predictx.ui.favorites.FavoritesScreen
import com.soccertips.predictx.ui.fixturedetails.FixtureDetailsScreen
import com.soccertips.predictx.ui.items.ItemsListScreen
import com.soccertips.predictx.ui.team.TeamScreen
import com.soccertips.predictx.ui.theme.PredictXTheme
import com.soccertips.predictx.viewmodel.CategoriesViewModel
import com.soccertips.predictx.viewmodel.SharedViewModel
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
    private val sharedPrefs by lazy {
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
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize AppUpdateManager
        if (UPDATE_TYPE == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateUpdatedListener)
        }
        //Request exact alarm permission
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            showExactAlarmPermissionDialog()
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

        setContent {
            PredictXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    AppNavigation(fixtureId.value)

                }
            }
        }
        // Handle notification intent
        handleNotificationIntent(intent, fixtureId)

        // Initialize Firebase Analytics
        analytics = FirebaseAnalytics.getInstance(this)

        if (shouldCheckForUpdates()) {
            checkForAppUpdates()
        }

        // Check for app updates when the activity is created
        // checkForAppUpdates()
        // requestReview()
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
        handleNotificationIntent(intent, null)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun handleNotificationIntent(intent: Intent?, fixtureId: MutableState<String?>?) {
        val id = intent?.getStringExtra("fixtureId")
        if (!id.isNullOrEmpty()) {
            fixtureId?.value = id
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

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppNavigation(fixtureId: String? = null) {
    val navController = rememberNavController()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val uiState by categoriesViewModel.uiState.collectAsState()
    val sharedViewModel: SharedViewModel = hiltViewModel()


    NavHost(
        navController = navController,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        startDestination = Routes.Splash.route
    ) {
        // Splash Screen
        composable(Routes.Splash.route) {
            SplashScreen(
                navController = navController,
                initialFixtureId = fixtureId,
                onSplashCompleted = { sharedViewModel.markSplashCompleted() }
            )
        }

        composable(Routes.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Routes.Categories.route) {
            CategoriesScreen(navController = navController)
        }
        composable(
            Routes.Favorites.route
        ) {
            FavoritesScreen(navController = navController)
        }

        composable(
            Routes.ItemsList.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideOutOfContainer(
                    animationSpec = tween(300, easing = EaseOut),
                    towards = AnimatedContentTransitionScope.SlideDirection.End
                )
            },

            ) { backStackEntry ->

            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val decodeUrl = java.net.URLDecoder.decode(categoryId, "UTF-8")
            if (uiState is UiState.Success) {
                val categories = (uiState as UiState.Success<List<Category>>).data
                ItemsListScreen(
                    navController = navController,
                    categoryId = decodeUrl,
                    categories = categories,
                )
            }
        }
        composable(
            Routes.FixtureDetails.route,
            arguments = listOf(navArgument("fixtureId") { type = NavType.StringType }),
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideIntoContainer(
                    animationSpec = tween(300, easing = EaseIn),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(
                        300, easing = LinearEasing
                    )
                ) + slideOutOfContainer(
                    animationSpec = tween(300, easing = EaseOut),
                    towards = AnimatedContentTransitionScope.SlideDirection.End
                )
            },
        ) { backStackEntry ->
            val fixtureId = backStackEntry.arguments?.getString("fixtureId") ?: ""

            FixtureDetailsScreen(
                navController = navController,
                fixtureId = fixtureId,
            )
        }
        composable(
            Routes.TeamDetails.route,
            arguments = listOf(
                navArgument("teamId") { type = NavType.StringType },
                navArgument("leagueId") { type = NavType.StringType },
                navArgument("season") { type = NavType.StringType }),

            )
        { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
            val leagueId = backStackEntry.arguments?.getString("leagueId") ?: ""
            val season = backStackEntry.arguments?.getString("season") ?: ""

            TeamScreen(
                navController = navController,
                teamId = teamId,
                leagueId = leagueId,
                season = season,


                )
        }
    }
    // Handle deep links after splash screen
    LaunchedEffect(fixtureId) {
        if (sharedViewModel.isSplashCompleted) {
            fixtureId?.let { id ->
                navController.navigate(Routes.FixtureDetails.createRoute(id))
            }
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