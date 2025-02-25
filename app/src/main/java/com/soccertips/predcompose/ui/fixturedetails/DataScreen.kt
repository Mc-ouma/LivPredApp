package com.soccertips.predcompose.ui.fixturedetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soccertips.predcompose.data.model.ResponseData
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.viewmodel.FixtureDetailsViewModel
import com.soccertips.predcompose.viewmodel.SharedViewModel

@Composable
fun DataScreen(
    paddingValues: PaddingValues,
    scrollState: LazyListState,
    showFixtureScore: Boolean,
    viewModel: FixtureDetailsViewModel,
    sharedViewModel: SharedViewModel,
    pages: Array<FixtureDetailsScreenPage>,
    formState: UiState<List<SharedViewModel.FixtureWithType>>,
    fixtureDetails: ResponseData,
    navController: NavController
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        state = scrollState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            AnimatedVisibility(
                visible = showFixtureScore,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                FixtureScoreAndScorers(
                    viewModel = viewModel,
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentHeight()
                        .fillMaxWidth(),
                    navController = navController,
                    leagueId = fixtureDetails.league.id.toString(),
                    season = fixtureDetails.league.season.toString(),
                )
            }
        }

        item {
            FixtureDetailsTabs(
                modifier = Modifier.fillMaxWidth(),
                pages = pages,
                formState = formState,
                fixtureDetails = fixtureDetails,
                viewModel = viewModel,
                sharedViewModel = sharedViewModel,
                navController = navController
            )
        }
    }
}