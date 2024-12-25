package com.soccertips.predcompose.ui.fixturedetails

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.soccertips.predcompose.Menu
import com.soccertips.predcompose.R
import com.soccertips.predcompose.model.ResponseData
import com.soccertips.predcompose.model.Team
import com.soccertips.predcompose.model.events.FixtureEvent
import com.soccertips.predcompose.model.headtohead.FixtureDetails
import com.soccertips.predcompose.model.lineups.TeamLineup
import com.soccertips.predcompose.model.standings.TeamStanding
import com.soccertips.predcompose.model.statistics.Response
import com.soccertips.predcompose.navigation.Routes
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.ui.components.ErrorMessage
import com.soccertips.predcompose.ui.components.LoadingIndicator
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureHeadToHeadScreen
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureLineupsScreen
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureMatchDetailsScreen
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureStandingsScreen
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureStatisticsScreen
import com.soccertips.predcompose.ui.fixturedetails.fixturedetailstab.FixtureSummaryScreen
import com.soccertips.predcompose.viewmodel.FixtureDetailsViewModel
import com.soccertips.predcompose.viewmodel.SharedViewModel
import kotlinx.coroutines.launch
import timber.log.Timber


enum class FixtureDetailsScreenPage(val titleResId: Int) {
    MATCH_DETAILS(R.string.match_details),
    STATISTICS(R.string.statistics),
    HEAD_TO_HEAD(R.string.head_to_head),
    LINEUPS(R.string.lineups),
    STANDINGS(R.string.standings),
    SUMMARY(R.string.summary),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixtureDetailsScreen(
    navController: NavController,
    fixtureId: String,
    sharedViewModel: SharedViewModel = hiltViewModel(LocalContext.current as ViewModelStoreOwner),
    viewModel: FixtureDetailsViewModel = hiltViewModel(),
    pages: Array<FixtureDetailsScreenPage> = FixtureDetailsScreenPage.entries.toTypedArray(),
) {
    // Collecting various states from the ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val fixtureStatsState by viewModel.fixtureStatsState.collectAsState()
    val fixtureEventsState by viewModel.fixtureEventsState.collectAsState()
    val fixturePredictionsState by viewModel.predictionsState.collectAsState()
    val formState by sharedViewModel.fixturesState.collectAsState()
    val headToHeadState by viewModel.headToHeadState.collectAsState()
    val lineupsState by viewModel.lineupsState.collectAsState()
    val standingsState by sharedViewModel.standingsState.collectAsState()


    // Fetch fixture details when fixtureId changes
    LaunchedEffect(fixtureId) {
        viewModel.fetchFixtureDetails(fixtureId)
        //  sharedViewModel.fetchFixtures(season = "2023",homeTeamId = "357", awayTeamId = "358", last = "10",)
    }

    // Fetch form and predictions when fixture details are available
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val fixtureDetails = (uiState as UiState.Success<ResponseData>).data
            viewModel.fetchFormAndPredictions(
                season = fixtureDetails.league.season.toString(),
                homeTeamId = fixtureDetails.teams.home.id.toString(),
                awayTeamId = fixtureDetails.teams.away.id.toString(),
                fixtureId = fixtureId,
                last = "6",
                leagueId = fixtureDetails.league.id.toString(),
            )
            sharedViewModel.fetchStandings(
                leagueId = fixtureDetails.league.id.toString(),
                season = fixtureDetails.league.season.toString()
            )
            sharedViewModel.fetchFixtures(
                season = fixtureDetails.league.season.toString(),
                homeTeamId = fixtureDetails.teams.home.id.toString(),
                awayTeamId = fixtureDetails.teams.away.id.toString(),
                last = "6",
            )
        }
    }

    // Set up the scrollable state for detecting scroll events
    val scrollState = rememberLazyListState()

    // Track visibility of FixtureScoreAndScorers
    val showFixtureScore by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex == 0
        }
    }

    // Set up scroll behavior for the collapsible TopAppBar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    val fixtureDetails = (uiState as? UiState.Success<ResponseData>)?.data
                    fixtureDetails?.let {
                        FixtureTopBarContent(showFixtureScore, it)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Menu()
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is UiState.Loading -> LoadingScreen(paddingValues)
            is UiState.Success -> DataScreen(
                paddingValues,
                scrollState,
                showFixtureScore,
                viewModel,
                pages,
                fixtureStatsState,
                fixtureEventsState,
                fixturePredictionsState,
                formState,
                headToHeadState,
                lineupsState,
                standingsState,
                (uiState as UiState.Success<ResponseData>).data,
                navController
            )

            is UiState.Error -> ErrorScreen(paddingValues)
            UiState.Idle, UiState.Empty -> EmptyScreen(paddingValues)
        }

        // Display "No data available" message for each empty state
        EmptyStateMessages(fixtureStatsState, fixtureEventsState, fixturePredictionsState)
    }
}

