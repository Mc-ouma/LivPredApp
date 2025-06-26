package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.lineups.CoachInfo
import com.soccertips.predictx.data.model.lineups.PlayerColors
import com.soccertips.predictx.data.model.lineups.PlayerInfo
import com.soccertips.predictx.data.model.lineups.PlayerLineup
import com.soccertips.predictx.data.model.lineups.TeamColors
import com.soccertips.predictx.data.model.lineups.TeamInfo
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

@Composable
fun FixtureLineupsScreen(lineups: Pair<TeamLineup, TeamLineup>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Match Formation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
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

            // Formation Visualization
            FormationVisualization(
                homeTeam = lineups.first,
                awayTeam = lineups.second,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(vertical = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Detailed lineups
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home team lineup details
                TeamLineupDetails(
                    lineup = lineups.first,
                    modifier = Modifier.weight(1f)
                )

                // Vertical divider
                VerticalDivider(
                    modifier = Modifier
                        .height(300.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )

                // Away team lineup details
                TeamLineupDetails(
                    lineup = lineups.second,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TeamHeaderCompact(lineup: TeamLineup) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
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
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
fun GridBasedFormation(
    teamLineup: TeamLineup,
    isLeftSide: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Calculate maximum row and column from grid positions
        val maxRow = teamLineup.startXI?.mapNotNull {
            it.player.grid?.split(":")?.getOrNull(0)?.toIntOrNull()
        }?.maxOrNull() ?: 4
        val maxCol = teamLineup.startXI?.mapNotNull {
            it.player.grid?.split(":")?.getOrNull(1)?.toIntOrNull()
        }?.maxOrNull() ?: 5

        // Group players by row to calculate column centers
        val playersByRow = teamLineup.startXI?.groupBy {
            it.player.grid?.split(":")?.getOrNull(0)?.toIntOrNull() ?: -1
        }?.filter { it.key != -1 } ?: emptyMap()

        playersByRow.forEach { (row, players) ->
            // Calculate the center column for this row
            val cols = players.mapNotNull { it.player.grid?.split(":")?.getOrNull(1)?.toIntOrNull() }
            val avgCol = if (cols.isNotEmpty()) cols.average().toFloat() else maxCol / 2f
            val colSpread = if (cols.size > 1) (cols.max() - cols.min()).toFloat() / maxCol else 0.2f

            players.forEach { playerLineup ->
                val player = playerLineup.player
                val grid = player.grid ?: return@forEach
                val parts = grid.split(":")
                val r = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
                val c = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach

                // Calculate x and y positions
                val xPos: Float
                val yPos: Float
                if (isLeftSide) { // Home team (left half)
                    xPos = if (maxRow > 1) (r - 1).toFloat() / (maxRow - 1).toFloat() * 0.5f else 0f
                    // Center y-position around avgCol, scaled to pitch height
                    yPos = ((c - avgCol) / (maxCol * colSpread)) + 0.5f
                } else { // Away team (right half)
                    xPos = 1f - (if (maxRow > 1) (r - 1).toFloat() / (maxRow - 1).toFloat() * 0.5f else 0f)
                    yPos = 1f - ((c - avgCol) / (maxCol * colSpread)) - 0.5f
                }

                // Ensure yPos stays within bounds [0, 1]
                val clampedYPos = yPos.coerceIn(0.1f, 0.9f)

                // Determine player colors
                val playerColor = if (player.pos == " #G") {
                    parseColorOrFallback(teamLineup.team.colors?.goalkeeper?.primary, MaterialTheme.colorScheme.secondary)
                } else {
                    parseColorOrFallback(teamLineup.team.colors?.player?.primary, MaterialTheme.colorScheme.primary)
                }
                if (player.pos == "G") {
                    parseColorOrFallback(teamLineup.team.colors?.goalkeeper?.number, Color.White)
                } else {
                    parseColorOrFallback(teamLineup.team.colors?.player?.number, Color.White)
                }

                // Place player dot
                Box(
                    modifier = Modifier
                        .offset(
                            x = (xPos * 250).dp - 10.dp,
                            y = (clampedYPos * 140).dp - 10.dp
                        )
                ) {
                    PlayerDot(
                        color = playerColor,
                        player = player
                    )
                }
            }
        }
    }
}

@Composable
fun FormationVisualization(
    homeTeam: TeamLineup,
    awayTeam: TeamLineup,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2D5339))
    ) {
        // Field markings
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Draw field markings
            drawRect(
                color = Color(0xFF216128).copy(alpha = 0.8f),
                size = size
            )

            // Center circle
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = size.minDimension * 0.15f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )

            // Center line
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                strokeWidth = 2f
            )

            // Goal areas
            val goalAreaWidth = size.width * 0.1f
            drawRect(
                color = Color.White.copy(alpha = 0.4f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.3f),
                size = androidx.compose.ui.geometry.Size(goalAreaWidth, size.height * 0.4f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            drawRect(
                color = Color.White.copy(alpha = 0.4f),
                topLeft = androidx.compose.ui.geometry.Offset(size.width - goalAreaWidth, size.height * 0.3f),
                size = androidx.compose.ui.geometry.Size(goalAreaWidth, size.height * 0.4f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }

        // Add player formations
        GridBasedFormation(
            teamLineup = homeTeam,
            isLeftSide = true,
            modifier = Modifier.fillMaxSize()
        )

        GridBasedFormation(
            teamLineup = awayTeam,
            isLeftSide = false,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun PlayerDot(
    color: Color,
    player: PlayerInfo? = null
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        // Show player number if available
        if (player?.number != null) {
            Text(
                text = player.number.toString(),
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TeamLineupDetails(lineup: TeamLineup, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(300.dp), // Fixed height to avoid infinite height constraints
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Team name as a section header
        Text(
            text = lineup.team.name ?: "",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Use a scrollable column instead of LazyColumn to avoid nesting issues
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Take remaining space
                .verticalScroll(rememberScrollState()), // Make it scrollable
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Starting XI header
            SectionHeader(title = stringResource(R.string.starting_eleven))

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier
                .weight(0.15f)
                .padding(end = 8.dp),
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
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coach photo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = coach.photo,
                        onError = {
                            /* Handle error gracefully */
                        }
                    ),
                    contentDescription = coach.name?.take(20)
                        ?: stringResource(R.string.coach_photo),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Player Number in a circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(primaryColor),
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player.name?.take(20) ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position indicator chip
                    Surface(
                        modifier = Modifier.padding(end = 6.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = primaryColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = player.pos ?: stringResource(R.string.na),
                            style = MaterialTheme.typography.bodySmall,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Grid position if available
                    if (!player.grid.isNullOrBlank()) {
                        Text(
                            text = player.grid.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LineUpPreview() {
    val team1 = TeamLineup(
        team = TeamInfo(
            id = 12,
            name = "Team A",
            logo = "https://example.com/logo_a.png",
            colors = TeamColors(
                player = PlayerColors(
                    primary = "#FF0000",
                    number = "#FFFFFF",
                    border = "#000000"
                ),
                goalkeeper = PlayerColors(
                    primary = "#00FF00",
                    number = "#FFFFFF",
                    border = "#000000"
                ),
            ),
        ),


        formation = "4-3-1-2",
        startXI = listOf(
            PlayerLineup(
                PlayerInfo(
                    id = 2932,
                    name = "Jordan Pickford",
                    number = 1,
                    pos = "G",
                    grid = "1:1"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 19150,
                    name = "Mason Holgate",
                    number = 4,
                    pos = "D",
                    grid = "2:4"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 2934,
                    name = "Michael Keane",
                    number = 5,
                    pos = "D",
                    grid = "2:3"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 19073,
                    name = "Ben Godfrey",
                    number = 22,
                    pos = "D",
                    grid = "2:2"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 2724,
                    name = "Lucas Digne",
                    number = 12,
                    pos = "D",
                    grid = "2:1"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 18805,
                    name = "Abdoulaye Doucouré",
                    number = 16,
                    pos = "M",
                    grid = "3:3"
                )
            ),
            PlayerLineup(PlayerInfo(id = 326, name = "Allan", number = 6, pos = "M", grid = "3:2")),
            PlayerLineup(
                PlayerInfo(
                    id = 18762,
                    name = "Tom Davies",
                    number = 26,
                    pos = "M",
                    grid = "3:1"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 2795,
                    name = "Gylfi Sigurðsson",
                    number = 10,
                    pos = "M",
                    grid = "4:1"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 18766,
                    name = "Dominic Calvert-Lewin",
                    number = 9,
                    pos = "F",
                    grid = "5:2"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 2413,
                    name = "Richarlison",
                    number = 7,
                    pos = "F",
                    grid = "5:1"
                )
            )
        ),
        coach = CoachInfo(id = 1, name = "Coach A", photo = "https://example.com/coach_a.png"),
        substitutes =
            listOf(
                PlayerLineup(
                    player =
                        PlayerInfo(
                            name =
                                "Sub 2", pos =
                                "F", number =
                                9, id =
                                8, grid =
                                "CF"
                        )
                )
            ),
    )

    val team2 = TeamLineup(
        team = TeamInfo(
            id = 34,
            name = "Team B",
            logo = "https://example.com/logo_b.png",
            colors = TeamColors(
                player = PlayerColors(
                    primary = "#0000FF",
                    number = "#FFFFFF",
                    border = "#000000"
                ),
                goalkeeper = PlayerColors(
                    primary = "#FFFF00",
                    number = "#FFFFFF",
                    border = "#000000"
                ),
            ),
        ),

        formation = "4-3-3",
        startXI = listOf(
            PlayerLineup(
                PlayerInfo(
                    id = 617,
                    name = "Ederson",
                    number = 31,
                    pos = "G",
                    grid = "1:1"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 627,
                    name = "Kyle Walker",
                    number = 2,
                    pos = "D",
                    grid = "2:4"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 626,
                    name = "John Stones",
                    number = 5,
                    pos = "D",
                    grid = "2:3"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 567,
                    name = "Rúben Dias",
                    number = 3,
                    pos = "D",
                    grid = "2:2"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 641,
                    name = "Oleksandr Zinchenko",
                    number = 11,
                    pos = "D",
                    grid = "2:1"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 629,
                    name = "Kevin De Bruyne",
                    number = 17,
                    pos = "M",
                    grid = "3:3"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 640,
                    name = "Fernandinho",
                    number = 25,
                    pos = "M",
                    grid = "3:2"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 631,
                    name = "Phil Foden",
                    number = 47,
                    pos = "M",
                    grid = "3:1"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 635,
                    name = "Riyad Mahrez",
                    number = 26,
                    pos = "F",
                    grid = "4:3"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 643,
                    name = "Gabriel Jesus",
                    number = 9,
                    pos = "F",
                    grid = "4:2"
                )
            ),
            PlayerLineup(
                PlayerInfo(
                    id = 645,
                    name = "Raheem Sterling",
                    number = 7,
                    pos = "F",
                    grid = "4:1"
                )
            )
        ),

        substitutes =
            listOf(
                PlayerLineup(
                    player =
                        PlayerInfo(
                            name =
                                "Sub 2", pos =
                                "F", number =
                                9, id =
                                8, grid =
                                "CF"
                        )
                )
            ),
        coach =
            CoachInfo(
                id =
                    2, name =
                    "Coach B", photo =
                    "https://example.com/coach_b.png"
            ),

        )

    FixtureLineupsScreen(lineups = Pair(team1, team2))


}