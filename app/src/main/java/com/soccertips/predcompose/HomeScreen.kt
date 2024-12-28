package com.soccertips.predcompose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soccertips.predcompose.ui.categories.CategoriesScreen
import com.soccertips.predcompose.ui.favorites.FavoritesScreen
import com.soccertips.predcompose.viewmodel.FavoritesViewModel

sealed class BottomNavScreens(
    open val route: String,
    open val title: String,
    open val selectedIcon: ImageVector,
    open val unselectedIcon: ImageVector,
    open val hasNews: Boolean,
    open val badgeCount: Int? = null
) {
    object Categories :
        BottomNavScreens("categories", "Home", Icons.Filled.Home, Icons.Outlined.Home, false, null)

    data class Favorite(
        override val route: String = "favorites",
        override val title: String = "Saved",
        override val selectedIcon: ImageVector = Icons.Filled.Favorite,
        override val unselectedIcon: ImageVector = Icons.Filled.FavoriteBorder,
        override val hasNews: Boolean = false,
        override val badgeCount: Int? = null
    ) : BottomNavScreens(route, title, selectedIcon, unselectedIcon, hasNews, badgeCount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val homeNavController = rememberNavController()
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val favoriteCount by favoritesViewModel.favoriteCount.collectAsStateWithLifecycle()
    val items = listOf(
        BottomNavScreens.Categories,
        BottomNavScreens.Favorite(badgeCount = favoriteCount),
    )
    var selectedItemIndex by rememberSaveable {
        mutableIntStateOf(0)
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HomeTopBar(
                selectedItemIndex = selectedItemIndex,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItemIndex == index,
                        onClick = {
                            selectedItemIndex = index
                            homeNavController.navigate(item.route)
                        },
                        label = {
                            Text(text = item.title)
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (item.badgeCount != null) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(text = item.badgeCount.toString())
                                        }
                                    } else if (item.hasNews) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (index == selectedItemIndex) {
                                        item.selectedIcon
                                    } else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            }
                        }
                    )
                }
            }
        },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                NavHost(
                    navController = homeNavController,
                    startDestination = BottomNavScreens.Categories.route
                ) {
                    composable(BottomNavScreens.Categories.route) {
                        CategoriesScreen(navController = navController)
                    }
                    composable(BottomNavScreens.Favorite().route) {
                        FavoritesScreen(navController = navController)
                    }
                }
            }
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    selectedItemIndex: Int,
    scrollBehavior: TopAppBarScrollBehavior
) {
    CenterAlignedTopAppBar(
        title = { if (selectedItemIndex == 0) Text("Categories") else Text("Favorites") },

        modifier = modifier,
        navigationIcon = {

        },
        actions = {
            Menu()

        },
        scrollBehavior = scrollBehavior,

        )

}

