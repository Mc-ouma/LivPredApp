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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@Composable
fun FixtureScoreAndScorers(
    viewModel: FixtureDetailsViewModel,
    modifier: Modifier = Modifier,
    navController: NavController,
    leagueId: String,
    season: String,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is FixtureDetailsUiState.Loading -> {
            Text(
                text = "Loading...",
                modifier = Modifier.padding(16.dp),
                color = Color.Gray,
            )
        }

        is FixtureDetailsUiState.Success -> {
            val response = (uiState as FixtureDetailsUiState.Success).fixtureDetails
            val homeTeamId = response.teams.home.id
            val awayTeamId = response.teams.away.id
            val homeGoalScorers = viewModel.getHomeGoalScorers(response.events, homeTeamId)
            val awayGoalScorers = viewModel.getAwayGoalScorers(response.events, awayTeamId)
            viewModel.formatTimestamp(response.fixture.timestamp)
            val initialMatchStatusText =
                viewModel.getMatchStatusText(
                    response.fixture.status.short,
                    response.fixture.status.elapsed,
                    response.fixture.timestamp,
                )
            val cardColors = LocalCardColors.current
            val cardElevation = LocalCardElevation.current

            var matchStatusText by remember { mutableStateOf(initialMatchStatusText) }
            val timestamp = response.fixture.timestamp

            LaunchedEffect(key1 = timestamp) {
                while (true) {
                    val currentTime = ZonedDateTime.now(ZoneId.systemDefault()).toEpochSecond()
                    val timeDifference = timestamp - currentTime
                    if (timeDifference > 0 && timeDifference <= 3600) {
                        val minutes = timeDifference / 60
                        val seconds = timeDifference % 60
                        matchStatusText = String.format(Locale.getDefault(), "%02d:%02d remaining", minutes, seconds)
                    } else {
                        matchStatusText = initialMatchStatusText
                    }
                    delay(60000) // Update every minute
                }
            }

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
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                            Modifier
                                .weight(1f)
                                .wrapContentHeight()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(text = response.fixture.status.short)
                            Text(
                                text = "${response.goals.home} - ${response.goals.away}",
                                fontSize = 20.sp
                            )

                            if (matchStatusText.isNotEmpty()) {
                                Text(
                                    text = matchStatusText,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }

                        }
                        Column(modifier = Modifier.weight(1f)) {
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
                        Modifier
                            .wrapContentWidth()
                            .wrapContentHeight()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            homeGoalScorers.forEach { (playerName, elapsed) ->
                                Scorers(
                                    playerName = playerName,
                                    elapsed = elapsed,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (homeGoalScorers.isNotEmpty() || awayGoalScorers.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.SportsSoccer,
                                    contentDescription = "Soccer Ball",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
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
