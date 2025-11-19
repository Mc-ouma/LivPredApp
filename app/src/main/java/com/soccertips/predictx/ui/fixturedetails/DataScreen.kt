package com.soccertips.predictx.ui.fixturedetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soccertips.predictx.admob.RewardedAdManager
import com.soccertips.predictx.data.model.ResponseData
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.viewmodel.FixtureDetailsViewModel
import com.soccertips.predictx.viewmodel.SharedViewModel

@Composable
fun DataScreen(
    paddingValues: PaddingValues,
    showFixtureScore: Boolean,
    viewModel: FixtureDetailsViewModel,
    sharedViewModel: SharedViewModel,
    pages: Array<FixtureDetailsScreenPage>,
    formState: UiState<List<SharedViewModel.FixtureWithType>>,
    fixtureDetails: ResponseData,
    navController: NavController,
    rewardedAdManager: RewardedAdManager
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        FixtureDetailsTabs(
            modifier = Modifier.fillMaxSize(),
            pages = pages,
            formState = formState,
            fixtureDetails = fixtureDetails,
            viewModel = viewModel,
            sharedViewModel = sharedViewModel,
            navController = navController,
            rewardedAdManager = rewardedAdManager,
        )
    }
}