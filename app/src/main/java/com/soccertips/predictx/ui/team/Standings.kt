package com.soccertips.predictx.ui.team


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.standings.Goals
import com.soccertips.predictx.data.model.standings.HomeAwayRecord
import com.soccertips.predictx.data.model.standings.OverallRecord
import com.soccertips.predictx.data.model.standings.TeamInfo
import com.soccertips.predictx.data.model.standings.TeamStanding
import com.soccertips.predictx.ui.theme.PredictXTheme
import kotlin.collections.List

@Composable
fun FixtureStandings(
    standings: List<TeamStanding>,
    teamId1: Int,
    lazyListState: LazyListState,
) {
    val groupedStandings = standings.groupBy { it.group }

    // Define colors inside the composable context
    val primaryBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val secondaryBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {


        groupedStandings.toList().forEachIndexed { index, (groupName, groupStandings) ->
            val backgroundColor = if (index % 2 == 0) primaryBackground else secondaryBackground

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = backgroundColor, shape = MaterialTheme.shapes.medium)
                        .padding(8.dp)
                ) {
                    GroupHeader(groupName = groupName)
                }
            }
            items(groupStandings) { teamStanding ->
                TeamRow(
                    teamStanding = teamStanding,
                    isHighlighted = teamStanding.team.id == teamId1
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


@Composable
fun GroupHeader(groupName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = groupName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add rank column header
            Text(
                text = "#",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center
            )

            // Other column headers with consistent alignment
            listOf(
                stringResource(R.string.team),
                stringResource(R.string.played),
                stringResource(R.string.won),
                stringResource(R.string.drawn),
                stringResource(R.string.lost),
                stringResource(R.string.goal_difference),
                stringResource(R.string.points)
            ).forEachIndexed { index, text ->
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(
                        when {
                            index == 0 -> 4f // Team name
                            index == 6 -> 3f // Points
                            else -> 2f       // Other stats
                        }
                    ),
                    textAlign = if (index == 0) TextAlign.Start else TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun TeamRow(teamStanding: TeamStanding, isHighlighted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${teamStanding.rank}",
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.Center
        )


        Text(
            text = teamStanding.team.name,
            maxLines = 1,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(6f) // Increased weight from 4f to 6f
        )

        listOf(
            teamStanding.all.played,
            teamStanding.all.win,
            teamStanding.all.draw,
            teamStanding.all.lose,
            teamStanding.goalsDiff,
            teamStanding.points
        ).forEachIndexed { index, value ->
            Text(
                text = "$value",
                fontWeight = if (index == 5) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(
                    when (index) {
                        4 -> 1.5f  // Goal difference - reduced from 3f or 2f
                        5 -> 2.5f  // Points - give slightly more space
                        else -> 2f // Other stats remain the same
                    }
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true, locale = "pt")
@Composable
private fun LeagueCardPreview() {
    val sampleStandings = listOf(
        TeamStanding(
            rank = 1,
            team = TeamInfo(
                id = 33,
                name = "Manchester City",
                logo = "https://media.api-sports.io/football/teams/50.png",
            ),
            points = 23,
            goalsDiff = 11,
            group = "Premier League - Group A",
            form = "WWWDD",
            status = "same",
            description = "Champions League",
            all = OverallRecord(
                played = 9,
                win = 7,
                draw = 2,
                lose = 0,
                goals = Goals(`for` = 20, against = 9),
            ),
            home = HomeAwayRecord(
                played = 5,
                win = 4,
                draw = 1,
                lose = 0,
                goals = Goals(`for` = 12, against = 6),
            ),
            away = HomeAwayRecord(
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
            team = TeamInfo(
                id = 40,
                name = "Liverpool",
                logo = "https://media.api-sports.io/football/teams/40.png",
            ),
            points = 22,
            goalsDiff = 12,
            group = "Premier League - Group A",
            form = "DWLWW",
            status = "same",
            description = "Champions League",
            all = OverallRecord(
                played = 9,
                win = 7,
                draw = 1,
                lose = 1,
                goals = Goals(`for` = 17, against = 5),
            ),
            home = HomeAwayRecord(
                played = 4,
                win = 3,
                draw = 0,
                lose = 1,
                goals = Goals(`for` = 7, against = 2),
            ),
            away = HomeAwayRecord(
                played = 5,
                win = 4,
                draw = 1,
                lose = 0,
                goals = Goals(`for` = 10, against = 3),
            ),
            update = "2024-10-28T00:00:00+00:00",
        ),
        TeamStanding(
            rank = 1,
            team = TeamInfo(
                id = 50,
                name = "Chelsea",
                logo = "https://media.api-sports.io/football/teams/49.png",
            ),
            points = 20,
            goalsDiff = 8,
            group = "Premier League - Group B",
            form = "WWLWD",
            status = "same",
            description = "Champions League",
            all = OverallRecord(
                played = 9,
                win = 6,
                draw = 2,
                lose = 1,
                goals = Goals(`for` = 15, against = 7),
            ),
            home = HomeAwayRecord(
                played = 5,
                win = 3,
                draw = 1,
                lose = 1,
                goals = Goals(`for` = 9, against = 4),
            ),
            away = HomeAwayRecord(
                played = 4,
                win = 3,
                draw = 1,
                lose = 0,
                goals = Goals(`for` = 6, against = 3),
            ),
            update = "2024-10-28T00:00:00+00:00",
        )
    )
    PredictXTheme {
        FixtureStandings(
            standings = sampleStandings,
            teamId1 = 33,
            lazyListState = rememberLazyListState()
        )
    }
}