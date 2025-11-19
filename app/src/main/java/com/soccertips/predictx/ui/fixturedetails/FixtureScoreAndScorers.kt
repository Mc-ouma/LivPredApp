package com.soccertips.predictx.ui.fixturedetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soccertips.predictx.R
import com.soccertips.predictx.ui.FixtureDetailsUiState
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import com.soccertips.predictx.viewmodel.FixtureDetailsViewModel
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

@Composable
fun FixtureScoreAndScorers(
    viewModel: FixtureDetailsViewModel,
    modifier: Modifier = Modifier,
    navController: NavController,
    leagueId: String,
    season: String,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    when (uiState) {
        is FixtureDetailsUiState.Loading -> {
            Text(
                text = stringResource(R.string.loading),
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
                        matchStatusText = context.getString(R.string.time_format, minutes, seconds)
                    } else {
                        matchStatusText = initialMatchStatusText
                    }
                    delay(1000) // Update every second
                }
            }

            Card(
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = cardColors,
                elevation = cardElevation,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header with match status
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = response.fixture.status.short,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                            if (matchStatusText.isNotEmpty()) {
                                Text(
                                    text = matchStatusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    // Teams and Score Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
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

                        // Score in center
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .weight(0.7f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "${response.goals.home} - ${response.goals.away}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )
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

                    // Goal Scorers Section
                    if (homeGoalScorers.isNotEmpty() || awayGoalScorers.isNotEmpty()) {
                        androidx.compose.material3.Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
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
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsSoccer,
                                        contentDescription = stringResource(R.string.soccer_ball),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp),
                                    )
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
            }
        }

        is FixtureDetailsUiState.Error -> {
            ErrorScreen(
                paddingValues = PaddingValues(0.dp),
                message = stringResource(R.string.no_data_available),
                // (uiState as FixtureDetailsUiState.Error).message
                onRetry = {}
            )
        }
    }
}

@Composable
fun Scorers(
    playerName: String,
    elapsed: String,
) {
    Text(
        text = "$playerName $elapsed'",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    )
}