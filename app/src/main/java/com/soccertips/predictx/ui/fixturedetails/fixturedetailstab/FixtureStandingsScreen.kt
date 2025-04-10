package com.soccertips.predictx.ui.fixturedetails.fixturedetailstab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.data.model.standings.Goals
import com.soccertips.predictx.data.model.standings.HomeAwayRecord
import com.soccertips.predictx.data.model.standings.OverallRecord
import com.soccertips.predictx.data.model.standings.TeamInfo
import com.soccertips.predictx.data.model.standings.TeamStanding
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import kotlin.collections.List

@Composable
fun FixtureStandingsScreen(
    standings: List<TeamStanding>,
    teamId1: Int,
    teamId2: Int,
) {
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = Modifier
            .fillMaxWidth()
            .padding( 8.dp),
    ) {
        // Group teams by their group name
        val groupedStandings = standings
            .groupBy { it.group } // Group teams by the 'group' field

        // Main Column for the entire screen content
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {

            val primaryBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            val secondaryBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

            // Iterate over the grouped standings
            groupedStandings.toList().forEachIndexed { index, (groupName, groupStandings) ->

                if (index>0) {
                    Spacer(modifier = Modifier.height(16.dp)) // Space between groups
                }

                // Determine the background color based on the index
                val backgroundColor = if (index % 2 == 0) primaryBackground else secondaryBackground


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = backgroundColor,
                            shape = MaterialTheme.shapes.medium)
                        .padding(vertical = 8.dp)


                ) {
                    // Display group header as the card title
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp), // Padding below the title
                        textAlign = TextAlign.Center
                    )

                    // Header Row for the columns under the group name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank, Logo, and Team Name headers
                        Text(text = "", modifier = Modifier.weight(1f))
                        Text(text = "", modifier = Modifier.weight(1f))
                        Text(text = "Team", modifier = Modifier.weight(4f))

                        // Stats headers
                        Text(text = "P", modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(text = "W", modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(text = "D", modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(text = "L", modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(
                            text = "+/-",
                            modifier = Modifier.weight(3f),
                            textAlign = TextAlign.End
                        )
                        Text(text = "GD", modifier = Modifier.weight(2f), textAlign = TextAlign.End)
                        Text(
                            text = "Pts",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f),
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp)) // Space between header and content

                    // Column for team standings in the group
                    groupStandings.forEach { teamStanding ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()

                                .background(
                                    color = if (teamStanding.team.id == teamId1 || teamStanding.team.id == teamId2)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        Color.Transparent,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank column
                            Text(
                                text = "${teamStanding.rank}",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )

                            // Team logo column
                            Image(
                                painter = rememberAsyncImagePainter(model = teamStanding.team.logo),
                                contentDescription = "${teamStanding.team.name} logo",
                                modifier = Modifier
                                    .size(24.dp)
                                    .weight(1f)
                            )

                            // Team name column
                            Text(
                                text = teamStanding.team.name,
                                maxLines = 1,
                                textAlign = TextAlign.Start,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(4f)
                            )

                            // Stats columns (align text to end for numeric values)
                            Text(
                                text = "${teamStanding.all.played}",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "${teamStanding.all.win}",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "${teamStanding.all.draw}",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "${teamStanding.all.lose}",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "${teamStanding.all.goals.`for`}-${teamStanding.all.goals.against}",
                                modifier = Modifier.weight(3f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "${teamStanding.goalsDiff}",
                                modifier = Modifier.weight(2f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "${teamStanding.points}",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(2f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun LeagueCardPreview() {
    val sampleStandings =
        listOf(
            TeamStanding(
                rank = 1,
                team =
                TeamInfo(
                    id = 33,
                    name = "Manchester City",
                    logo = "https://media.api-sports.io/football/teams/50.png",
                ),
                points = 23,
                goalsDiff = 11,
                group = "Premier League",
                form = "WWWDD",
                status = "same",
                description = "Champions League",
                all =
                OverallRecord(
                    played = 9,
                    win = 7,
                    draw = 2,
                    lose = 0,
                    goals = Goals(`for` = 20, against = 9),
                ),
                home =
                HomeAwayRecord(
                    played = 5,
                    win = 4,
                    draw = 1,
                    lose = 0,
                    goals = Goals(`for` = 12, against = 6),
                ),
                away =
                HomeAwayRecord(
                    played = 4,
                    win = 3,
                    draw = 1,
                    lose = 0,
                    goals = Goals(`for` = 8, against = 3),
                ),
                update = "2024-10-28T00:00:00+00:00",
            ),
            TeamStanding(
                rank = 2,
                team =
                TeamInfo(
                    id = 40,
                    name = "Liverpool",
                    logo = "https://media.api-sports.io/football/teams/40.png",
                ),
                points = 22,
                goalsDiff = 12,
                group = "Premier League",
                form = "DWLWW",
                status = "same",
                description = "Champions League",
                all =
                OverallRecord(
                    played = 9,
                    win = 7,
                    draw = 1,
                    lose = 1,
                    goals = Goals(`for` = 17, against = 5),
                ),
                home =
                HomeAwayRecord(
                    played = 4,
                    win = 3,
                    draw = 0,
                    lose = 1,
                    goals = Goals(`for` = 7, against = 2),
                ),
                away =
                HomeAwayRecord(
                    played = 5,
                    win = 4,
                    draw = 1,
                    lose = 0,
                    goals = Goals(`for` = 10, against = 3),
                ),
                update = "2024-10-28T00:00:00+00:00",
            ),
        )
    FixtureStandingsScreen(standings = sampleStandings, teamId1 = 33, teamId2 = 34)
//
}