@Composable
fun FixtureTopBarContent(showFixtureScore: Boolean, fixtureDetails: ResponseData) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = !showFixtureScore,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = fixtureDetails.teams.home.logo),
                contentDescription = "Home Logo",
                modifier = Modifier.size(24.dp)
            )
        }
        AnimatedVisibility(
            visible = !showFixtureScore,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = fixtureDetails.goals.let { goals ->
                    "${goals.home} - ${goals.away}"
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        AnimatedVisibility(
            visible = !showFixtureScore,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = fixtureDetails.teams.away.logo),
                contentDescription = "Away Logo",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LoadingScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator()
    }
}

@Composable
fun DataScreen(
    paddingValues: PaddingValues,
    scrollState: LazyListState,
    showFixtureScore: Boolean,
    viewModel: FixtureDetailsViewModel,
    pages: Array<FixtureDetailsScreenPage>,
    fixtureStatsState: UiState<List<Response>>,
    fixtureEventsState: UiState<List<FixtureEvent>>,
    fixturePredictionsState: UiState<List<com.soccertips.predcompose.model.prediction.Response>>,
    formState: UiState<List<SharedViewModel.FixtureWithType>>,
    headToHeadState: UiState<List<FixtureDetails>>,
    lineupsState: UiState<List<TeamLineup>>,
    standingsState: UiState<List<TeamStanding>>,
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
                fixtureStatsState = fixtureStatsState,
                fixtureEventsState = fixtureEventsState,
                fixturePredictionsState = fixturePredictionsState,
                formState = formState,
                headToHeadState = headToHeadState,
                lineupsState = lineupsState,
                standingsState = standingsState,
                fixtureDetails = fixtureDetails,
                navController = navController,
            )
        }
    }
}

