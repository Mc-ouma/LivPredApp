package com.soccertips.predcompose.ui.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureCard
import com.soccertips.predcompose.viewmodel.SharedViewModel.FixtureWithType

@Composable
fun ResultsScreen(
    fixtures: List<FixtureWithType>,
    navController: NavController,
    homeTeamIdInt: Int,
    awayTeamIdInt: Int,
    onScroll: (Boolean) -> Unit
) {
    val lazyListState = rememberLazyListState()

    // Observe scroll state to hide/show the team info card
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                // Hide team info card when scrolling up, show it when scrolling down
                onScroll(firstVisibleItemIndex == 0)
            }
    }

    // Display the list of fixtures
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(fixtures) { fixture ->
            FixtureCard(
                fixture = fixture.fixture,
                isHome = fixture.isHome,
                homeTeamIdInt = homeTeamIdInt,
                awayTeamIdInt = awayTeamIdInt,
                navController = navController
            )
            HorizontalDivider()
        }
    }
}

