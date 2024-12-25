package com.soccertips.predcompose.ui.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
    fixtures: List<FixtureWithType>, navController: NavController, homeTeamIdInt: Int,
    awayTeamIdInt: Int, onScroll: (Boolean) -> Unit
) {
    val lazyListState = rememberLazyListState()

    // Observe scroll state to hide/show the page info
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                // Hide page info when scrolling up, show it when scrolling down
                onScroll(firstVisibleItemIndex == 0)
            }
    }

    // Page Info (conditionally visible based on scroll state)
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(fixtures) { response ->
                if (true) {
                    FixtureCard(
                        fixture = response.fixture,
                        isHome = response.isHome,
                        homeTeamIdInt = homeTeamIdInt,
                        awayTeamIdInt = awayTeamIdInt,
                        navController = navController
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/*

@Composable
fun TabsWithViewPagerAndPageInfo() {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val tabTitles = listOf("Tab 1", "Tab 2", "Tab 3")
    val coroutineScope = rememberCoroutineScope()

    // State to hold the current page info
    val pageInfo = remember {
        mutableStateOf("Page Info: ${tabTitles[pagerState.currentPage]}")
    }

    // Update the page info whenever the page changes
    LaunchedEffect(pagerState.currentPage) {
        pageInfo.value = "Page Info: ${tabTitles[pagerState.currentPage]}"
    }

    // State to control visibility of the page info
    var pageInfoVisible by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // Page Info (conditionally visible based on scroll state)
        val density = LocalDensity.current
        AnimatedVisibility(
            visible = pageInfoVisible,
            enter = slideInVertically {
                // Slide in from 40 dp from the top.
                with(density) { -40.dp.roundToPx() }
            } + expandVertically(
                // Expand from the top.
                expandFrom = Alignment.Top
            ) + fadeIn(
                // Fade in with the initial alpha of 0.3f.
                initialAlpha = 0.3f
            ),
            exit = slideOutVertically() + shrinkVertically() + fadeOut(),
        ) {
            Card() {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pageInfo.value,
                        color = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // Tabs
        LazyRow(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            items(tabTitles) { title ->
                Tab(
                    selected = pagerState.currentPage == tabTitles.indexOf(title),
                    onClick = {
                        coroutineScope.launch {
                            pagerState.scrollToPage(tabTitles.indexOf(title))
                        }
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // ViewPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) { page ->
            // Content for each page with LazyColumn
            when (page) {
                0 -> ScrollablePageContent(
                    "Page 1 Content",
                    Color.LightGray,
                ) { visible ->
                    pageInfoVisible = visible
                }

                1 -> ScrollablePageContent("Page 2 Content", Color.Cyan) { visible ->
                    pageInfoVisible = visible
                }

                2 -> ScrollablePageContent(
                    "Page 3 Content",
                    Color.Magenta
                ) { visible ->
                    pageInfoVisible = visible
                }
            }
        }
    }
}

@Composable
fun ScrollablePageContent(
    text: String,
    backgroundColor: Color,
    onPageInfoVisibilityChanged: (Boolean) -> Unit
) {
    val lazyListState = rememberLazyListState()

    // Observe scroll state to hide/show the page info
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                // Hide page info when scrolling up, show it when scrolling down
                onPageInfoVisibilityChanged(firstVisibleItemIndex == 0)
            }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(100) { index ->
            Text(
                text = "$text - Item $index",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = Color.Black
            )
        }
    }
}

@Preview
@Composable
private fun TabsWithViewPagerAndPageInfoPreview() {
    TabsWithViewPagerAndPageInfo()

}*/
