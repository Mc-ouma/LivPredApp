package com.soccertips.predcompose.ui.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predcompose.R
import com.soccertips.predcompose.data.model.ServerResponse
import com.soccertips.predcompose.ui.theme.LocalCardColors
import com.soccertips.predcompose.ui.theme.LocalCardElevation
import com.soccertips.predcompose.viewmodel.ItemsListViewModel


/*@Composable
fun ItemCard(
    item: ServerResponse,
    onClick: () -> Unit,
    onFavoriteClick: (ServerResponse) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemsListViewModel
) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    val teamHomeDetails = TeamDetails(item.hLogoPath, item.homeTeam)
    val teamAwayDetails = TeamDetails(item.aLogoPath, item.awayTeam)
    var isFavorite by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

    LaunchedEffect(isFavorite) {
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
                    showToast = true
                    toastMessage =
                        if (isFavorite) "Added to Favorites" else "Removed from Favorites"
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
}*/
@Composable
fun ItemCard(
    item: ServerResponse,
    onClick: () -> Unit,
    onFavoriteClick: (ServerResponse) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemsListViewModel
) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    var isFavorite by remember { mutableStateOf(false) }


    // Use key parameter to prevent unnecessary recompositions
    LaunchedEffect(item.fixtureId) {
        isFavorite = viewModel.isFavorite(item)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ),
        colors = cardColors,
        elevation = cardElevation
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MatchHeader(
                league = item.league?.split(",")?.firstOrNull() ?: "Unknown",
                leagueLogo = item.leagueLogo,
                date = item.mDate ?: "Unknown",
                isFavorite = isFavorite,
                onFavoriteClick = {
                    isFavorite = !isFavorite
                    onFavoriteClick(item)
                    viewModel.toggleFavorite(item)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TeamsRow(
                homeTeam = TeamDetails(item.hLogoPath, item.homeTeam),
                awayTeam = TeamDetails(item.aLogoPath, item.awayTeam),
                matchTime = item.mTime ?: "TBD",
                score = item.result
            )

            Spacer(modifier = Modifier.height(8.dp))

            MatchStatusRow(
                pick = item.pick,
                status = item.mStatus,
                statusColor = item.color
            )
        }
    }
}

@Composable
private fun MatchHeader(
    league: String,
    leagueLogo: String?,
    date: String,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    var favoriteState by remember { mutableStateOf(isFavorite) }
    Column(modifier = Modifier.fillMaxWidth()) {
        // League info in its own row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            AsyncImage(
                model = leagueLogo,
                contentDescription = "League Logo",
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.placeholder)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = league,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Date and favorite in a separate row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = {
                    favoriteState = !favoriteState
                    onFavoriteClick()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.6f
                    )
                )
            }
        }
    }
}

@Composable
private fun TeamsRow(
    homeTeam: TeamDetails,
    awayTeam: TeamDetails,
    matchTime: String,
    score: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EnhancedTeamInfo(
            teamDetails = homeTeam,
            alignment = Alignment.Start,
            modifier = Modifier.weight(2f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = matchTime,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (!score.isNullOrBlank() && score != "Unknown") {
                Text(
                    text = score,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        EnhancedTeamInfo(
            teamDetails = awayTeam,
            alignment = Alignment.End,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun EnhancedTeamInfo(
    teamDetails: TeamDetails,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignment == Alignment.Start) Arrangement.Start else Arrangement.End,
        modifier = modifier
    ) {
        if (alignment == Alignment.End) {
            Text(
                text = teamDetails.teamName ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(
                model = teamDetails.teamLogo,
                contentDescription = "${teamDetails.teamName} logo",
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            AsyncImage(
                model = teamDetails.teamLogo,
                contentDescription = "${teamDetails.teamName} logo",
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = teamDetails.teamName ?: "Unknown",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MatchStatusRow(
    pick: String?,
    status: String?,
    statusColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pick: ${pick ?: "TBD"}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (statusColor != Color.Unspecified) {
                Icon(
                    imageVector = Icons.Outlined.FiberManualRecord,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = status ?: "TBD",
                style = MaterialTheme.typography.labelMedium,
                color = when (statusColor) {
                    Color.Green -> MaterialTheme.colorScheme.primary
                    Color.Red -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                }
            )
        }
    }
}


@Composable
fun LeagueInfo(leagueLogo: String?, leagueName: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {

        Image(
            rememberAsyncImagePainter(leagueLogo),
            contentDescription = "League Logo",
            modifier = Modifier
                .height(24.dp)
                .width(24.dp),

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

@Composable
fun TeamInfo(
    teamDetails: TeamDetails,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Image(
            rememberAsyncImagePainter(teamDetails.teamLogo),
            contentDescription = "Team Logo",
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .height(24.dp)
                .width(24.dp),

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

@Preview
@Composable
private fun MatchHeaderPreview() {
    MatchHeader(
        league = "Premier League",
        leagueLogo = "https://cdn.soccertips.com/assets/leagues/england-premier-league.png",
        date = "2022-12-31",
        isFavorite = false,
        onFavoriteClick = {}
    )

}

