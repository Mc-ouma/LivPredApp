package com.soccertips.predcompose.ui.favorites

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
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
import com.soccertips.predcompose.ui.components.MyCustomIndicator
import com.soccertips.predcompose.ui.fixturedetails.EmptyScreen
import com.soccertips.predcompose.ui.items.LeagueInfo
import com.soccertips.predcompose.ui.items.TeamDetails
import com.soccertips.predcompose.ui.items.TeamInfo
import com.soccertips.predcompose.ui.theme.LocalCardColors
import com.soccertips.predcompose.ui.theme.LocalCardElevation
import com.soccertips.predcompose.viewmodel.FavoritesViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarState = rememberSnackBarState()
    var isRefreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()

    // Observe SnackbarState changes and show Snackbar
    LaunchedEffect(snackbarState.message) {
        if (snackbarState.message.isNotEmpty()) {
            val result = snackbarHostState.showSnackbar(
                message = snackbarState.message,
                actionLabel = snackbarState.actionLabel,
            )
            if (result == SnackbarResult.ActionPerformed) {
                snackbarState.onActionPerformed?.invoke()
            }
            // Reset SnackbarState after showing
            snackbarState.message = ""
            snackbarState.actionLabel = ""
            snackbarState.onActionPerformed = null
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (uiState) {
            is UiState.Loading -> {
                LoadingIndicator()
            }

            is UiState.Error -> {
                ErrorMessage(
                    message = (uiState as UiState.Error).message,
                    onRetry = { viewModel.loadFavorites() },
                )
            }

            is UiState.Success -> {
                val favoriteItems = (uiState as UiState.Success<List<FavoriteItem>>).data
                if (favoriteItems.isEmpty()) {
                    EmptyScreen(paddingValues = PaddingValues(16.dp), message = "No favorite items")
                } else {
                    FavoritesScreen(
                        navController = navController,
                        viewModel = viewModel,
                        favoriteItems = favoriteItems,
                        snackbarState = snackbarState,
                        isRefreshing = isRefreshing,
                        state = state,
                        onRefresh = {
                            isRefreshing = true
                            viewModel.loadFavorites()
                            isRefreshing = false
                        }
                    )
                }
            }

            is UiState.Empty -> {
                EmptyScreen(paddingValues = PaddingValues(16.dp))
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = hiltViewModel(),
    favoriteItems: List<FavoriteItem>,
    snackbarState: SnackbarState,
    isRefreshing: Boolean,
    state: PullToRefreshState,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        indicator = {
            MyCustomIndicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(favoriteItems) { item ->
                FavoriteItemCard(
                    item = item,
                    onFavoriteClick = { favoriteItem ->
                        viewModel.removeFromFavorites(favoriteItem) // Remove the item
                        snackbarState.message = "Removed from Favorites"
                        snackbarState.actionLabel = "Undo"
                        snackbarState.onActionPerformed = {
                            viewModel.restoreFavorites(favoriteItem) // Restore the item
                        }
                    },
                    isFavorite = true,
                    onClick = {
                        navController.navigate(
                            Routes.FixtureDetails.createRoute(item.fixtureId)
                        )
                    },
                    snackbarState = snackbarState,
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FavoriteItemCard(
    item: FavoriteItem,
    onFavoriteClick: (FavoriteItem) -> Unit = {},
    isFavorite: Boolean,
    onClick: () -> Unit,
    snackbarState: SnackbarState,
) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
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
            ),
        colors = cardColors,
        elevation = cardElevation
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1f))

                LeagueInfo(
                    leagueLogo = item.leagueLogo,
                    leagueName = item.league?.split(",")?.firstOrNull()
                )
                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = {
                    onFavoriteClick(item) // Remove the item
                    snackbarState.message = "Removed from Favorites"
                    snackbarState.actionLabel = "Undo"
                    snackbarState.onActionPerformed = {
                        onFavoriteClick(item) // Restore the item when "Undo" is clicked
                    }
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

@Composable
fun rememberSnackBarState(): SnackbarState {
    return remember { SnackbarState() }
}

class SnackbarState {
    var message by mutableStateOf("")
    var actionLabel by mutableStateOf("")
    var onActionPerformed by mutableStateOf<(() -> Unit)?>(null)
}

