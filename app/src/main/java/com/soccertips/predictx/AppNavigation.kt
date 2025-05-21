package com.soccertips.predictx

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.ui.categories.CategoriesScreen
import com.soccertips.predictx.ui.favorites.FavoritesScreen
import com.soccertips.predictx.ui.fixturedetails.FixtureDetailsScreen
import com.soccertips.predictx.ui.items.ItemsListScreen
import com.soccertips.predictx.ui.team.TeamScreen
import com.soccertips.predictx.viewmodel.CategoriesViewModel
import com.soccertips.predictx.viewmodel.SharedViewModel
import java.net.URLDecoder

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppNavigation(fixtureId: String? = null, forceNavigate: Boolean = false) {
    val navController = rememberNavController()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val uiState by categoriesViewModel.uiState.collectAsState()
    val sharedViewModel: SharedViewModel = hiltViewModel()
    val context = LocalContext.current

    // Check for pending navigation from notification
    val activity = LocalActivity.current as MainActivity
    val pendingFixtureId = remember {
        mutableStateOf(activity.sharedPrefs.getString("pending_navigation_fixture_id", null))
    }

    // If there's a pending navigation, use it; otherwise use the provided fixture ID
    val targetFixtureId = remember { mutableStateOf(pendingFixtureId.value ?: fixtureId) }

    // UI error handling
    if (uiState is UiState.Error) {
        val errorMessage = (uiState as UiState.Error).message
        AlertDialog(
            onDismissRequest = { categoriesViewModel.retryLoadCategories() },
            title = { Text("Information") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        // Open Telegram channel
                        val telegramUrl =
                            "https://t.me/+SlbFLBrgmVJiMQiG" // Replace with your actual channel
                        val intent = Intent(Intent.ACTION_VIEW, telegramUrl.toUri())
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                ) {
                    Text("Join Our Telegram Channel")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoriesViewModel.retryLoadCategories() }) {
                    Text("Retry")
                }
            }
        )
    }

    NavHost(
        navController = navController,
        enterTransition = { EnterTransition.Companion.None },
        exitTransition = { ExitTransition.Companion.None },
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
            arguments = listOf(navArgument("categoryId") { type = NavType.Companion.StringType }),
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
            val decodeUrl = URLDecoder.decode(categoryId, "UTF-8")
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
            arguments = listOf(navArgument("fixtureId") { type = NavType.Companion.StringType }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "app://com.soccertips.predictx/fixture/{fixtureId}"
                action = Intent.ACTION_VIEW
            }),
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
            val fixtureIdArgument = backStackEntry.arguments?.getString("fixtureId") ?: ""

            FixtureDetailsScreen(
                navController = navController,
                fixtureId = fixtureIdArgument,
            )
        }
        composable(
            Routes.TeamDetails.route,
            arguments = listOf(
                navArgument("teamId") { type = NavType.Companion.StringType },
                navArgument("leagueId") { type = NavType.Companion.StringType },
                navArgument("season") { type = NavType.Companion.StringType }),

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
    // Handle deep links after splash screen - improved to handle pending navigation and forced navigation
    val shouldForceNavigate = remember {
        mutableStateOf(
            forceNavigate || activity.sharedPrefs.getBoolean(
                "force_navigate_from_foreground",
                false
            )
        )
    }

    // Clear the force navigate flag after reading it
    if (activity.sharedPrefs.getBoolean("force_navigate_from_foreground", false)) {
        activity.sharedPrefs.edit {
            remove("force_navigate_from_foreground")
        }
    }

    // Handle immediate navigation when forced (from notification click in foreground)
    LaunchedEffect(shouldForceNavigate.value) {
        if (shouldForceNavigate.value && !targetFixtureId.value.isNullOrEmpty()) {
            val idToNavigate = targetFixtureId.value!!

            // Make sure we're not already on this screen
            if (navController.currentDestination?.route != Routes.FixtureDetails.createRoute(
                    idToNavigate
                )
            ) {
                navController.navigate(Routes.FixtureDetails.createRoute(idToNavigate)) {
                    // Save the current state so back navigation works
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }

                // Clear any pending navigation IDs to prevent duplicate navigation
                activity.sharedPrefs.edit {
                    remove("pending_navigation_fixture_id")
                }
                pendingFixtureId.value = null
            }

            // Reset the force flag after navigation is complete
            shouldForceNavigate.value = false
        }
    }

    // Original LaunchedEffect for handling navigation after splash
    LaunchedEffect(sharedViewModel.isSplashCompleted) {
        // Only do this if we're not already forcing navigation
        if (!shouldForceNavigate.value && sharedViewModel.isSplashCompleted) {
            val idToNavigate = targetFixtureId.value

            if (!idToNavigate.isNullOrEmpty() &&
                navController.currentDestination?.route != Routes.FixtureDetails.createRoute(
                    idToNavigate
                )
            ) {
                // Navigate to the fixture details with proper back stack handling
                navController.navigate(Routes.FixtureDetails.createRoute(idToNavigate)) {
                    // Save the current state so back navigation works
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }

                // Clear the pending navigation to prevent duplicate navigation
                if (pendingFixtureId.value != null) {
                    activity.sharedPrefs.edit {
                        remove("pending_navigation_fixture_id")
                    }
                    // Reset the stored value
                    pendingFixtureId.value = null
                }
            }
        }
    }
}