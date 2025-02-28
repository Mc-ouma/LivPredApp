package com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predcompose.data.model.lineups.PlayerInfo
import com.soccertips.predcompose.data.model.lineups.TeamColors
import com.soccertips.predcompose.data.model.lineups.TeamLineup
import com.soccertips.predcompose.ui.theme.LocalCardColors
import com.soccertips.predcompose.ui.theme.LocalCardElevation


@Composable
fun FixtureLineupsScreen(lineups: Pair<TeamLineup, TeamLineup>) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    // Display the home and away teams' lineups side by side in a Row
    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween // Ensures there's space between the two columns
        ) {
            // Home team lineup
            TeamLineupColumn(lineup = lineups.first)

            // Spacer between the teams (you can adjust the width as needed)
            Spacer(modifier = Modifier.width(16.dp))

            // Away team lineup
            TeamLineupColumn(lineup = lineups.second)
        }
    }
}

@Composable
fun TeamLineupColumn(lineup: TeamLineup) {
    Column(
        modifier = Modifier
            .width(180.dp) // Width for each team's lineup (can be adjusted)
            .padding(horizontal = 8.dp)
    ) {
        // Team Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = lineup.team.logo,
                    onError = {
                        /* Handle error gracefully (show a default logo or icon) */
                    }
                ),
                contentDescription = "${lineup.team.name?.take(20) ?: "Team logo"} ",
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = lineup.team.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = lineup.formation,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Starting XI
        lineup.startXI?.forEach { playerLineup ->
            PlayerRow(player = playerLineup.player, teamColors = lineup.team.colors)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Substitutes
        Text(
            text = "Substitutes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        lineup.substitutes?.forEach { playerLineup ->
            PlayerRow(player = playerLineup.player, teamColors = lineup.team.colors)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Coach
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Coach: ${lineup.coach.name?.take(20) ?: "Coach"}",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = rememberAsyncImagePainter(
                    model = lineup.coach.photo,
                    onError = {
                        /* Handle coach photo error (fallback image) */
                    }
                ),
                contentDescription = lineup.coach.name?.take(20) ?: "Coach photo",
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
fun PlayerRow(
    player: PlayerInfo,
    teamColors: TeamColors?,
) {
    if (teamColors == null) {
        // Handle the case where teamColors is null
        Text(
            text = player.name?.take(20) ?: "Unknown Player",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }

    val playerColor = if (player.pos == "G") teamColors.goalkeeper else teamColors.player

    val primaryColor = Color(android.graphics.Color.parseColor("#${playerColor.primary}"))
    val numberColor = Color(android.graphics.Color.parseColor("#${playerColor.number}"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Player Number
        Text(
            text = player.number?.toString() ?: "N/A",
            modifier = Modifier
                .background(primaryColor)
                .padding(4.dp),
            color = numberColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Player Name and Position
        Column {
            Text(
                text = player.name?.take(20) ?: "Unknown Player",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "(${player.pos?.take(20) ?: "N/A"})",
                style = MaterialTheme.typography.bodySmall,
                color = primaryColor,
            )
        }
    }
}



