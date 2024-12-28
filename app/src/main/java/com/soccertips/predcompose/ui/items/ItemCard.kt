package com.soccertips.predcompose.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.soccertips.predcompose.data.model.ServerResponse
import com.soccertips.predcompose.ui.theme.LocalCardColors
import com.soccertips.predcompose.ui.theme.LocalCardElevation
import com.soccertips.predcompose.viewmodel.ItemsListViewModel


@Composable
fun ItemCard(
    item: ServerResponse,
    onClick: () -> Unit,
    onFavoriteClick: (ServerResponse) -> Unit,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    viewModel: ItemsListViewModel
) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    val teamHomeDetails = TeamDetails(item.hLogoPath, item.homeTeam)
    val teamAwayDetails = TeamDetails(item.aLogoPath, item.awayTeam)
    var isFavorite by remember { mutableStateOf(false) }

    LaunchedEffect (isFavorite) {
        isFavorite = viewModel.isFavorite(item)
    }
    Card(
        modifier = modifier
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
            modifier = Modifier
                .padding(16.dp),
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
                    onFavoriteClick(item)
                    viewModel.toggleFavorite(item)
                    isFavorite = !isFavorite
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
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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
                    item.mTime?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
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
                item.pick?.let {
                    Text(
                        text = "Pick $it",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (item.color != Color.Unspecified) {
                    Icon(
                        imageVector = Icons.Outlined.FiberManualRecord,
                        contentDescription = "Status Indicator",
                        tint = item.color,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text(
                    text = item.mStatus ?: "TBD",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )

            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun LeagueInfo(leagueLogo: String?, leagueName: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {

        GlideImage(
            model = leagueLogo,
            contentDescription = "League Logo",
            modifier = Modifier
                .height(24.dp)
                .width(24.dp),
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,

            )
        Text(
            text = leagueName ?: "Unknown League",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(start = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

data class TeamDetails(
    val teamLogo: String?,
    val teamName: String?
)

// Reusable Composable for Team Info
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun TeamInfo(
    teamDetails: TeamDetails,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        GlideImage(
            model = teamDetails.teamLogo,
            contentDescription = "Team Logo",
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .height(24.dp)
                .width(24.dp),
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,

            )
        teamDetails.teamName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
