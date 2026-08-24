package com.soccertips.predictx.ui.items

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.soccertips.predictx.Menu
import com.soccertips.predictx.R
import com.soccertips.predictx.admob.CollapsibleBannerAdView
import com.soccertips.predictx.admob.InterstitialAdManager
import com.soccertips.predictx.admob.NativeAdItem
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.ui.components.DateUtils
import com.soccertips.predictx.ui.components.ErrorMessage
import com.soccertips.predictx.ui.components.LoadingIndicator
import com.soccertips.predictx.viewmodel.ItemsListViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import timber.log.Timber
import androidx.compose.ui.platform.LocalLocale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdManagerEntryPoint {
    fun interstitialAdManager(): InterstitialAdManager
}

// Extension function to safely find the Activity from any Context
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsListScreen(
    navController: NavController,
    categoryId: String,
    categories: List<Category>,
    viewModel: ItemsListViewModel = hiltViewModel(),
) {
    // Access the interstitial ad manager via LocalContext and get the application context
    val context = LocalContext.current
    val interstitialAdManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AdManagerEntryPoint::class.java
        )
            .interstitialAdManager()
    }

    // Get the real-time ad readiness state
    val isAdReady by interstitialAdManager.isAdReady.collectAsState()

    // Ensure the activity context is set on the interstitial ad manager
    // and preload interstitial ad if not already loaded
    LaunchedEffect(Unit) {
        val activity = context.findActivity()
        if (activity != null) {
            interstitialAdManager.setActivityContext(activity)

            // Only trigger load if ad is not ready and not currently loading
            if (!interstitialAdManager.isAdReady.value && !interstitialAdManager.isCurrentlyLoading()) {
                Timber.tag("InterstitialAd").d("ItemsListScreen: Ad not ready, triggering load")
                interstitialAdManager.forceLoadAd()
            } else {
                Timber.tag("InterstitialAd").d("ItemsListScreen: Ad already ready or loading")
            }

            Timber.d("InterstitialAdManager activity context set in ItemsListScreen")
        } else {
            Timber.w("Could not find Activity context in ItemsListScreen")
        }
    }

    // Log ad readiness changes for debugging
    LaunchedEffect(isAdReady) {
        Timber.tag("InterstitialAd").d("ItemsListScreen: Ad ready state changed to $isAdReady")
    }
    val category =
        remember(categoryId) { categories.find { it.url == categoryId } } ?: categories.first()

    // Check for pending navigation date from betting success notification
    val activity = context.findActivity()
    val sharedPrefs = activity?.getSharedPreferences("predictx_prefs", Context.MODE_PRIVATE)
    val pendingNavigationDate = sharedPrefs?.getString("pending_navigation_date", null)

    // Date range: 5 days back + today + tomorrow (if available)
    val today = LocalDate.now()
    val pastDays = 5
    val todayPageIndex = pastDays // page 5 = today

    val tomorrowHasItems by viewModel.tomorrowHasItems.collectAsState()
    val pageCount = todayPageIndex + 1 + (if (tomorrowHasItems) 1 else 0)

    // Map page index to date
    fun pageToDate(page: Int): LocalDate = today.minusDays((todayPageIndex - page).toLong())

    // Determine initial page based on pending navigation date
    val initialPage = if (!pendingNavigationDate.isNullOrEmpty()) {
        try {
            val parsedDate = LocalDate.parse(pendingNavigationDate)
            sharedPrefs?.edit()?.remove("pending_navigation_date")?.apply()
            val daysFromToday = ChronoUnit.DAYS.between(today, parsedDate).toInt()
            (todayPageIndex + daysFromToday).coerceIn(0, pageCount - 1)
        } catch (e: Exception) {
            Timber.w("Failed to parse pending navigation date: $pendingNavigationDate")
            todayPageIndex
        }
    } else {
        todayPageIndex
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount }
    )

    // Check if tomorrow has items
    LaunchedEffect(category) {
        viewModel.checkTomorrowItems(category.url)
    }

    // Determine the selected date based on current page
    val selectedDate = pageToDate(pagerState.currentPage)
    val formattedDate = DateUtils.formatRelativeDate(context, selectedDate.toString())
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Fetch items when page changes
    LaunchedEffect(category, pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.fetchItems(category.url, pageToDate(page))
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Function to handle back navigation with an interstitial ad
    val navigateBackWithAd: () -> Unit = {
        // Show interstitial ad on back navigation
        val isAdCurrentlyLoading = interstitialAdManager.isCurrentlyLoading()
        Timber.tag("InterstitialAd")
            .d(
                "Back navigation: isAdReady = $isAdReady, isAdLoading = $isAdCurrentlyLoading"
            )
        if (isAdReady && !isAdCurrentlyLoading) {
            try {
                val activity = navController.context as? Activity
                activity?.let {
                    Timber.tag("InterstitialAd")
                        .d("Showing interstitial ad for back navigation")
                    interstitialAdManager.showInterstitialAdWithCallback(
                        it,
                        onAdDismissed = {
                            // Navigate back after ad is dismissed
                            Timber.tag("InterstitialAd").d("Ad dismissed, navigating back")
                            navController.popBackStack()
                        }
                    )
                }
                    ?: run {
                        Timber.tag("InterstitialAd")
                            .w("Activity is null, fallback navigation")
                        navController.popBackStack() // Fallback if activity is null
                    }
            } catch (e: Exception) {
                Timber.tag("InterstitialAd").e("Error showing ad: ${e.message}")
                e.printStackTrace()
                navController.popBackStack() // Fallback on error
            }
        } else {
            // If no ad is ready, just navigate back
            if (isAdCurrentlyLoading) {
                Timber.tag("InterstitialAd")
                    .d("Ad is loading, direct navigation without waiting")
            } else {
                Timber.tag("InterstitialAd").d("No ad ready, direct navigation")
            }
            navController.popBackStack()
        }
    }

    // Handle system back gesture
    BackHandler { navigateBackWithAd() }

    Scaffold(
        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        val canSwipeLeft = pagerState.currentPage > 0
                        val canSwipeRight = pagerState.currentPage < pageCount - 1
                        val activeColor = MaterialTheme.colorScheme.onSurface
                        val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        val leftColor = if (canSwipeLeft) activeColor else inactiveColor
                        val rightColor = if (canSwipeRight) activeColor else inactiveColor

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (canSwipeLeft) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
                                enabled = canSwipeLeft
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous Day",
                                    tint = leftColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = {
                                    if (canSwipeRight) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                enabled = canSwipeRight
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next Day",
                                    tint = rightColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = navigateBackWithAd) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        Menu(
                            onNavigateToSubscription = {
                                navController.navigate(com.soccertips.predictx.navigation.Routes.Subscription.route)
                            }
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )

                // Interactive Horizontal Date Tab Selector Bar
                DateSelectorRow(
                    pagerState = pagerState,
                    pageCount = pageCount,
                    pageToDate = ::pageToDate,
                    onDateSelected = { targetPage ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                )
            }
        },
        bottomBar = {
            CollapsibleBannerAdView(
                onUpgradeClick = { navController.navigate(Routes.Subscription.route) }
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) { page ->
            val pageDate = pageToDate(page)

            val dateStates by viewModel.dateUiStates.collectAsState()
            val pageUiState: UiState<List<com.soccertips.predictx.data.model.ServerResponse>> =
                dateStates[pageDate] ?: UiState.Loading

            Box(modifier = Modifier.fillMaxSize()) {
                when (pageUiState) {
                    is UiState.Loading -> {
                        LoadingIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is UiState.Error -> {
                        ErrorMessage(
                            message =
                                stringResource(
                                    R.string.failed_to_fetch_games_please_try_again_later
                                ),
                            onRetry = { viewModel.fetchItems(category.url, pageDate) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    is UiState.Success -> {
                        val items = pageUiState.data
                        if (items.isEmpty()) {
                            ErrorMessage(
                                message =
                                    stringResource(
                                        R.string.no_games_found_for_the_selected_date
                                    ),
                                onRetry = { viewModel.fetchItems(category.url, pageDate) },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            // Show native ad after 5th item when there are 6+ items
                            val showNativeAd = items.size >= 6

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                itemsIndexed(items) { index, item ->
                                    ItemCard(
                                        item = item,
                                        onClick = {
                                            navController.navigate(
                                                Routes.FixtureDetails.createRoute(
                                                    item.fixtureId ?: ""
                                                ),
                                            )
                                        },
                                        onFavoriteClick = { viewModel.toggleFavorite(item) },
                                        viewModel = viewModel
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Insert native ad after 5th item (index 4)
                                    if (showNativeAd && index == 4) {
                                        NativeAdItem()
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }

                    is UiState.Empty -> {
                        ErrorMessage(
                            message = stringResource(R.string.no_games_found_for_the_selected_date),
                            onRetry = { viewModel.fetchItems(category.url, pageDate) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    is UiState.ShowSnackbar -> { /* Handled elsewhere */ }
                }
            }
        }
    }
}

/**
 * Modern, interactive horizontal date selector bar.
 * Provides clear visual affordance that multiple dates are available to browse via tap or swipe.
 */
@Composable
private fun DateSelectorRow(
    pagerState: PagerState,
    pageCount: Int,
    pageToDate: (Int) -> LocalDate,
    onDateSelected: (Int) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val today = remember { LocalDate.now() }

    // Auto-scroll the selected chip into view when page changes (via swipe or tap)
    LaunchedEffect(pagerState.currentPage) {
        lazyListState.animateScrollToItem(
            index = pagerState.currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
            scrollOffset = -120
        )
    }

    LazyRow(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(pageCount) { page ->
            val date = pageToDate(page)
            val isSelected = pagerState.currentPage == page
            val isToday = date == today

            val dayLabel = when (date) {
                today -> stringResource(R.string.today)
                today.minusDays(1) -> stringResource(R.string.yesterday)
                today.plusDays(1) -> stringResource(R.string.tomorrow)
                else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale)
            }

            val dateText = date.format(DateTimeFormatter.ofPattern("d MMM"))

            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else if (isToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }

            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                onClick = { onDateSelected(page) },
                shape = RoundedCornerShape(14.dp),
                color = containerColor,
                border = if (isToday && !isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                } else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .height(50.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = if (isSelected) 0.9f else 0.75f)
                    )
                }
            }
        }
    }
}
