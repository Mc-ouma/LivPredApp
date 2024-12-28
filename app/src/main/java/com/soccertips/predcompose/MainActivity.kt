package com.soccertips.predcompose

import android.os.Build
import android.os.Bundle
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() { // Annotate with Hilt
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PredComposeTheme {
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface) {
                    AppNavigation()
                }

            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val uiState by categoriesViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        startDestination = Routes.Home.route
    ) {
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
}
