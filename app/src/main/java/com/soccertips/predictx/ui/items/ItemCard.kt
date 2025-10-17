package com.soccertips.predictx.ui.items

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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.ServerResponse
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import com.soccertips.predictx.viewmodel.ItemsListViewModel

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
    LaunchedEffect(item.fixtureId) { isFavorite = viewModel.isFavorite(item) }

    Card(
            modifier =
                    modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable(
                                    onClick = onClick,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple()
                            ),
            colors = cardColors,
            elevation = cardElevation,
            shape = MaterialTheme.shapes.medium
    ) {
        Column(
                modifier = Modifier.padding(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header with league info and favorite button
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

            // Main content - teams and score
            TeamsRow(
                    homeTeam = TeamDetails(item.hLogoPath, item.homeTeam),
                    awayTeam = TeamDetails(item.aLogoPath, item.awayTeam),
                    matchTime = item.mTime ?: "TBD",
                    score = item.result,
                    statusColor = item.color
            )

            // Footer with pick only
            MatchStatusRow(pick = item.pick)
        }
    }
}

@Composable
fun MatchHeader(
        league: String,
        leagueLogo: String?,
        date: String,
        isFavorite: Boolean,
        onFavoriteClick: () -> Unit
) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.small
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // League info with logo
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                )
            }

            // Favorite button
            IconButton(onClick = onFavoriteClick, modifier = Modifier.size(32.dp)) {
                Icon(
                        imageVector =
                                if (isFavorite) Icons.Filled.Favorite
                                else Icons.Outlined.FavoriteBorder,
                        contentDescription =
                                if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint =
                                if (isFavorite) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TeamsRow(
        homeTeam: TeamDetails,
        awayTeam: TeamDetails,
        matchTime: String,
        score: String?,
        statusColor: Color = Color.Unspecified
) {
    val displayScore =
            score?.let { rawScore ->
                val parts = rawScore.split(":")
                parts
                        .find { part ->
                            val trimmed = part.trim()
                            trimmed.matches(Regex(".*\\d+\\s*-\\s*\\d+.*"))
                        }
                        ?.let { scorePart ->
                            val scoreMatch = Regex("(\\d+\\s*-\\s*\\d+)").find(scorePart.trim())
                            scoreMatch?.value
                        }
            }
                    ?: "-"

    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Home Team
        EnhancedTeamInfo(
                teamDetails = homeTeam,
                alignment = Alignment.Start,
                modifier = Modifier.weight(1f)
        )

        // Center - Score/Time with better styling
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp).weight(0.7f)
        ) {
            if (!score.isNullOrBlank() && score != "Unknown" && displayScore != "-") {
                Text(
                        text = displayScore,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color =
                                when (statusColor) {
                                    Color.Green -> MaterialTheme.colorScheme.primary
                                    Color.Red -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                        textAlign = TextAlign.Center
                )
                Text(
                        text = "FT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                )
            } else {
                Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                            text = matchTime,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Away Team
        EnhancedTeamInfo(
                teamDetails = awayTeam,
                alignment = Alignment.End,
                modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun EnhancedTeamInfo(
        teamDetails: TeamDetails,
        alignment: Alignment.Horizontal,
        modifier: Modifier = Modifier
) {
    Column(
            horizontalAlignment = alignment,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
    ) {
        AsyncImage(
                model = teamDetails.teamLogo,
                contentDescription = "${teamDetails.teamName} logo",
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.placeholder)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
                text = teamDetails.teamName ?: "Unknown",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (alignment == Alignment.Start) TextAlign.Start else TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MatchStatusRow(pick: String?) {
    Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = "Pick: ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
            )
            Text(
                    text = pick ?: "TBD",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

data class TeamDetails(val teamLogo: String?, val teamName: String?)

// Reusable Composable for Team Info

@Composable
fun TeamInfo(teamDetails: TeamDetails, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Image(
                rememberAsyncImagePainter(teamDetails.teamLogo),
                contentDescription = "Team Logo",
                modifier = Modifier.padding(horizontal = 8.dp).height(24.dp).width(24.dp),
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
