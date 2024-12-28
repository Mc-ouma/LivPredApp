package com.soccertips.predcompose.ui.favorites

import android.annotation.SuppressLint
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
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import com.soccertips.predcompose.navigation.Routes
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.ui.components.ErrorMessage
import com.soccertips.predcompose.ui.components.LoadingIndicator
import com.soccertips.predcompose.ui.items.LeagueInfo
import com.soccertips.predcompose.ui.items.TeamDetails
import com.soccertips.predcompose.ui.items.TeamInfo
import com.soccertips.predcompose.ui.theme.LocalCardColors
import com.soccertips.predcompose.ui.theme.LocalCardElevation
import com.soccertips.predcompose.ui.theme.PredComposeTheme
import com.soccertips.predcompose.viewmodel.FavoritesViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


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
                Text(
                    text = "No favorite items found.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium,
                )
            } else {
                FavoritesScreen(
                    navController = navController,
                    viewModel = viewModel,
                    favoriteItems = favoriteItems,
                )
            }
        }

        else -> { /* No Action */
        }
    }
}

@Composable
fun FavoritesScreen(
    navController: NavController,
    viewModel: FavoritesViewModel = hiltViewModel(),
    favoriteItems: List<FavoriteItem>
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(favoriteItems) { item ->
            FavoriteItemCard(
                item = item,
                onFavoriteClick = { viewModel.removeFromFavorites(item) },
                isFavorite = true,
                viewModel = viewModel,
                onClick = {
                    navController.navigate(
                        Routes.FixtureDetails.createRoute(
                            item.fixtureId
                        )
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
    viewModel: FavoritesViewModel,
    onClick: () -> Unit
) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    val teamHomeDetails = TeamDetails(item.hLogoPath, item.homeTeam)
    val teamAwayDetails = TeamDetails(item.aLogoPath, item.awayTeam)
    val formattedDate = viewModel.formatRelativeDate(item.mDate)
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

                IconButton(onClick = { onFavoriteClick(item) }) {
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
                        //tint = item.color,
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

