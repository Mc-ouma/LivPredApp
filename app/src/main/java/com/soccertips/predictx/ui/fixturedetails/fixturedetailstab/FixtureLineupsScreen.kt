package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.lineups.CoachInfo
import com.soccertips.predictx.data.model.lineups.PlayerInfo
import com.soccertips.predictx.data.model.lineups.TeamColors
import com.soccertips.predictx.data.model.lineups.TeamLineup

// Helper function to safely parse color strings
private fun parseColorOrFallback(colorString: String?, fallback: Color): Color {
    if (colorString == null) return fallback
    return try {
        if (colorString.startsWith("#")) {
            Color(colorString.toColorInt())
        } else {
            Color("#$colorString".toColorInt())
        }
    } catch (_: Exception) {
        fallback
    }
}

// Object to handle localized player positions
object PlayerPositionLocalizer {
    @Composable
    fun getLocalizedPosition(position: String?): String {
        return when (position?.uppercase()) {
            "G" -> stringResource(R.string.position_goalkeeper)
            "D" -> stringResource(R.string.position_defender)
            "M" -> stringResource(R.string.position_midfielder)
            "F" -> stringResource(R.string.position_forward)
            else -> position ?: stringResource(R.string.na)
        }
    }

    @Composable
    fun getShortLocalizedPosition(position: String?): String {
        return when (position?.uppercase()) {
            "G" -> stringResource(R.string.position_goalkeeper_short)
            "D" -> stringResource(R.string.position_defender_short)
            "M" -> stringResource(R.string.position_midfielder_short)
            "F" -> stringResource(R.string.position_forward_short)
            else -> position ?: stringResource(R.string.na)
        }
    }
}

@Composable
fun FixtureLineupsScreen(lineups: Pair<TeamLineup, TeamLineup>) {
    Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Match Formation Header
            Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                TeamHeaderCompact(lineups.first)
                Text(
                        text = "vs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                )
                TeamHeaderCompact(lineups.second)
            }

            HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Detailed lineups
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home team lineup details
                TeamLineupDetails(lineup = lineups.first, modifier = Modifier.weight(1f))

                // Vertical divider
                VerticalDivider(
                        modifier = Modifier.height(300.dp).width(1.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                )

                // Away team lineup details
                TeamLineupDetails(lineup = lineups.second, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TeamHeaderCompact(lineup: TeamLineup) {
    Card(
            shape = RoundedCornerShape(8.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
        ) {
            // Team Logo
            Box(
                    modifier =
                            Modifier.size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(4.dp),
                    contentAlignment = Alignment.Center
            ) {
                Image(
                        painter =
                                rememberAsyncImagePainter(
                                        model = lineup.team.logo,
                                        onError = {
                                            /* Handle error gracefully */
                                        }
                                ),
                        contentDescription = lineup.team.name?.take(20)
                                        ?: stringResource(R.string.team_logo),
                        modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Team Info
            Column {
                Text(
                        text = lineup.team.name ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )

                // Formation with visual indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                                text = lineup.formation ?: "-",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeamLineupDetails(lineup: TeamLineup, modifier: Modifier = Modifier) {
    Column(
            modifier =
                    modifier.padding(horizontal = 8.dp)
                            .fillMaxWidth()
                            .height(300.dp), // Fixed height to avoid infinite height constraints
            verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        // Use a scrollable column instead of LazyColumn to avoid nesting issues
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .weight(1f) // Take remaining space
                                .verticalScroll(rememberScrollState()), // Make it scrollable
                verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // Starting XI players
            lineup.startXI?.forEach { playerLineup ->
                PlayerRow(player = playerLineup.player, teamColors = lineup.team.colors)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Substitutes header
            SectionHeader(title = stringResource(R.string.substitutes))

            // Substitutes players
            lineup.substitutes?.forEach { playerLineup ->
                PlayerRow(player = playerLineup.player, teamColors = lineup.team.colors)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Coach section
            CoachSection(coach = lineup.coach)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
                modifier = Modifier.weight(0.15f).padding(end = 8.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary
        )

        Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
        )

        HorizontalDivider(
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun CoachSection(coach: CoachInfo) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Coach photo
            Box(
                    modifier =
                            Modifier.size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
            ) {
                Image(
                        painter =
                                rememberAsyncImagePainter(
                                        model = coach.photo,
                                        onError = {
                                            /* Handle error gracefully */
                                        }
                                ),
                        contentDescription = coach.name?.take(20)
                                        ?: stringResource(R.string.coach_photo),
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Coach info
            Column {
                Text(
                        text = stringResource(R.string.coach_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                        text = coach.name ?: stringResource(R.string.coach_default),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                )
            }
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
                text = player.name?.take(20) ?: stringResource(R.string.unknown_player),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
        )
        return
    }

    val playerColor = if (player.pos == "G") teamColors.goalkeeper else teamColors.player

    // Use helper function instead of try-catch in composable
    val primaryColor = parseColorOrFallback(playerColor.primary, MaterialTheme.colorScheme.primary)
    val numberColor = parseColorOrFallback(playerColor.number, MaterialTheme.colorScheme.onPrimary)
    parseColorOrFallback(playerColor.border, MaterialTheme.colorScheme.outline)

    Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
            shape = RoundedCornerShape(8.dp)
    ) {
        Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            // Player Number in a circle
            Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(primaryColor),
                    contentAlignment = Alignment.Center
            ) {
                Text(
                        text = player.number?.toString() ?: stringResource(R.string.na),
                        color = numberColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Player Name and Position
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = player.name?.take(20) ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Position indicator chip
                    Surface(
                            modifier = Modifier.padding(end = 6.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = primaryColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                                text =
                                        PlayerPositionLocalizer.getShortLocalizedPosition(
                                                player.pos
                                        ),
                                style = MaterialTheme.typography.bodySmall,
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}


