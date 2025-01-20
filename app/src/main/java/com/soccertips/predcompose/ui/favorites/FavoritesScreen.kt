package com.soccertips.predcompose.ui.favorites

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import com.soccertips.predcompose.navigation.Routes
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.ui.components.DateUtils
import com.soccertips.predcompose.ui.components.ErrorMessage
import com.soccertips.predcompose.ui.components.LoadingIndicator
import com.soccertips.predcompose.ui.fixturedetails.EmptyScreen
import com.soccertips.predcompose.ui.items.LeagueInfo
import com.soccertips.predcompose.ui.items.TeamDetails
import com.soccertips.predcompose.ui.items.TeamInfo
import com.soccertips.predcompose.ui.theme.LocalCardColors
import com.soccertips.predcompose.viewmodel.FavoritesViewModel


@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarData by viewModel.snackbarFlow.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()


    // Observe SnackbarState changes and show Snackbar
    LaunchedEffect(snackbarData) {
        snackbarData?.let {data ->
            val result = snackbarHostState.showSnackbar(
                message = data.message,
                actionLabel = data.actionLabel,
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    data.onActionPerformed?.invoke()
                }

                SnackbarResult.Dismissed -> {
                    // Do nothing
                }
            }
            viewModel.resetSnackbar()
        }

    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true; viewModel.loadFavorites(); isRefreshing = false
            }, // Start refresh gesture
            state = state,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    if (targetState is UiState.Success) {
                        fadeIn(animationSpec = tween(durationMillis = 300)) +
                                scaleIn(
                                    initialScale = 0.9f,
                                    animationSpec = tween(durationMillis = 300)
                                ) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 200))
                    } else {
                        fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 200))
                    }
                }
            ) { uiState ->
                when (uiState) {
                    is UiState.Loading -> {
                        LoadingIndicator()
                    }

                    is UiState.Error -> {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = { viewModel.loadFavorites() },
                        )
                    }

                    is UiState.Success -> {
                        val favoriteItems = uiState.data
                        val listState = rememberLazyListState()
                        if (favoriteItems.isEmpty()) {
                            EmptyScreen(
                                paddingValues = PaddingValues(16.dp),
                                message = "No favorite items"
                            )
                        } else {
                            FavoritesScreen(
                                navController = navController,
                                viewModel = viewModel,
                                favoriteItems = favoriteItems,
                                listState = listState
                            )
                        }
                    }

                    is UiState.Empty ->
                        EmptyScreen(paddingValues = PaddingValues(16.dp))

                    else -> Unit
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = hiltViewModel(),
    favoriteItems: List<FavoriteItem>,
    listState: LazyListState,
) {

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = favoriteItems, key = { item -> item.fixtureId }
        ) { item ->
            var isItemVisible by remember { mutableStateOf(true) }
            AnimatedVisibility(
                visible = isItemVisible,
                enter = fadeIn() + slideInVertically(),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300)),
                content = {
                    FavoriteItemCard(
                        item = item,
                        onFavoriteClick = { favoriteItem ->
                            viewModel.removeFromFavorites(favoriteItem) // Remove the item
                            viewModel.showSnackbar(
                                message = "Removed from Favorites",
                                actionLabel = "Undo",
                                onActionPerformed = {
                                    viewModel.restoreFavorites(favoriteItem)
                                    isItemVisible = true
                                }
                            )
                            isItemVisible = false
                        },
                        isFavorite = true,
                        onClick = {
                            navController.navigate(
                                Routes.FixtureDetails.createRoute(item.fixtureId)
                            )
                        },
                    )
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}

@Composable
fun FavoriteItemCard(
    item: FavoriteItem,
    onFavoriteClick: (FavoriteItem) -> Unit = {},
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    val cardColors = LocalCardColors.current
    val teamHomeDetails = TeamDetails(item.hLogoPath, item.homeTeam)
    val teamAwayDetails = TeamDetails(item.aLogoPath, item.awayTeam)
    val formattedDate = DateUtils.formatRelativeDate(item.mDate)


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
            .padding(4.dp),

        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1f))

                LeagueInfo(
                    leagueLogo = item.leagueLogo,
                    leagueName = item.league?.split(",")?.firstOrNull()
                )
                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = {
                    onFavoriteClick(item)
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.align(Alignment.Top),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamInfo(
                    teamDetails = teamHomeDetails,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = item.mTime ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                TeamInfo(
                    teamDetails = teamAwayDetails,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pick ${item.pick}",
                    style = MaterialTheme.typography.labelMedium,
                )
                if (item.color != Color.Unspecified.toArgb()) {
                    Icon(
                        imageVector = Icons.Outlined.FiberManualRecord,
                        contentDescription = "Status Indicator",
                        tint = Color(item.color),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text(
                    text = item.mStatus ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}


