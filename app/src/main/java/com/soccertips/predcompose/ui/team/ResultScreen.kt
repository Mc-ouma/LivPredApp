package com.soccertips.predcompose.ui.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureCard
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.TeamId
import com.soccertips.predcompose.viewmodel.SharedViewModel.FixtureWithType
import timber.log.Timber

@Composable
fun ResultsScreen(
    fixtures: List<FixtureWithType>,
    navController: NavController,
    teamId: Int,
    lazyListState: LazyListState,
) {

    Timber.d("ResultsScreen: $fixtures  homeTeamIdInt: $teamId")

    // Filter fixtures based on isHome property and team ID
    val filteredFixtures = fixtures.filter { fixtureWithType ->
        (fixtureWithType.specialId.toInt() == teamId)
    }

    // Display the list of fixtures
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(filteredFixtures) { fixture ->
            FixtureCard(
                fixture = fixture.fixture,
                isHome = fixture.isHome,
                teamId = TeamId(teamId, teamId),
                navController = navController
            )
            HorizontalDivider()
        }
        item {
            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

