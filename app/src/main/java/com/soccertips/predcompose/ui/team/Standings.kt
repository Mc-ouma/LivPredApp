package com.soccertips.predcompose.ui.team


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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predcompose.data.model.standings.Goals
import com.soccertips.predcompose.data.model.standings.HomeAwayRecord
import com.soccertips.predcompose.data.model.standings.OverallRecord
import com.soccertips.predcompose.data.model.standings.TeamInfo
import com.soccertips.predcompose.data.model.standings.TeamStanding
import com.soccertips.predcompose.ui.theme.PredComposeTheme
import kotlin.collections.List

@Composable
fun FixtureStandings(
    standings: List<TeamStanding>,
    teamId1: Int,
    onTeamInfoVisibilityChanged: (Boolean) -> Unit
) {
    val lazyListState = rememberLazyListState()
    // Observe scroll state to hide/show the page info
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                // Hide page info when scrolling up, show it when scrolling down
                onTeamInfoVisibilityChanged(firstVisibleItemIndex == 0)
            }
    }

    val groupedStandings = standings.groupBy { it.group }


    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        groupedStandings.forEach { (groupName, groupStandings) ->
            item {
                GroupHeader(groupName = groupName)
            }
            items(groupStandings) { teamStanding ->
                TeamRow(
                    teamStanding = teamStanding,
                    isHighlighted = teamStanding.team.id == teamId1
                )
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
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "",
                "",
                "Team",
                "P",
                "W",
                "D",
                "L",
                "+/-",
                "GD",
                "Pts"
            ).forEach { text ->
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(if (text == "Team") 4f else if (text == "Pts ") 3f else if (text == "+/-") 4f else 2f),
                    textAlign = TextAlign.End
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
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(
                color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${teamStanding.rank}",
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.Center
        )

        Image(
            painter = rememberAsyncImagePainter(model = teamStanding.team.logo),
            contentDescription = "${teamStanding.team.name} logo",
            modifier = Modifier
                .size(24.dp)
                .weight(1f)
        )

        Text(
            text = teamStanding.team.name,
            maxLines = 1,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(4f)
        )

        listOf(
            teamStanding.all.played,
            teamStanding.all.win,
            teamStanding.all.draw,
            teamStanding.all.lose,
            "${teamStanding.all.goals.`for`}-${teamStanding.all.goals.against}",
            teamStanding.goalsDiff,
            teamStanding.points
        ).forEachIndexed { index, value ->
            Text(
                text = "$value",
                fontWeight = if (index == 6) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(if (index == 4) 3f else 2f),
                textAlign = TextAlign.End
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.S)
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
                rank = 10,
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
    PredComposeTheme {
        FixtureStandings(
            standings = sampleStandings,
            teamId1 = 33,
        ) { visible ->
            // Do nothing
        }
    }
//
}
