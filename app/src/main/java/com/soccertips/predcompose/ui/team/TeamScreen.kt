package com.soccertips.predcompose.ui.team


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predcompose.R
import com.soccertips.predcompose.model.lastfixtures.FixtureDetails
import com.soccertips.predcompose.model.standings.TeamStanding
import com.soccertips.predcompose.model.team.squad.Response
import com.soccertips.predcompose.model.team.teamscreen.TeamData
import com.soccertips.predcompose.model.team.teamscreen.TeamStatistics
import com.soccertips.predcompose.model.team.teamscreen.Venue
import com.soccertips.predcompose.model.team.transfer.Response2
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.ui.fixturedetails.ErrorScreen
import com.soccertips.predcompose.ui.theme.LocalCardColors
import com.soccertips.predcompose.ui.theme.LocalCardElevation
import com.soccertips.predcompose.ui.theme.PredComposeTheme
import com.soccertips.predcompose.viewmodel.SharedViewModel
import com.soccertips.predcompose.viewmodel.SharedViewModel.FixtureWithType
import com.soccertips.predcompose.viewmodel.TeamViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

enum class TeamStatisticsTab(@StringRes val title: Int) {
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
    val teamDataState by viewModel.teamData.collectAsStateWithLifecycle()
    val teamState by viewModel.team.collectAsStateWithLifecycle()
    val playersState by viewModel.players.collectAsStateWithLifecycle()
    val transfersState by viewModel.transfers.collectAsStateWithLifecycle()
    val lastFixturesState by sharedViewModel.fixturesState.collectAsStateWithLifecycle()
    val standingsState by sharedViewModel.standingsState.collectAsStateWithLifecycle()
    val nextFixturesState by viewModel.fixtures.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(TeamStatisticsTab.OVERVIEW) }
    val teamInfoVisible = remember { mutableStateOf(true) } // Declare as MutableState<Boolean>

    LaunchedEffect(teamId) {
        if (!viewModel.isTeamDataLoaded) {
            viewModel.getTeams(leagueId, season, teamId)
        }
        if (!viewModel.isTeamDataLoaded) {
            viewModel.getTeamData(teamId.toInt())
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
                    val countryName = when (val state = teamDataState) {
                        is UiState.Success<*> -> {
                            val data = state.data
                            if (data is com.soccertips.predcompose.model.team.teamscreen.Response) {
                                data.team.country
                            } else {
                                "Team Info"
                            }
                        }

                        is UiState.Loading -> "Loading..."
                        is UiState.Error -> "Error"
                        UiState.Empty -> "No Data"
                        UiState.Idle -> "Idle"
                    }
                    Text(
                        text = countryName,
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
        when (teamDataState) {
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
                        val teamInfoCardData =
                            (teamDataState as? UiState.Success<List<com.soccertips.predcompose.model.team.teamscreen.Response>>)?.data?.firstOrNull()

                        teamInfoCardData?.let { response ->
                            TeamInfoCard(
                                statistics = response,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
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
    teamInfoVisible: MutableState<Boolean>
) {
    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row
        TeamsTabRow(
            pages = pages,
            pagerState = pagerState,
            coroutineScope = coroutineScope,
            onTabSelected = onTabSelected
        )

        // Horizontal Pager for Tab Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            val selectedTab = pages[page]

            when (selectedTab) {
                TeamStatisticsTab.OVERVIEW -> {
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

                TeamStatisticsTab.SQUAD -> {
                    ShowDataOrLoading(
                        state = playersState,
                        content = { squadResponse: List<Response> ->
                            SquadScreen(
                                squadResponse = squadResponse,
                            ) { visible ->
                                teamInfoVisible.value = visible
                            }
                        }
                    )
                }

                TeamStatisticsTab.STANDINGS -> {
                    ShowDataOrLoading(
                        state = standingsState,
                        content = { standings: List<TeamStanding> ->
                            FixtureStandings(
                                standings = standings,
                                teamId1 = teamId.toInt(),
                            ) { visible ->
                                teamInfoVisible.value = visible
                            }
                        }
                    )
                }

                TeamStatisticsTab.TRANSFERS -> {
                    ShowDataOrLoading(
                        state = transfersState,
                        content = { transfers: List<Response2> ->
                            TransferScreen(
                                viewModel = viewModel,
                                transfers = transfers,
                                teamId = teamId,
                            ) { visible ->
                                teamInfoVisible.value = visible
                            }
                        }
                    )
                }

                TeamStatisticsTab.FIXTURES -> {
                    ShowDataOrLoading(
                        state = nextFixturesState,
                        content = { fixtures: List<FixtureDetails> ->
                            FixturesScreen(
                                fixtures = fixtures,
                                navController = navController,
                                viewModel = viewModel,
                            ) { visible ->
                                teamInfoVisible.value = visible
                            }
                        }
                    )
                }

                TeamStatisticsTab.RESULTS -> {
                    ShowDataOrLoading(
                        state = lastFixturesState,
                        content = { fixtures: List<FixtureWithType> ->
                            ResultsScreen(
                                fixtures = fixtures,
                                navController = navController,
                                homeTeamIdInt = teamId.toInt(),
                                awayTeamIdInt = teamId.toInt(),
                                onScroll = { visible ->
                                    teamInfoVisible.value = visible
                                }
                            )
                        }
                    )
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(imageVector = icon, contentDescription = title)
                        Spacer(modifier = Modifier.height(4.dp))
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
fun TeamInfoCard(
    statistics: com.soccertips.predcompose.model.team.teamscreen.Response,
    modifier: Modifier = Modifier
) {
    val stadiumDetailsVisible = remember { mutableStateOf(false) }
    val cardColors = LocalCardColors.current
    val cardElevation = LocalCardElevation.current
    Card(
        colors = cardColors,
        elevation = cardElevation,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Team Logo and Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = rememberAsyncImagePainter(statistics.team.logo),
                    contentDescription = "Team Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = statistics.team.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Founded: ${statistics.team.founded}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Country: ${statistics.team.country}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { stadiumDetailsVisible.value = !stadiumDetailsVisible.value },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (stadiumDetailsVisible.value) "Hide Stadium Details" else "Show Stadium Details"
                )
            }
            AnimatedVisibility(visible = stadiumDetailsVisible.value) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    GridLayout(items = listOf(
                        {
                            TeamInfoItem(
                                icon = Icons.Default.LocationOn,
                                title = "Stadium",
                                value = statistics.venue.name
                            )
                        },
                        {
                            TeamInfoItem(
                                icon = Icons.Default.LocationCity,
                                title = "City",
                                value = statistics.venue.city
                            )
                        },
                        {
                            TeamInfoItem(
                                icon = Icons.Default.Home,
                                title = "Address",
                                value = statistics.venue.address
                            )
                        },
                        {
                            TeamInfoItem(
                                icon = Icons.Default.Grass,
                                title = "Surface",
                                value = statistics.venue.surface
                            )
                        },
                        {
                            TeamInfoItem(
                                icon = Icons.Default.People,
                                title = "Capacity",
                                value = statistics.venue.capacity.toString()
                            )
                        }
                    ))
                }
            }
        }
    }
}

@Composable
fun GridLayout(items: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp, 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items.size) { index ->
            items[index]()
        }
    }
}

@Composable
fun TeamInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun Table(headers: List<String>, rows: List<List<String>>) {
    Column(
        modifier = Modifier.border(
            2.dp,
            MaterialTheme.colorScheme.primary,
            RoundedCornerShape(8.dp)
        )
    ) {
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

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun PreviewTeamInfoCard() {
    val sampleStatistics = com.soccertips.predcompose.model.team.teamscreen.Response(
        team = TeamData(
            name = "Manchester United",
            founded = 1878,
            country = "England",
            logo = "https://upload.wikimedia.org/wikipedia/en/7/7a/Manchester_United_FC_crest.svg",
            id = 33,
            national = false,
            code = "MUN"
        ),
        venue = Venue(
            name = "Old Trafford",
            city = "Manchester",
            address = "Sir Matt Busby Way",
            surface = "Grass",
            capacity = 74310,
            id = 1,
            image = "https://upload.wikimedia.org/wikipedia/commons/2/2a/Old_Trafford_inside_20060726_1.jpg"
        )
    )

    MaterialTheme {
        PredComposeTheme {
            TeamInfoCard(statistics = sampleStatistics)
        }
    }
}