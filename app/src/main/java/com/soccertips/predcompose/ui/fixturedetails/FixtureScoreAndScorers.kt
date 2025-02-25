package com.soccertips.predcompose.ui.fixturedetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.soccertips.predcompose.ui.FixtureDetailsUiState
import com.soccertips.predcompose.ui.theme.LocalCardColors
import com.soccertips.predcompose.ui.theme.LocalCardElevation
import com.soccertips.predcompose.viewmodel.FixtureDetailsViewModel

@Composable
fun FixtureScoreAndScorers(
    viewModel: FixtureDetailsViewModel,
    modifier: Modifier = Modifier.Companion,
    navController: NavController,
    leagueId: String,
    season: String,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is FixtureDetailsUiState.Loading -> {
            Text(
                text = "Loading...",
                modifier = Modifier.Companion.padding(16.dp),
                color = Color.Companion.Gray,
            )
        }

        is FixtureDetailsUiState.Success -> {
            val response = (uiState as FixtureDetailsUiState.Success).fixtureDetails
            val homeTeamId = response.teams.home.id
            val awayTeamId = response.teams.away.id
            val homeGoalScorers = viewModel.getHomeGoalScorers(response.events, homeTeamId)
            val awayGoalScorers = viewModel.getAwayGoalScorers(response.events, awayTeamId)
            viewModel.formatTimestamp(response.fixture.timestamp)
            val matchStatusText =
                viewModel.getMatchStatusText(
                    response.fixture.status.short,
                    response.fixture.status.elapsed,
                    response.fixture.timestamp,
                )
            val cardColors = LocalCardColors.current
            val cardElevation = LocalCardElevation.current

            Card(
                modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = cardColors,
                elevation = cardElevation
            ) {
                Column(
                    modifier
                        .padding(8.dp)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                ) {
                    Row(
                        modifier =
                        Modifier.Companion
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.Companion.weight(1f)) {
                            response.teams.home.let { homeTeam ->
                                TeamColumn(
                                    team = homeTeam,
                                    leagueId = leagueId,
                                    season = season,
                                    navController = navController
                                )
                            }
                        }
                        Column(
                            modifier =
                            Modifier.Companion
                                .weight(1f)
                                .wrapContentHeight()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(text = response.fixture.status.short)
                            Row(
                                modifier =
                                Modifier.Companion
                                    .wrapContentHeight()
                                    .align(Alignment.Companion.CenterHorizontally),
                                verticalAlignment = Alignment.Companion.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                response.score.let {
                                    Text(
                                        text = response.goals.home.toString(),
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = "-",
                                        fontSize = 16.sp,
                                        modifier = Modifier.Companion.padding(horizontal = 4.dp),
                                    )
                                    Text(
                                        text = response.goals.away.toString(),
                                        fontSize = 20.sp
                                    )
                                }
                            }
                            if (matchStatusText.isNotEmpty()) {
                                Text(
                                    text = matchStatusText,
                                    fontSize = 14.sp,
                                    color = Color(0xFF6200EE),
                                    modifier = Modifier.Companion.padding(top = 4.dp),
                                    textAlign = TextAlign.Companion.Center,
                                )
                            }

                        }
                        Column(modifier = Modifier.Companion.weight(1f)) {
                            response.teams.away.let { awayTeam ->
                                TeamColumn(
                                    team = awayTeam,
                                    leagueId = leagueId,
                                    season = season,
                                    navController = navController
                                )
                            }
                        }
                    }
                    Row(
                        modifier =
                        Modifier.Companion
                            .wrapContentWidth()
                            .wrapContentHeight()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Companion.CenterVertically,
                    ) {
                        Column(modifier = Modifier.Companion.weight(1f)) {
                            homeGoalScorers.forEach { (playerName, elapsed) ->
                                Scorers(
                                    playerName = playerName,
                                    elapsed = elapsed,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier.Companion.weight(1f),
                            contentAlignment = Alignment.Companion.Center,
                        ) {
                            if (homeGoalScorers.isNotEmpty() || awayGoalScorers.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.SportsSoccer,
                                    contentDescription = "Soccer Ball",
                                    tint = Color.Companion.Gray,
                                    modifier = Modifier.Companion.size(24.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.Companion.weight(1f)) {
                            awayGoalScorers.forEach { (playerName, elapsed) ->
                                Scorers(
                                    playerName = playerName,
                                    elapsed = elapsed,
                                )
                            }
                        }
                    }
                }
            }
        }

        is FixtureDetailsUiState.Error -> {
            ErrorScreen(
                paddingValues = PaddingValues(0.dp),
                message = (uiState as FixtureDetailsUiState.Error).message
            )
        }

        else -> {
            EmptyScreen(paddingValues = PaddingValues(0.dp), message = "No data available")
        }


    }

}