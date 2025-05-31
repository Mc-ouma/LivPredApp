package com.soccertips.predictx.ui.team


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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.rememberAsyncImagePainter
import com.soccertips.predictx.data.model.lastfixtures.FixtureDetails
import com.soccertips.predictx.data.model.standings.TeamStanding
import com.soccertips.predictx.data.model.team.teamscreen.Response
import com.soccertips.predictx.data.model.team.teamscreen.TeamData
import com.soccertips.predictx.data.model.team.teamscreen.TeamStatistics
import com.soccertips.predictx.data.model.team.teamscreen.Venue
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.ui.fixturedetails.EmptyScreen
import com.soccertips.predictx.ui.fixturedetails.ErrorScreen
import com.soccertips.predictx.ui.theme.LocalCardColors
import com.soccertips.predictx.ui.theme.LocalCardElevation
import com.soccertips.predictx.ui.theme.PredictXTheme
import com.soccertips.predictx.viewmodel.SharedViewModel
import com.soccertips.predictx.viewmodel.SharedViewModel.FixtureWithType
import com.soccertips.predictx.viewmodel.TeamViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import com.soccertips.predictx.R

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
    val lastFixturesState by sharedViewModel.fixturesState.collectAsStateWithLifecycle()
    val standingsState by sharedViewModel.standingsState.collectAsStateWithLifecycle()
    val nextFixturesState by viewModel.fixtures.collectAsStateWithLifecycle()

    val isTeamDataLoading by viewModel.isTeamDataLoading.collectAsStateWithLifecycle()
    val isPlayersLoading by viewModel.isPlayersLoading.collectAsStateWithLifecycle()
    val isFixturesLoading by viewModel.isFixturesLoading.collectAsStateWithLifecycle()
    val isTeamLoading by viewModel.isTeamLoading.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(TeamStatisticsTab.OVERVIEW) }
    val teamInfoVisible = remember { mutableStateOf(true) } // Declare as MutableState<Boolean>

    LaunchedEffect(teamId) {
        coroutineScope {
            val teamDataDeferred = async {
                if (!viewModel.isTeamDataLoaded) {
                    viewModel.getTeams(leagueId, season, teamId)
                }
            }
            val teamDeferred = async {
                if (!viewModel.isTeamDataLoaded) {
                    viewModel.getTeamData(teamId.toInt())
                }
            }
            val playersDeferred = async {
                if (!viewModel.isPlayersDataLoaded) {
                    viewModel.getPlayers(teamId)
                }
            }
            val transfersDeferred = async {
                if (!viewModel.isTransfersDataLoaded) {
                    viewModel.getTransfers(teamId)
                }
            }
            val nextFixturesDeferred = async {
                if (!viewModel.isFixturesDataLoaded) {
                    viewModel.getNextFixtures(season, teamId, "10")
                }
            }
            val standingsDeferred = async {
                if (!sharedViewModel.isStandingsDataLoaded) {
                    sharedViewModel.fetchStandings(leagueId, season)
                }
            }
            val fixturesDeferred = async {
                if (!sharedViewModel.isFixturesDataLoaded) {
                    sharedViewModel.fetchFixtures(season, teamId, teamId, "10")
                }
            }

            awaitAll(
                teamDataDeferred,
                teamDeferred,
                playersDeferred,
                transfersDeferred,
                nextFixturesDeferred,
                standingsDeferred,
                fixturesDeferred
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    when (val state = teamDataState) {
                        is UiState.Success<*> -> {
                            val data = state.data
                            if (data is Response) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Team Logo
                                    Image(
                                        painter = rememberAsyncImagePainter(model = data.team.logo),
                                        contentDescription = "Team Logo",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Team Name and Country
                                    Column {
                                        Text(
                                            text = data.team.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = data.team.country,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Team Info",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        is UiState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isTeamDataLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Loading Team Info...",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                } else {
                                    Text(
                                        text = "Team Info",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                        is UiState.Error -> Text(
                            text = "Error Loading Team",
                            style = MaterialTheme.typography.titleMedium
                        )
                        UiState.Empty -> Text(
                            text = "No Team Data",
                            style = MaterialTheme.typography.titleMedium
                        )
                        is UiState.ShowSnackbar -> Text(
                            text = "Team Info",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
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
        if (isTeamDataLoading || isPlayersLoading || isFixturesLoading || isTeamLoading) {

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator()
            }


        } else {
            when (teamDataState) {
                is UiState.Loading ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                    }

                is UiState.Success ->

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
                                (teamDataState as? UiState.Success<List<Response>>)?.data?.firstOrNull()

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
                            lastFixturesState = lastFixturesState,
                            nextFixturesState = nextFixturesState,
                            standingsState = standingsState,
                            teamId = teamId,
                            viewModel = viewModel,
                            onTabSelected = { currentTab = it },
                            teamInfoVisible = teamInfoVisible // Pass MutableState<Boolean>
                        )
                    }


                is UiState.Error ->
                    ErrorScreen(paddingValues,
                        "An error occurred. Please check your internet or check again later",
                        //(teamDataState as UiState.Error).message
                        onRetry = {
                            viewModel.getTeams(leagueId, season, teamId)
                            viewModel.getTeamData(teamId.toInt())
                            viewModel.getPlayers(teamId)
                            viewModel.getTransfers(teamId)
                            viewModel.getNextFixtures(season, teamId, "10")
                            sharedViewModel.fetchStandings(leagueId, season)
                            sharedViewModel.fetchFixtures(season, teamId, teamId, "10")
                        }
                    )


                UiState.Empty ->
                    EmptyScreen(paddingValues)

                else -> Unit

            }
        }
    }
}

@Composable
fun TeamsTab(
    pages: Array<TeamStatisticsTab>,
    navController: NavController,
    teamState: UiState<TeamStatistics>,
    playersState: UiState<List<com.soccertips.predictx.data.model.team.squad.Response>>,
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

    val transfersFlow = viewModel.getTransfers(teamId)
    val transfers = transfersFlow.collectAsLazyPagingItems()

    // Remember the current tab selection and prevent recomposition
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    //Lift the scroll state up to the parent composable
    val lazyListState = rememberLazyListState()

    // Observe scroll state to hide/show the team info card
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                // Hide team info card when scrolling up, show it when scrolling down
                teamInfoVisible.value = firstVisibleItemIndex == 0
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row
        TeamsTabRow(
            pages = pages,
            pagerState = pagerState,
            coroutineScope = coroutineScope,
            onTabSelected = currentOnTabSelected
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
                                lazyListState = lazyListState
                            )
                        }
                    )
                }

                TeamStatisticsTab.SQUAD -> {
                    ShowDataOrLoading(
                        state = playersState,
                        content = { squadResponse: List<com.soccertips.predictx.data.model.team.squad.Response> ->
                            SquadScreen(
                                squadResponse = squadResponse,
                                lazyListState = lazyListState
                            )
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
                                lazyListState = lazyListState
                            )
                        }
                    )
                }

                TeamStatisticsTab.TRANSFERS -> {
                    TransferScreen(
                        transfers = transfers,
                        teamId = teamId,
                        lazyListState = lazyListState
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
                                lazyListState = lazyListState
                            )
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
                                teamId = teamId.toInt(),
                                lazyListState = lazyListState
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
    content: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Loading ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }


        is UiState.Success -> {
            Timber.tag("ShowDataOrLoading").d("Data loaded successfully: ${state.data}")
            content(state.data)
        }

        is UiState.Error ->
            ErrorScreen(paddingValues = PaddingValues(0.dp),
                message = "An error occurred. Please check your internet or check again later",
                //state.message
                onRetry = {

                }
                )


        UiState.Empty ->
            EmptyScreen(paddingValues = PaddingValues(0.dp))

        else -> Unit

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
    statistics: Response,
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
                    color = MaterialTheme.colorScheme.onPrimary,
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
                    .background(if (index % 2 == 0) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
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
    val sampleStatistics = Response(
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
        PredictXTheme {
            TeamInfoCard(statistics = sampleStatistics)
        }
    }
}