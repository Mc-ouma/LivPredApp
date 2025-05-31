package com.soccertips.predictx.ui.fixturedetails

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Summarize
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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.soccertips.predictx.Menu2
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.ResponseData
import com.soccertips.predictx.data.model.Team
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.FixtureDetailsUiState
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.viewmodel.FixtureDetailsViewModel
import com.soccertips.predictx.viewmodel.SharedViewModel
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
    val uiState by viewModel.uiState.collectAsState()
    val formState by sharedViewModel.fixturesState.collectAsState()

    // Track if initial data fetch has been performed
    var hasInitialFetch by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(fixtureId) {
        if (!hasInitialFetch) {
            Timber.d("Initial data fetch for fixture: $fixtureId")
            viewModel.fetchFixtureDetails(fixtureId)
            hasInitialFetch = true
        }
    }

    // Process fixture details when they become available
    LaunchedEffect(uiState) {
        when (uiState) {
            is FixtureDetailsUiState.Success -> {
                val fixtureDetails = (uiState as FixtureDetailsUiState.Success).fixtureDetails
                val season = fixtureDetails.league.season.toString()
                val homeTeamId = fixtureDetails.teams.home.id.toString()
                val awayTeamId = fixtureDetails.teams.away.id.toString()
                val leagueId = fixtureDetails.league.id.toString()
                val last = "10"

                // Parallelize independent data fetching for performance
                viewModel.fetchFormAndPredictions(fixtureId)
                sharedViewModel.fetchStandings(leagueId, season)
                sharedViewModel.fetchFixtures(season, homeTeamId, awayTeamId, last)

                // Fetch additional data needed for initial view
                viewModel.fetchFixtureStats(fixtureId, homeTeamId, awayTeamId)
            }
            is FixtureDetailsUiState.Loading -> {
                Timber.d("Loading fixture details...")
            }
            is FixtureDetailsUiState.Error -> {
                Timber.e("Error loading fixture details: ${(uiState as FixtureDetailsUiState.Error).message}")
            }
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
                    val fixtureDetails = (uiState as? FixtureDetailsUiState.Success)?.fixtureDetails
                    if (fixtureDetails != null) {
                        FixtureTopBarContent(showFixtureScore, fixtureDetails)
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
                    Menu2()
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is FixtureDetailsUiState.Loading -> LoadingScreen(paddingValues)
            is FixtureDetailsUiState.Success -> {
                val fixtureDetails = (uiState as FixtureDetailsUiState.Success).fixtureDetails
                DataScreen(
                    paddingValues = paddingValues,
                    scrollState = scrollState,
                    showFixtureScore = showFixtureScore,
                    viewModel = viewModel,
                    sharedViewModel = sharedViewModel,
                    pages = pages,
                    formState = formState,
                    fixtureDetails = fixtureDetails,
                    navController = navController,
                )
            }
            is FixtureDetailsUiState.Error -> ErrorScreen(
                paddingValues = paddingValues,
                message = "An error occurred. Please check your internet connection or try again later.",
                onRetry = {
                    viewModel.fetchFixtureDetails(fixtureId)
                }
            )
        }

        // Display "No data available" message for each empty state
        EmptyStateMessages(uiState = uiState)
    }

    // Cleanup resources when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            Timber.d("Cleaning up FixtureDetailsScreen resources")
            // Add any cleanup logic needed
        }
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
            Box(modifier = Modifier.size(24.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(fixtureDetails.teams.home.logo)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build()
                    ),
                    contentDescription = "Home Logo",
                    modifier = Modifier.size(24.dp)
                )
            }
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
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        AnimatedVisibility(
            visible = !showFixtureScore,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(fixtureDetails.teams.away.logo)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build()
                    ),
                    contentDescription = "Away Logo",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FixtureDetailsTabs(
    modifier: Modifier = Modifier,
    pages: Array<FixtureDetailsScreenPage> = FixtureDetailsScreenPage.entries.toTypedArray(),
    fixtureDetails: ResponseData,
    viewModel: FixtureDetailsViewModel,
    sharedViewModel: SharedViewModel,
    formState: UiState<List<SharedViewModel.FixtureWithType>>,
    navController: NavController,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    // Remember the current tab to avoid unnecessary data fetching
    var currentTab by remember { mutableStateOf(0) }

    Column(modifier = modifier) {
        // TabRow with PagerState
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            divider = { /* No divider */ }
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
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        // HorizontalPager that syncs with TabRow
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = true,
            key = { pages[it].name }
        ) { pageIndex ->
            // Only fetch data if moving to a different tab
            LaunchedEffect(pageIndex) {
                if (currentTab != pageIndex) {
                    currentTab = pageIndex

                    val fixtureId = fixtureDetails.fixture.id.toString()
                    val homeTeamId = fixtureDetails.teams.home.id.toString()
                    val awayTeamId = fixtureDetails.teams.away.id.toString()

                    when (pages[pageIndex]) {
                        FixtureDetailsScreenPage.STATISTICS -> {
                            viewModel.fetchFixtureStats(fixtureId, homeTeamId, awayTeamId)
                        }
                        FixtureDetailsScreenPage.HEAD_TO_HEAD -> {
                            viewModel.fetchHeadToHead(homeTeamId, awayTeamId)
                        }
                        FixtureDetailsScreenPage.LINEUPS -> {
                            viewModel.fetchLineups(fixtureId)
                        }
                        FixtureDetailsScreenPage.SUMMARY -> {
                            viewModel.fetchFixtureEvents(fixtureId)
                        }
                        else -> { /* No additional data needed */ }
                    }
                }
            }

            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    slideInHorizontally(animationSpec = tween(300)) { direction ->
                        direction * 30
                    } + fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "Tab Content Transition"
            ) { targetPageIndex ->
                // Render the appropriate tab content
                when (pages[targetPageIndex]) {
                    FixtureDetailsScreenPage.MATCH_DETAILS -> {
                        val predictionsState by viewModel.predictionsState.collectAsState()
                        FixtureMatchDetailsTab(
                            formState = formState,
                            fixturePredictionsState = predictionsState,
                            fixtureDetails = fixtureDetails,
                            navController = navController
                        )
                    }
                    FixtureDetailsScreenPage.STATISTICS -> {
                        val fixtureStatsState by viewModel.fixtureStatsState.collectAsState()
                        FixtureStatisticsTab(fixtureStatsState = fixtureStatsState)
                    }
                    FixtureDetailsScreenPage.HEAD_TO_HEAD -> {
                        val headToHeadState by viewModel.headToHeadState.collectAsState()
                        FixtureHeadToHeadTab(
                            headToHeadState = headToHeadState,
                            navController = navController
                        )
                    }
                    FixtureDetailsScreenPage.LINEUPS -> {
                        val lineupsState by viewModel.lineupsState.collectAsState()
                        FixtureLineupsTab(lineupsState = lineupsState)
                    }
                    FixtureDetailsScreenPage.STANDINGS -> {
                        val standingsState by sharedViewModel.standingsState.collectAsState()
                        FixtureStandingsTab(
                            standingsState = standingsState,
                            fixtureDetails = fixtureDetails
                        )
                    }
                    FixtureDetailsScreenPage.SUMMARY -> {
                        val fixtureEventsState by viewModel.fixtureEventsState.collectAsState()
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
fun Scorers(
    playerName: String,
    elapsed: String,
) {
    Text(
        text = "$playerName $elapsed",
        fontSize = 10.sp,
        color = Color.Gray,
    )
}

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
                if (!leagueId.isNullOrEmpty() && !season.isNullOrEmpty()) {
                    navController.navigate(
                        Routes.TeamDetails.createRoute(
                            team.id.toString(),
                            leagueId,
                            season
                        )
                    )
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(team.logo)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
            ),
            contentDescription = "${team.name} Logo",
            modifier = Modifier.size(42.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = team.name,
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun TeamColumnPrev() {
    TeamColumn(
        team = Team(
            id = 1,
            name = "Team A",
            logo = "https://example.com/logo.png",
            winner = true
        ),
        leagueId = "123",
        season = "2023",
        navController = NavController(LocalContext.current),
    )
}
