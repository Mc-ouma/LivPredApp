package com.soccertips.predictx

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.soccertips.predictx.ui.categories.CategoriesScreen
import com.soccertips.predictx.ui.favorites.FavoritesScreen
import com.soccertips.predictx.viewmodel.FavoritesViewModel
import com.soccertips.predictx.viewmodel.MainViewModel
import kotlinx.coroutines.launch

sealed class BottomNavScreens(
        open val route: String,
        @StringRes open val title: Int,
        open val selectedIcon: ImageVector,
        open val unselectedIcon: ImageVector,
        open val hasNews: Boolean,
        open val badgeCount: Int? = null
) {
    object Categories :
            BottomNavScreens(
                    "categories",
                    R.string.home_categories,
                    Icons.Filled.Home,
                    Icons.Outlined.Home,
                    false,
                    null
            )

    data class Favorite(
            override val route: String = "favorites",
            @StringRes override val title: Int = R.string.favorites,
            override val selectedIcon: ImageVector = Icons.Filled.Favorite,
            override val unselectedIcon: ImageVector = Icons.Filled.FavoriteBorder,
            override val hasNews: Boolean = false,
            override val badgeCount: Int? = null
    ) : BottomNavScreens(route, title, selectedIcon, unselectedIcon, hasNews, badgeCount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    rememberNavController()
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val favoriteCount by
            favoritesViewModel.favoriteCount.collectAsStateWithLifecycle(initialValue = 0)
    val mainViewModel: MainViewModel = hiltViewModel()
    val networkUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val items =
            listOf(
                    BottomNavScreens.Categories,
                    BottomNavScreens.Favorite(badgeCount = favoriteCount),
            )
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // State to track the last back press time
    var backPressState by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val pagerState = rememberPagerState { items.size }
    val scope = rememberCoroutineScope()

    // Handle back button press
    BackHandler(enabled = true) {
        if (selectedItemIndex == 1) { // If on FavoritesScreen
            selectedItemIndex = 0 // Navigate back to CategoriesScreen
        } else { // If on CategoriesScreen
            // Handle double back press to exit
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressState <= 2000) {
                android.os.Process.killProcess(android.os.Process.myPid())
            } else {
                backPressState = currentTime
                Toast.makeText(
                                context,
                                context.getString(R.string.press_back_again_to_exit),
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        }
    }

    LaunchedEffect(networkUiState) {
        if (networkUiState is MainViewModel.UiState.NetworkError) {
            val result =
                    snackbarHostState.showSnackbar(
                            message = context.getString(R.string.no_internet_connection),
                            actionLabel = context.getString(R.string.retry),
                            duration = SnackbarDuration.Long
                    )
            if (result == SnackbarResult.ActionPerformed) {
                mainViewModel.retryNetworkOperation()
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page -> selectedItemIndex = page }
    }

    Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(16.dp))
            },
            topBar = {
                HomeTopBar(selectedItemIndex = selectedItemIndex, scrollBehavior = scrollBehavior)
            },
            bottomBar = {
                AnimatedNavigationBar(
                        selectedItem = pagerState.currentPage,
                        onItemSelected = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        buttons =
                                items.map { item ->
                                    ButtonData(
                                            text = stringResource(item.title),
                                            icon = item.selectedIcon,
                                            hasNews = item.hasNews,
                                            badgeCount = item.badgeCount
                                    )
                                },
                        barColor = MaterialTheme.colorScheme.primaryContainer,
                        circleColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                /*NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            label = { Text(text = stringResource(item.title)) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (item.badgeCount != null) {
                                            Badge(
                                                containerColor =
                                                    MaterialTheme.colorScheme
                                                        .primary
                                            ) { Text(text = item.badgeCount.toString()) }
                                        } else if (item.hasNews) {
                                            Badge(
                                                containerColor =
                                                    MaterialTheme.colorScheme
                                                        .primary
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector =
                                            if (pagerState.currentPage == index) {
                                                item.selectedIcon
                                            } else item.unselectedIcon,
                                        contentDescription = stringResource(item.title)
                                    )
                                }
                            }
                        )
                    }
                }*/
            },
            content = { padding ->
                HorizontalPager(state = pagerState, modifier = Modifier.padding(padding)) { page ->
                    when (page) {
                        0 -> CategoriesScreen(navController = navController)
                        1 -> FavoritesScreen(navController = navController)
                    }
                }
            }
    )
}

@Composable
private fun Circle(
        modifier: Modifier = Modifier,
        color: Color = MaterialTheme.colorScheme.onPrimary,
        radius: Dp,
        button: ButtonData,
        iconColor: Color,
) {
    Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(radius * 2).clip(CircleShape).background(color),
    ) {
        BadgedBox(
                badge = {
                    if (button.badgeCount != null) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text(text = button.badgeCount.toString())
                        }
                    } else if (button.hasNews) {
                        Badge(containerColor = MaterialTheme.colorScheme.error)
                    }
                }
        ) {
            AnimatedContent(
                    targetState = button.icon,
                    label = "Bottom bar circle icon",
            ) { targetIcon -> Icon(targetIcon, button.text, tint = iconColor) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
        modifier: Modifier = Modifier,
        selectedItemIndex: Int,
        scrollBehavior: TopAppBarScrollBehavior
) {
    CenterAlignedTopAppBar(
            title = {
                if (selectedItemIndex == 0) Text(stringResource(R.string.categories))
                else Text(stringResource(R.string.favorites))
            },
            modifier = modifier,
            navigationIcon = {},
            actions = { Menu() },
            scrollBehavior = scrollBehavior,
    )
}

data class ButtonData(
        val text: String,
        val icon: ImageVector,
        val hasNews: Boolean = false,
        val badgeCount: Int? = null
)

@Composable
fun AnimatedNavigationBar(
        selectedItem: Int,
        onItemSelected: (Int) -> Unit,
        buttons: List<ButtonData>,
        barColor: Color,
        circleColor: Color,
        selectedColor: Color,
        unselectedColor: Color,
) {
    val circleRadius = 26.dp

    var barSize by remember { mutableStateOf(IntSize(0, 0)) }
    // first item's center offset for Arrangement.SpaceAround
    val offsetStep = remember(barSize) { barSize.width.toFloat() / (buttons.size * 2) }
    val offset = remember(selectedItem, offsetStep) { offsetStep + selectedItem * 2 * offsetStep }
    val circleRadiusPx = LocalDensity.current.run { circleRadius.toPx().toInt() }
    val offsetTransition = updateTransition(offset, "offset transition")
    val animation = spring<Float>(dampingRatio = 0.5f, stiffness = Spring.StiffnessVeryLow)
    val cutoutOffset by
            offsetTransition.animateFloat(
                    transitionSpec = {
                        if (this.initialState == 0f) {
                            snap()
                        } else {
                            animation
                        }
                    },
                    label = "cutout offset"
            ) { it }
    val circleOffset by
            offsetTransition.animateIntOffset(
                    transitionSpec = {
                        if (this.initialState == 0f) {
                            snap()
                        } else {
                            spring(animation.dampingRatio, animation.stiffness)
                        }
                    },
                    label = "circle offset"
            ) { IntOffset(it.toInt() - circleRadiusPx, -circleRadiusPx) }
    val barShape =
            remember(cutoutOffset) {
                BarShape(
                        offset = cutoutOffset,
                        circleRadius = circleRadius,
                        cornerRadius = 25.dp,
                )
            }

    Box(modifier = Modifier.fillMaxWidth().graphicsLayer { clip = false }) {
        Row(
                modifier =
                        Modifier.onPlaced { barSize = it.size }.fillMaxWidth().drawBehind {
                            // Draw the bar shape directly behind the row
                            val outline = barShape.createOutline(size, layoutDirection, this)
                            if (outline is Outline.Generic) {
                                drawPath(path = outline.path, color = barColor)
                            }
                        },
                horizontalArrangement = Arrangement.SpaceAround,
        ) {
            buttons.forEachIndexed { index, button ->
                val isSelected = index == selectedItem
                NavigationBarItem(
                        selected = isSelected,
                        onClick = { onItemSelected(index) },
                        icon = {
                            val iconAlpha by
                                    animateFloatAsState(
                                            targetValue = if (isSelected) 0f else 1f,
                                            label = "Navbar item icon"
                                    )
                            // Only show badge when not selected (floating circle will show it when
                            // selected)
                            if (!isSelected) {
                                BadgedBox(
                                        badge = {
                                            if (button.badgeCount != null) {
                                                Badge(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.primary
                                                ) { Text(text = button.badgeCount.toString()) }
                                            } else if (button.hasNews) {
                                                Badge(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                ) {
                                    Icon(
                                            imageVector = button.icon,
                                            contentDescription = button.text,
                                            modifier = Modifier.alpha(iconAlpha)
                                    )
                                }
                            } else {
                                Icon(
                                        imageVector = button.icon,
                                        contentDescription = button.text,
                                        modifier = Modifier.alpha(iconAlpha)
                                )
                            }
                        },
                        label = { Text(button.text) },
                        colors =
                                NavigationBarItemDefaults.colors(
                                        selectedIconColor = selectedColor,
                                        selectedTextColor = selectedColor,
                                        unselectedIconColor = unselectedColor,
                                        unselectedTextColor = unselectedColor,
                                        indicatorColor = Color.Transparent,
                                )
                )
            }
        }
        // Circle on top, completely independent of the row
        Circle(
                modifier = Modifier.offset { circleOffset }.zIndex(2f),
                color = circleColor,
                radius = circleRadius,
                button = buttons[selectedItem],
                iconColor = selectedColor,
        )
    }
}

private class BarShape(
        private val offset: Float,
        private val circleRadius: Dp,
        private val cornerRadius: Dp,
        private val circleGap: Dp = 5.dp,
) : Shape {

    override fun createOutline(
            size: androidx.compose.ui.geometry.Size,
            layoutDirection: LayoutDirection,
            density: Density
    ): Outline {
        return Outline.Generic(getPath(size, density))
    }

    private fun getPath(size: androidx.compose.ui.geometry.Size, density: Density): Path {
        val cutoutCenterX = offset
        val cutoutRadius = density.run { (circleRadius + circleGap).toPx() }
        val cornerRadiusPx = density.run { cornerRadius.toPx() }
        val cornerDiameter = cornerRadiusPx * 2
        return Path().apply {
            val cutoutEdgeOffset = cutoutRadius * 1.5f
            val cutoutLeftX = cutoutCenterX - cutoutEdgeOffset
            val cutoutRightX = cutoutCenterX + cutoutEdgeOffset

            // bottom left
            moveTo(x = 0F, y = size.height)
            // top left
            if (cutoutLeftX > 0) {
                val realLeftCornerDiameter =
                        if (cutoutLeftX >= cornerRadiusPx) {
                            // there is a space between rounded corner and cutout
                            cornerDiameter
                        } else {
                            // rounded corner and cutout overlap
                            cutoutLeftX * 2
                        }
                arcTo(
                        rect =
                                androidx.compose.ui.geometry.Rect(
                                        left = 0f,
                                        top = 0f,
                                        right = realLeftCornerDiameter,
                                        bottom = realLeftCornerDiameter
                                ),
                        startAngleDegrees = 180.0f,
                        sweepAngleDegrees = 90.0f,
                        forceMoveTo = false
                )
            }
            lineTo(cutoutLeftX, 0f)
            // cutout
            cubicTo(
                    x1 = cutoutCenterX - cutoutRadius,
                    y1 = 0f,
                    x2 = cutoutCenterX - cutoutRadius,
                    y2 = cutoutRadius,
                    x3 = cutoutCenterX,
                    y3 = cutoutRadius,
            )
            cubicTo(
                    x1 = cutoutCenterX + cutoutRadius,
                    y1 = cutoutRadius,
                    x2 = cutoutCenterX + cutoutRadius,
                    y2 = 0f,
                    x3 = cutoutRightX,
                    y3 = 0f,
            )
            // top right
            if (cutoutRightX < size.width) {
                val realRightCornerDiameter =
                        if (cutoutRightX <= size.width - cornerRadiusPx) {
                            cornerDiameter
                        } else {
                            (size.width - cutoutRightX) * 2
                        }
                arcTo(
                        rect =
                                androidx.compose.ui.geometry.Rect(
                                        left = size.width - realRightCornerDiameter,
                                        top = 0f,
                                        right = size.width,
                                        bottom = realRightCornerDiameter
                                ),
                        startAngleDegrees = -90.0f,
                        sweepAngleDegrees = 90.0f,
                        forceMoveTo = false
                )
            }
            // bottom right
            lineTo(x = size.width, y = size.height)
            close()
        }
    }
}
