package com.soccertips.predcompose.ui.fixturedetails

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soccertips.predcompose.data.model.ResponseData
import com.soccertips.predcompose.data.model.events.FixtureEvent
import com.soccertips.predcompose.data.model.headtohead.FixtureDetails
import com.soccertips.predcompose.data.model.lineups.TeamLineup
import com.soccertips.predcompose.data.model.prediction.Response
import com.soccertips.predcompose.data.model.standings.TeamStanding
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.ui.components.ErrorMessage
import com.soccertips.predcompose.ui.components.LoadingIndicator
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureHeadToHeadScreen
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureLineupsScreen
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureMatchDetailsScreen
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureStandingsScreen
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureStatisticsScreen
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureSummaryScreen
import com.soccertips.predcompose.viewmodel.SharedViewModel
import timber.log.Timber

@Composable
fun FixtureMatchDetailsTab(
    fixturePredictionsState: UiState<List<Response>>,
    fixtureDetails: ResponseData,
    formState: UiState<List<SharedViewModel.FixtureWithType>>,
    navController: NavController,
) {
    when {
        formState is UiState.Loading || fixturePredictionsState is UiState.Loading -> {
            LoadingIndicator()
        }

        formState is UiState.Success && fixturePredictionsState is UiState.Success -> {
            FixtureMatchDetailsScreen(
                fixtures = formState.data,
                predictions = fixturePredictionsState.data[0].predictions,
                fixtureDetails = fixtureDetails,
                homeTeamId = fixtureDetails.teams.home.id.toString(),
                awayTeamId = fixtureDetails.teams.away.id.toString(),
                navController = navController
            )
            Timber.Forest.tag("FixtureMatchDetailsTab")
                .d("FixtureMatchDetailsTab: ${formState.data} ")
        }

        formState is UiState.Error -> {
            ErrorMessage(
                message = formState.message,
                onRetry = { /* Handle retry */ },
            )
        }

        else -> {
            Text(text = "😞 Error loading match details", color = Color.Companion.Red)
        }
    }
}

@Composable
fun FixtureStatisticsTab(fixtureStatsState: UiState<List<com.soccertips.predcompose.data.model.statistics.Response>>) {
    when (fixtureStatsState) {
        is UiState.Success -> {
            FixtureStatisticsScreen(statistics = fixtureStatsState.data)
        }

        is UiState.Loading -> {
            LoadingIndicator()
        }

        is UiState.Error -> {
            ErrorMessage(
                message = fixtureStatsState.message,
                onRetry = { /* Handle retry */ },
            )
        }

        else -> {
            Text(text = "No data available", color = Color.Companion.Gray)
        }
    }
}

@Composable
fun FixtureHeadToHeadTab(
    headToHeadState: UiState<List<FixtureDetails>>,
    navController: NavController
) {
    when (headToHeadState) {
        is UiState.Success -> {
            FixtureHeadToHeadScreen(
                headToHead = headToHeadState.data,
                navController = navController
            )
            Timber.Forest.tag("FixtureHeadToHeadTab")
                .d("FixtureHeadToHeadTab: ${headToHeadState.data}")
        }

        is UiState.Loading -> {
            LoadingIndicator()
        }

        is UiState.Error -> {
            ErrorMessage(
                message = headToHeadState.message,
                onRetry = { /* Handle retry */ },
            )
        }

        else -> {
            Text(text = "No data available", color = Color.Companion.Gray)
        }
    }
}

@Composable
fun FixtureLineupsTab(lineupsState: UiState<List<TeamLineup>>) {
    when (lineupsState) {
        is UiState.Success -> {
            if (lineupsState.data.size >= 2) {
                FixtureLineupsScreen(
                    lineups = Pair(lineupsState.data[0], lineupsState.data[1])
                )
            } else {
                Text(text = "No data available", color = Color.Companion.Gray, modifier = Modifier.Companion.padding(16.dp), textAlign = TextAlign.Center)
            }
        }

        is UiState.Loading -> {
            LoadingIndicator()
        }

        is UiState.Error -> {
            ErrorMessage(
                message = lineupsState.message,
                onRetry = { /* Handle retry */ },
            )
        }

        else -> {
            Text(text = "No data available", color = Color.Companion.Gray)
        }
    }
}

@Composable
fun FixtureStandingsTab(
    standingsState: UiState<List<TeamStanding>>,
    fixtureDetails: ResponseData,
) {
    when (standingsState) {
        is UiState.Success -> {
            FixtureStandingsScreen(
                standings = standingsState.data,
                teamId1 = fixtureDetails.teams.home.id,
                teamId2 = fixtureDetails.teams.away.id,
            )
            Timber.Forest.tag("FixtureStandingsTab")
                .d("FixtureStandingsTab: ${standingsState.data}")
        }

        is UiState.Loading -> {
            LoadingIndicator()
        }

        is UiState.Error -> {
            ErrorMessage(
                message = standingsState.message,
                onRetry = { /* Handle retry */ },
            )
        }

        else -> {
            Text(text = "No standings available", color = Color.Companion.Gray)
        }
    }
}

@Composable
fun FixtureSummaryTab(
    fixtureEventsState: UiState<List<FixtureEvent>>,
    fixtureDetails: ResponseData
) {
    when (fixtureEventsState) {
        is UiState.Success -> {
            FixtureSummaryScreen(
                events = fixtureEventsState.data,
                homeTeamId = fixtureDetails.teams.home.id,
                awayTeamId = fixtureDetails.teams.away.id
            )
            Timber.Forest.tag("FixtureSummaryTab")
                .d("FixtureSummaryTab: ${fixtureEventsState.data}")
        }

        is UiState.Loading -> {
            LoadingIndicator()
        }

        is UiState.Error -> {
            ErrorMessage(
                message = fixtureEventsState.message,
                onRetry = { /* Handle retry */ },
            )
        }

        else -> {
            Text(text = "No events available", color = Color.Companion.Gray)
        }
    }
}