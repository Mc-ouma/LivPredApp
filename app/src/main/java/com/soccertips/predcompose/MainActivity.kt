package com.soccertips.predcompose

import android.app.AlarmManager
import android.content.Intent
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
import com.soccertips.predcompose.data.model.Category
import com.soccertips.predcompose.navigation.Routes
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.ui.categories.CategoriesScreen
import com.soccertips.predcompose.ui.favorites.FavoritesScreen
import com.soccertips.predcompose.ui.fixturedetails.FixtureDetailsScreen
import com.soccertips.predcompose.ui.items.ItemsListScreen
import com.soccertips.predcompose.ui.team.TeamScreen
import com.soccertips.predcompose.ui.theme.PredComposeTheme
import com.soccertips.predcompose.viewmodel.CategoriesViewModel
import com.soccertips.predcompose.viewmodel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // In-app update manager
    private lateinit var appUpdateManager: AppUpdateManager
    private val updateRequestCode = 100 // Request code for in-app updates
    private val updateType = AppUpdateType.FLEXIBLE // Update type: FLEXIBLE or IMMEDIATE

    // In-app review manager
    private val reviewManager: ReviewManager by lazy { ReviewManagerFactory.create(this) }
    private val fixtureId = mutableStateOf<String?>(null)

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize AppUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(this)
        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateUpdatedListener)
        }
        //Request exact alarm permission
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
        }

        setContent {
            PredComposeTheme {
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

        // Check for app updates when the activity is created
        // checkForAppUpdates()
        // requestReview()
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

    // In-app update logic
    private fun checkForAppUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val appUpdateOptions =
                AppUpdateOptions.newBuilder(updateType).setAllowAssetPackDeletion(true).build()
            val updateAvailability =
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isUpdateTypeAllowed = when (updateType) {
                AppUpdateType.FLEXIBLE -> appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                AppUpdateType.IMMEDIATE -> appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                else -> false
            }
            if (updateAvailability && isUpdateTypeAllowed) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    this,
                    appUpdateOptions,
                    updateRequestCode
                )
            }
        }
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // Notify the user that the update is ready to be installed
            showSnackbarForCompleteUpdate()
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

    override fun onResume() {
        super.onResume()
        if (updateType == AppUpdateType.IMMEDIATE) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    val appUpdateOptions =
                        AppUpdateOptions.newBuilder(updateType).setAllowAssetPackDeletion(true)
                            .build()
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        this,
                        appUpdateOptions,
                        updateRequestCode
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == updateRequestCode) {
            if (resultCode != RESULT_OK) {
                // Handle update failure
                Timber.d("Update failed")
            }
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

    private fun launchReviewFlow(reviewInfo: ReviewInfo) {
        reviewManager.launchReviewFlow(this, reviewInfo).addOnCompleteListener {
            // The flow has finished. The API does not indicate whether the user reviewed or not.
            Timber.d("Review flow completed.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        }
    }
}

@Composable
fun AppNavigation(fixtureId: String? = null) {
    val navController = rememberNavController()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val uiState by categoriesViewModel.uiState.collectAsState()
    val sharedViewModel: SharedViewModel = hiltViewModel()

    /* LaunchedEffect(fixtureId) {
         fixtureId?.let {
             navController.navigate(Routes.FixtureDetails.createRoute(fixtureId))
         }
     }*/

    NavHost(
        navController = navController,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        startDestination = Routes.Home.route
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
            if (uiState is UiState.Success) {
                val categories = (uiState as UiState.Success<List<Category>>).data
                ItemsListScreen(
                    navController = navController,
                    categoryId = categoryId,
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
            arguments = listOf(navArgument("teamId") { type = NavType.StringType },
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