@Composable
fun ErrorScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "An unexpected error occurred....",
            color = Color.Red,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun EmptyScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No data available",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun EmptyStateMessages(
    fixtureStatsState: UiState<List<Response>>,
    fixtureEventsState: UiState<List<FixtureEvent>>,
    fixturePredictionsState: UiState<List<com.soccertips.predcompose.model.prediction.Response>>
) {
    if (fixtureStatsState is UiState.Empty) {
        Text(
            text = "No fixture stats available",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }

    if (fixtureEventsState is UiState.Empty) {
        Text(
            text = "No fixture events available",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }

    if (fixturePredictionsState is UiState.Empty) {
        Text(
            text = "No fixture predictions available",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FixtureDetailsTabs(
    modifier: Modifier = Modifier,
    pages: Array<FixtureDetailsScreenPage> = FixtureDetailsScreenPage.entries.toTypedArray(),
    fixtureDetails: ResponseData,
    fixtureStatsState: UiState<List<Response>>,
    fixtureEventsState: UiState<List<FixtureEvent>>,
    fixturePredictionsState: UiState<List<com.soccertips.predcompose.model.prediction.Response>>,
    formState: UiState<List<SharedViewModel.FixtureWithType>>,
    headToHeadState: UiState<List<FixtureDetails>>,
    lineupsState: UiState<List<TeamLineup>>,
    standingsState: UiState<List<TeamStanding>>,
    navController: NavController,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        // TabRow with PagerState
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 16.dp,
        ) {
            pages.forEachIndexed { index, page ->
                val title = stringResource(id = page.titleResId)
                val icon = when (page) {
                    FixtureDetailsScreenPage.MATCH_DETAILS -> Icons.Default.SportsSoccer
                    FixtureDetailsScreenPage.STATISTICS -> Icons.Default.BarChart
                    FixtureDetailsScreenPage.HEAD_TO_HEAD -> Icons.AutoMirrored.Default.CompareArrows
                    FixtureDetailsScreenPage.LINEUPS -> Icons.Default.People
                    FixtureDetailsScreenPage.STANDINGS -> Icons.AutoMirrored.Default.List
                    FixtureDetailsScreenPage.SUMMARY -> Icons.Default.Summarize
                }
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = icon, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = title)
                        }
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // HorizontalPager that syncs with TabRow
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
        ) { pageIndex ->
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    slideInHorizontally(animationSpec = tween(300)) + fadeIn() togetherWith fadeOut()
                },
                label = "Tab Transition"
            ) { targetPageIndex ->
                when (pages[targetPageIndex]) {
                    FixtureDetailsScreenPage.MATCH_DETAILS -> {
                        FixtureMatchDetailsTab(
                            formState = formState,
                            fixturePredictionsState = fixturePredictionsState,
                            fixtureDetails = fixtureDetails,
                            navController = navController
                        )

                    }

                    FixtureDetailsScreenPage.STATISTICS -> {
                        FixtureStatisticsTab(fixtureStatsState = fixtureStatsState)
                    }

                    FixtureDetailsScreenPage.HEAD_TO_HEAD -> {
                        FixtureHeadToHeadTab(
                            headToHeadState = headToHeadState,
                            navController = navController
                        )
                    }

                    FixtureDetailsScreenPage.LINEUPS -> {
                        FixtureLineupsTab(lineupsState = lineupsState)
                    }

                    FixtureDetailsScreenPage.STANDINGS -> {
                        FixtureStandingsTab(
                            standingsState = standingsState,
                            fixtureDetails = fixtureDetails,
                        )
                    }

                    FixtureDetailsScreenPage.SUMMARY -> {
                        FixtureSummaryTab(
                            fixtureEventsState = fixtureEventsState,
                            fixtureDetails = fixtureDetails
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FixtureMatchDetailsTab(
    fixturePredictionsState: UiState<List<com.soccertips.predcompose.model.prediction.Response>>,
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
            Timber.tag("FixtureMatchDetailsTab").d("FixtureMatchDetailsTab: ${formState.data} ")
        }

        formState is UiState.Error -> {
            ErrorMessage(
                message = formState.message,
                onRetry = { /* Handle retry */ },
            )
        }

        else -> {
            Text(text = "😞 Error loading match details", color = Color.Red)
        }
    }
}

@Composable
fun FixtureStatisticsTab(fixtureStatsState: UiState<List<Response>>) {
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
            Text(text = "No data available", color = Color.Gray)
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
            Timber.tag("FixtureHeadToHeadTab").d("FixtureHeadToHeadTab: ${headToHeadState.data}")
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
            Text(text = "No data available", color = Color.Gray)
        }
    }
}

@Composable
fun FixtureLineupsTab(lineupsState: UiState<List<TeamLineup>>) {
    when (lineupsState) {
        is UiState.Success -> {
            FixtureLineupsScreen(
                lineups = Pair(lineupsState.data[0], lineupsState.data[1])
            )
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
            Text(text = "No data available", color = Color.Gray)
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
            Timber.tag("FixtureStandingsTab").d("FixtureStandingsTab: ${standingsState.data}")
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
            Text(text = "No standings available", color = Color.Gray)
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
            Timber.tag("FixtureSummaryTab").d("FixtureSummaryTab: ${fixtureEventsState.data}")
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
            Text(text = "No events available", color = Color.Gray)
        }
    }
}

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
        is UiState.Loading -> {
            Text(
                text = "Loading...",
                modifier = Modifier.padding(16.dp),
                color = Color.Gray,
            )
        }

        is UiState.Success -> {
            val response = (uiState as UiState.Success<ResponseData>).data
            val homeTeamId = response.teams.home.id
            val awayTeamId = response.teams.away.id
            val homeGoalScorers = viewModel.getHomeGoalScorers(response.events, homeTeamId)
            val awayGoalScorers = viewModel.getAwayGoalScorers(response.events, awayTeamId)
            viewModel.formatTimestamp(response.fixture.timestamp)
            val matchStatusText =
                viewModel.getMatchStatusText(
                    response.fixture.status.short,
                    response.fixture.status.elapsed,
                    response.fixture.timestamp,
                )

            Card(
                modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            Row(
                                modifier =
                                Modifier
                                    .wrapContentHeight()
                                    .align(Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                response.score.let {
                                    Text(
                                        text = response.goals.home.toString(),
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = "-",
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                    )
                                    Text(
                                        text = response.goals.away.toString(),
                                        fontSize = 20.sp
                                    )
                                }
                            }
                            if (matchStatusText.isNotEmpty()) {
                                Text(
                                    text = matchStatusText,
                                    fontSize = 14.sp,
                                    color = Color(0xFF6200EE),
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

        is UiState.Error -> {
            Text(
                text = (uiState as UiState.Error).message,
                modifier = Modifier.padding(16.dp),
                color = Color.Red,
            )
        }

        UiState.Idle -> {
            // Do nothing
        }

        UiState.Empty -> TODO()
    }
}

@Composable
fun Scorers(
    playerName: String,
    elapsed: String,
) {
    Text(
        text = "$playerName $elapsed",
        fontSize = 14.sp,
        color = Color.Gray,
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun TeamColumn(
    team: Team,
    leagueId: String? = null,
    season: String? = null,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    Column(
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable {
                navController.navigate(
                    Routes.TeamDetails.createRoute(
                        team.id.toString(),
                        leagueId.toString(),
                        season.toString()
                    )
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlideImage(
            model = team.logo,
            failure = placeholder(R.drawable.placeholder),
            contentDescription = "Team Logo",
            modifier = Modifier.size(42.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = team.name,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1,
            fontSize = 16.sp,
        )
    }
}

