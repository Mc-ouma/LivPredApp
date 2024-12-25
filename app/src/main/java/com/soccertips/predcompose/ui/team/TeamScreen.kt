package com.soccertips.predcompose.ui.team


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predcompose.R
import com.soccertips.predcompose.model.lastfixtures.FixtureDetails
import com.soccertips.predcompose.model.standings.TeamStanding
import com.soccertips.predcompose.model.team.squad.Response
import com.soccertips.predcompose.model.team.teamscreen.TeamStatistics
import com.soccertips.predcompose.model.team.transfer.Response2
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.ui.fixturedetails.ErrorScreen
import com.soccertips.predcompose.viewmodel.SharedViewModel
import com.soccertips.predcompose.viewmodel.SharedViewModel.FixtureWithType
import com.soccertips.predcompose.viewmodel.TeamViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

enum class TeamStatisticsTab(val title: Int) {
    OVERVIEW(R.string.overview),
    FIXTURES(R.string.fixtures),
    SQUAD(R.string.squad),
    RESULTS(R.string.result),
    TRANSFERS(R.string.transfers),
    STANDINGS(R.string.standings)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    navController: NavController,
    teamId: String,
    viewModel: TeamViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel(LocalContext.current as ViewModelStoreOwner),
    leagueId: String,
    season: String,
    pages: Array<TeamStatisticsTab> = TeamStatisticsTab.entries.toTypedArray(),
) {
    val teamState by viewModel.team.collectAsState()
    val playersState by viewModel.players.collectAsState()
    val transfersState by viewModel.transfers.collectAsState()
    val lastFixturesState by sharedViewModel.fixturesState.collectAsState()
    val standingsState by sharedViewModel.standingsState.collectAsState()
    val nextFixturesState by viewModel.fixtures.collectAsState()

    var currentTab by remember { mutableStateOf(TeamStatisticsTab.OVERVIEW) }
    val teamInfoVisible = remember { mutableStateOf(true) } // Declare as MutableState<Boolean>

    LaunchedEffect(teamId) {
        if (!viewModel.isTeamDataLoaded) {
            viewModel.getTeams(leagueId, season, teamId)
        }
        if (!viewModel.isPlayersDataLoaded) {
            viewModel.getPlayers(teamId)
        }
        if (!viewModel.isTransfersDataLoaded) {
            viewModel.getTransfers(teamId)
        }
        if (!viewModel.isFixturesDataLoaded) {
            viewModel.getNextFixtures(season, teamId, "10")
        }
        if (!sharedViewModel.isStandingsDataLoaded) {
            sharedViewModel.fetchStandings(leagueId, season)
        }
        if (!sharedViewModel.isFixturesDataLoaded) {
            sharedViewModel.fetchFixtures(season, teamId, teamId, "10")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val leagueName = when (teamState) {
                        is UiState.Success -> (teamState as UiState.Success<TeamStatistics>).data.league.name
                        else -> "Team Statistics"
                    }
                    Text(
                        text = leagueName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (teamState) {
            is UiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    val density = LocalDensity.current
                    AnimatedVisibility(
                        visible = teamInfoVisible.value,
                        enter = slideInVertically { with(density) { -40.dp.roundToPx() } } + expandVertically(
                            expandFrom = Alignment.Top
                        ) + fadeIn(initialAlpha = 0.3f),
                        exit = slideOutVertically() + shrinkVertically() + fadeOut(),
                    ) {
                        TeamInfoCard(
                            statistics = (teamState as UiState.Success<TeamStatistics>).data,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    TeamsTab(
                        pages = pages,
                        navController = navController,
                        teamState = teamState,
                        playersState = playersState,
                        transfersState = transfersState,
                        lastFixturesState = lastFixturesState,
                        nextFixturesState = nextFixturesState,
                        standingsState = standingsState,
                        teamId = teamId,
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        onTabSelected = { currentTab = it },
                        teamInfoVisible = teamInfoVisible // Pass MutableState<Boolean>
                    )
                }
            }

            is UiState.Error -> {
                ErrorScreen(paddingValues)
            }

            UiState.Empty -> TODO()
            UiState.Idle -> TODO()
        }
    }
}

@Composable
fun TeamsTab(
    modifier: Modifier = Modifier,
    pages: Array<TeamStatisticsTab>,
    navController: NavController,
    teamState: UiState<TeamStatistics>,
    playersState: UiState<List<Response>>,
    transfersState: UiState<List<Response2>>,
    lastFixturesState: UiState<List<FixtureWithType>>,
    nextFixturesState: UiState<List<FixtureDetails>>,
    standingsState: UiState<List<TeamStanding>>,
    teamId: String,
    viewModel: TeamViewModel,
    onTabSelected: (TeamStatisticsTab) -> Unit,
    teamInfoVisible: MutableState<Boolean> // Accept MutableState<Boolean>
) {
    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        TeamsTabRow(
            pages,
            pagerState,
            coroutineScope,
            onTabSelected
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            val selectedTab = pages[page]

            when (selectedTab) {
                TeamStatisticsTab.OVERVIEW -> {
                    LazyContent(
                        onScroll = { visible ->
                            teamInfoVisible.value = visible // Update the state
                        }
                    ) {
                        ShowDataOrLoading(
                            state = teamState,
                            content = { statistics: TeamStatistics ->
                                TeamStatisticsContent(
                                    statistics = statistics,
                                    teamInfoVisible = teamInfoVisible,
                                ) { visible ->
                                    teamInfoVisible.value = visible
                                }
                            }
                        )
                    }
                    Timber.d("TeamScreen: TeamId: $teamId, TeamStatisticsTab: $selectedTab, TeamInfoVisible: ${teamInfoVisible.value}, Page: $page, PagerState: ${pagerState.currentPage}, statistics: $teamState")
                }

                TeamStatisticsTab.SQUAD -> {
                    LazyContent(
                        onScroll = { visible ->
                            teamInfoVisible.value = visible // Update the state
                        }
                    ) {
                        ShowDataOrLoading(
                            state = playersState,
                            content = { squadResponse: List<Response> ->
                                SquadScreen(
                                    squadResponse = squadResponse,
                                    teamInfoVisible.value
                                ) { visible ->
                                    teamInfoVisible.value = visible
                                }
                            })
                    }
                }

                TeamStatisticsTab.STANDINGS -> {
                    LazyContent(
                        onScroll = { visible ->
                            teamInfoVisible.value = visible // Update the state
                        }
                    ) {
                        ShowDataOrLoading(
                            state = standingsState,
                            content = { standings: List<TeamStanding> ->
                                FixtureStandings(
                                    standings = standings,
                                    teamId1 = teamId.toInt(),
                                    teamInfoVisible.value
                                ) { visible ->
                                    teamInfoVisible.value = visible
                                }
                            },
                        )
                    }
                }

                TeamStatisticsTab.TRANSFERS -> {
                    LazyContent(
                        onScroll = { visible ->
                            teamInfoVisible.value = visible // Update the state
                        }
                    ) {
                        ShowDataOrLoading(
                            state = transfersState,
                            content = { transfers: List<Response2> ->
                                TransferScreen(
                                    viewModel = viewModel,
                                    transfers = transfers,
                                    teamId = teamId,
                                    teamInfoVisible.value
                                ) { visible ->
                                    teamInfoVisible.value = visible
                                }
                            }
                        )
                    }
                }

                TeamStatisticsTab.FIXTURES -> {
                    LazyContent(
                        onScroll = { visible ->
                            teamInfoVisible.value = visible // Update the state
                        }
                    ) {
                        ShowDataOrLoading(
                            state = nextFixturesState,
                            content = { fixtures: List<FixtureDetails> ->
                                FixturesScreen(
                                    fixtures = fixtures,
                                    navController = navController,
                                ) { visible ->
                                    teamInfoVisible.value = visible
                                }
                            },
                        )
                    }
                }

                TeamStatisticsTab.RESULTS -> {
                    LazyContent(
                        onScroll = { visible ->
                            teamInfoVisible.value = visible // Update the state
                        }
                    ) {
                        ShowDataOrLoading(
                            state = lastFixturesState,
                            content = { fixtures: List<FixtureWithType> ->
                                ResultsScreen(
                                    fixtures = fixtures,
                                    navController = navController,
                                    homeTeamIdInt = teamId.toInt(),
                                    awayTeamIdInt = teamId.toInt(),
                                ) { visible ->
                                    teamInfoVisible.value = visible
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> ShowDataOrLoading(
    state: UiState<T>,
    content: @Composable (T) -> Unit,
    onRetry: () -> Unit = {}
) {
    when (state) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is UiState.Success -> {
            Timber.tag("ShowDataOrLoading").d("Data loaded successfully: ${state.data}")
            content(state.data)
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }

        UiState.Empty -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No data available", style = MaterialTheme.typography.bodySmall)
            }
        }

        UiState.Idle -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Waiting for input", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun LazyContent(
    lazyListState: LazyListState = rememberLazyListState(),
    onScroll: (Boolean) -> Unit, // Callback to update visibility of TeamInfoCard
    content: @Composable () -> Unit
) {
    var previousOffset by remember { mutableIntStateOf(0) }

    // Observe scroll state to hide/show the TeamInfoCard
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                // Determine if scrolling up or down
                if (index == 0) {
                    // At the top of the list
                    onScroll(true)
                } else {
                    if (offset < previousOffset) {
                        // Scrolling up
                        onScroll(true)
                    } else if (offset > previousOffset) {
                        // Scrolling down
                        onScroll(false)
                    }
                }
                previousOffset = offset
            }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        Timber.tag("LazyContent").d("Content loaded: ${lazyListState.firstVisibleItemIndex} - ${lazyListState.firstVisibleItemScrollOffset} - $previousOffset")
        item {
            content()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsTabRow(
    pages: Array<TeamStatisticsTab>,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    onTabSelected: (TeamStatisticsTab) -> Unit
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
    ) {
        pages.forEachIndexed { index, tab ->
            val title = stringResource(id = tab.title)
            val icon = when (tab) {
                TeamStatisticsTab.OVERVIEW -> Icons.Default.Home
                TeamStatisticsTab.FIXTURES -> Icons.Default.Schedule
                TeamStatisticsTab.SQUAD -> Icons.Default.Group
                TeamStatisticsTab.RESULTS -> Icons.Default.Checklist
                TeamStatisticsTab.TRANSFERS -> Icons.Default.SyncAlt
                TeamStatisticsTab.STANDINGS -> Icons.Default.StackedBarChart
            }
            Tab(
                selected = pagerState.currentPage == index,
                onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    onTabSelected(tab)
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = icon, contentDescription = title)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(title)
                    }
                },

                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }


}

@Composable
fun TeamInfoCard(statistics: TeamStatistics, modifier: Modifier = Modifier) {
    // Team Info Card
    Card(
        modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(statistics.team.logo),
                contentDescription = "Team Logo",
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statistics.team.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }


}

@Composable
fun Table(headers: List<String>, rows: List<List<String>>) {
    Column {
        // Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp)
                ),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Rows
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (index % 2 == 0) Color.LightGray else Color.Transparent),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

}