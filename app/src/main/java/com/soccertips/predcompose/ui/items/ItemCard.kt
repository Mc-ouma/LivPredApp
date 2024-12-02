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
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.soccertips.predcompose.model.ServerResponse


@Composable
fun ItemCard(
    item: ServerResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LeagueInfo(
                leagueLogo = item.leagueLogo,
                leagueName = item.league?.split(",")?.firstOrNull()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamInfo(teamLogo = item.hLogoPath, teamName = item.homeTeam, item = item,modifier = Modifier.weight(1f))

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

                TeamInfo(teamLogo = item.aLogoPath, teamName = item.awayTeam, item = item,modifier = Modifier.weight(1f))
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

// Reusable Composable for Team Info
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun TeamInfo(teamLogo: String?, teamName: String?, item: ServerResponse,modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,modifier = modifier) {
        GlideImage(
            model = teamLogo,
            contentDescription = if (teamLogo == item.hLogoPath) "Home Team Logo" else "Away Team Logo",
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .height(24.dp)
                .width(24.dp),
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,

            )
        teamName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Placeholder Drawable

@Preview
@Composable
private fun ItemCardPreview() {
    ItemCard(
        item =
        ServerResponse(
            homeTeam = "Chelsea Manchester United",
            awayTeam = "Arsenal",
            mTime = "12:30",
            mDate = "12/12/2023",
            league = "Premier League",
            country = "England",
            outcome = "win",
            mStatus = "FT",
            pick = "0",
            color = Color.Green,
        ),
        onClick = {},
    )
}

@Preview
@Composable
private fun ItemCardPreviewLose() {
    ItemCard(
        item =
        ServerResponse(
            homeTeam = "Chelsea",
            awayTeam = "Arsenal",
            mTime = "12:30",
            mDate = "2023-12-12",
            league = "Premier League",
            outcome = "lose",
            mStatus = "FT",
            pick = "1",
            color = Color.Red,
        ),
        onClick = {},
    )
}

@Preview
@Composable
private fun ItemCardPreviewNoOutcome() {
    ItemCard(
        item =
        ServerResponse(
            homeTeam = "Chelsea",
            awayTeam = "Arsenal",
            mTime = "12:30",
            mDate = "2023-12-12",
            league = "Premier League",
            outcome = "",
            mStatus = "FT",
            pick = "1",
            color = Color.Unspecified,
        ),
        onClick = {},
    )
}
