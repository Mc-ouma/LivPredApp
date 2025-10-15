package com.soccertips.predictx.ui.team


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.standings.Goals
import com.soccertips.predictx.data.model.standings.HomeAwayRecord
import com.soccertips.predictx.data.model.standings.OverallRecord
import com.soccertips.predictx.data.model.standings.TeamInfo
import com.soccertips.predictx.data.model.standings.TeamStanding
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import com.soccertips.predictx.ui.theme.PredictXTheme

@Composable
fun FixtureStandings(
    standings: List<TeamStanding>,
    teamId1: Int,
    lazyListState: LazyListState,
) {
    val groupedStandings = standings.groupBy { it.group }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedStandings.forEach { (groupName, groupStandings) ->
            item {
                ModernGroupCard(
                    groupName = groupName,
                    standings = groupStandings,
                    highlightedTeamId = teamId1
                )
            }
        }
    }
}


@Composable
fun ModernGroupCard(
    groupName: String,
    standings: List<TeamStanding>,
    highlightedTeamId: Int
) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current

    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Group Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Column Headers
            StandingsHeader()

            Spacer(modifier = Modifier.height(8.dp))

            // Team Rows
            standings.forEachIndexed { index, teamStanding ->
                ModernTeamRow(
                    teamStanding = teamStanding,
                    isHighlighted = teamStanding.team.id == highlightedTeamId
                )

                if (index < standings.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun StandingsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Text(
            text = "#",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Team
        Text(
            text = stringResource(R.string.team),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Stats
        listOf("P", "W", "D", "L", "GD", "Pts").forEach { header ->
            Text(
                text = header,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(if (header == "Pts") 36.dp else if (header == "GD") 30.dp else 28.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ModernTeamRow(
    teamStanding: TeamStanding,
    isHighlighted: Boolean
) {
    val backgroundColor = when {
        isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val borderColor = when {
        isHighlighted -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (isHighlighted) {
                    Modifier.border(
                        width = 2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank with badge
        Box(
            modifier = Modifier
                .width(28.dp),
            contentAlignment = Alignment.Center
        ) {
            val rankColor = when (teamStanding.rank) {
                1 -> Color(0xFFFFD700) // Gold
                2 -> Color(0xFFC0C0C0) // Silver
                3 -> Color(0xFFCD7F32) // Bronze
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }

            if (teamStanding.rank <= 3) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(rankColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${teamStanding.rank}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = rankColor
                    )
                }
            } else {
                Text(
                    text = "${teamStanding.rank}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Team Name and Description
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = teamStanding.team.name,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            // Description indicator
            /*if (teamStanding.description?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(2.dp))
                DescriptionIndicator(description = teamStanding.description)
            }*/
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Statistics
        StatCell(teamStanding.all.played.toString(), 28.dp)
        StatCell(teamStanding.all.win.toString(), 28.dp, Color(0xFF4CAF50))
        StatCell(teamStanding.all.draw.toString(), 28.dp, Color(0xFFFF9800))
        StatCell(teamStanding.all.lose.toString(), 28.dp, Color(0xFFF44336))

        // Goal Difference with indicator
        Box(
            modifier = Modifier.width(30.dp),
            contentAlignment = Alignment.Center
        ) {
            val gdColor = when {
                teamStanding.goalsDiff > 0 -> Color(0xFF4CAF50)
                teamStanding.goalsDiff < 0 -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }

            Text(
                text = if (teamStanding.goalsDiff > 0) "+${teamStanding.goalsDiff}" else "${teamStanding.goalsDiff}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = gdColor,
                textAlign = TextAlign.Center
            )
        }

        // Points with emphasis
        Box(
            modifier = Modifier
                .width(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${teamStanding.points}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatCell(
    value: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
) {
    Box(
        modifier = Modifier.width(width),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}


@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
private fun LeagueCardPreview() {
    // Using actual data from teamstanding.json
    val sampleStandings = listOf(
        // Premier League Standing
        TeamStanding(
            rank = 2,
            team = TeamInfo(
                id = 33,
                name = "Manchester United",
                logo = "https://media.api-sports.io/football/teams/33.png",
            ),
            points = 60,
            goalsDiff = 25,
            group = "Premier League",
            form = "WWWDD",
            status = "same",
            description = "Promotion - Champions League (Group Stage)",
            all = OverallRecord(
                played = 30,
                win = 17,
                draw = 9,
                lose = 4,
                goals = Goals(`for` = 58, against = 33),
            ),
            home = HomeAwayRecord(
                played = 15,
                win = 8,
                draw = 3,
                lose = 4,
                goals = Goals(`for` = 31, against = 20),
            ),
            away = HomeAwayRecord(
                played = 15,
                win = 9,
                draw = 6,
                lose = 0,
                goals = Goals(`for` = 27, against = 13),
            ),
            update = "2021-04-05T00:00:00+00:00",
        ),
        TeamStanding(
            rank = 1,
            team = TeamInfo(
                id = 50,
                name = "Manchester City",
                logo = "https://media.api-sports.io/football/teams/50.png",
            ),
            points = 74,
            goalsDiff = 48,
            group = "Premier League",
            form = "WWLWW",
            status = "same",
            description = "Promotion - Champions League (Group Stage)",
            all = OverallRecord(
                played = 30,
                win = 23,
                draw = 5,
                lose = 2,
                goals = Goals(`for` = 70, against = 22),
            ),
            home = HomeAwayRecord(
                played = 15,
                win = 12,
                draw = 2,
                lose = 1,
                goals = Goals(`for` = 38, against = 10),
            ),
            away = HomeAwayRecord(
                played = 15,
                win = 11,
                draw = 3,
                lose = 1,
                goals = Goals(`for` = 32, against = 12),
            ),
            update = "2021-04-05T00:00:00+00:00",
        ),
        TeamStanding(
            rank = 3,
            team = TeamInfo(
                id = 40,
                name = "Liverpool",
                logo = "https://media.api-sports.io/football/teams/40.png",
            ),
            points = 57,
            goalsDiff = 21,
            group = "Premier League",
            form = "WDLWW",
            status = "same",
            description = "Promotion - Champions League (Group Stage)",
            all = OverallRecord(
                played = 30,
                win = 16,
                draw = 9,
                lose = 5,
                goals = Goals(`for` = 55, against = 34),
            ),
            home = HomeAwayRecord(
                played = 15,
                win = 9,
                draw = 4,
                lose = 2,
                goals = Goals(`for` = 30, against = 18),
            ),
            away = HomeAwayRecord(
                played = 15,
                win = 7,
                draw = 5,
                lose = 3,
                goals = Goals(`for` = 25, against = 16),
            ),
            update = "2021-04-05T00:00:00+00:00",
        ),
        // UEFA Champions League Standing
        TeamStanding(
            rank = 3,
            team = TeamInfo(
                id = 33,
                name = "Borussia Mönchengladbach",
                logo = "https://media.api-sports.io/football/teams/33.png",
            ),
            points = 9,
            goalsDiff = 5,
            group = "UEFA Champions League: Group H",
            form = "LLWLW",
            status = "same",
            description = "Promotion - Europa League (Round of 32)",
            all = OverallRecord(
                played = 6,
                win = 3,
                draw = 0,
                lose = 3,
                goals = Goals(`for` = 15, against = 10),
            ),
            home = HomeAwayRecord(
                played = 3,
                win = 2,
                draw = 0,
                lose = 1,
                goals = Goals(`for` = 10, against = 4),
            ),
            away = HomeAwayRecord(
                played = 3,
                win = 1,
                draw = 0,
                lose = 2,
                goals = Goals(`for` = 5, against = 6),
            ),
            update = "2021-04-07T00:00:00+00:00",
        ),
        TeamStanding(
            rank = 1,
            team = TeamInfo(
                id = 49,
                name = "Chelsea",
                logo = "https://media.api-sports.io/football/teams/49.png",
            ),
            points = 14,
            goalsDiff = 7,
            group = "UEFA Champions League: Group H",
            form = "WWDWW",
            status = "same",
            description = "Promotion - Champions League (Round of 16)",
            all = OverallRecord(
                played = 6,
                win = 4,
                draw = 2,
                lose = 0,
                goals = Goals(`for` = 12, against = 5),
            ),
            home = HomeAwayRecord(
                played = 3,
                win = 2,
                draw = 1,
                lose = 0,
                goals = Goals(`for` = 6, against = 2),
            ),
            away = HomeAwayRecord(
                played = 3,
                win = 2,
                draw = 1,
                lose = 0,
                goals = Goals(`for` = 6, against = 3),
            ),
            update = "2021-04-07T00:00:00+00:00",
        ),
        TeamStanding(
            rank = 2,
            team = TeamInfo(
                id = 85,
                name = "PSG",
                logo = "https://media.api-sports.io/football/teams/85.png",
            ),
            points = 12,
            goalsDiff = 6,
            group = "UEFA Champions League: Group H",
            form = "WWWLL",
            status = "same",
            description = "Promotion - Champions League (Round of 16)",
            all = OverallRecord(
                played = 6,
                win = 4,
                draw = 0,
                lose = 2,
                goals = Goals(`for` = 13, against = 7),
            ),
            home = HomeAwayRecord(
                played = 3,
                win = 3,
                draw = 0,
                lose = 0,
                goals = Goals(`for` = 9, against = 2),
            ),
            away = HomeAwayRecord(
                played = 3,
                win = 1,
                draw = 0,
                lose = 2,
                goals = Goals(`for` = 4, against = 5),
            ),
            update = "2021-04-07T00:00:00+00:00",
        )
    )

    PredictXTheme {
        Surface {
            FixtureStandings(
                standings = sampleStandings,
                teamId1 = 33,
                lazyListState = rememberLazyListState()
            )
        }
    }
}