package com.soccertips.predictx.ui.items

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.soccertips.predictx.Menu
import com.soccertips.predictx.R
import com.soccertips.predictx.admob.CollapsibleBannerAdView
import com.soccertips.predictx.admob.InterstitialAdManager
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
import timber.log.Timber

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
object Last5DaysSelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val fiveDaysAgo =
            System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000) // 5 days in milliseconds
        return utcTimeMillis >= fiveDaysAgo && utcTimeMillis <= System.currentTimeMillis()
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= LocalDate.now().year
    }
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
    // and preload interstitial ad
    LaunchedEffect(interstitialAdManager) {
        val activity = context.findActivity()
        if (activity != null) {
            interstitialAdManager.setActivityContext(activity)
            interstitialAdManager.useActivityContextForAdLoading(true)

            // Proactively load interstitial ad for back navigation
            Timber.tag("InterstitialAd").d("Preloading interstitial ad for back navigation")
            interstitialAdManager.loadAdIfNeeded()

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

    val datePickerState = rememberDatePickerState(selectableDates = Last5DaysSelectableDates)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Check for pending navigation date from betting success notification
    val activity = context.findActivity()
    val sharedPrefs = activity?.getSharedPreferences("predictx_prefs", Context.MODE_PRIVATE)
    val pendingNavigationDate = sharedPrefs?.getString("pending_navigation_date", null)

    // Initialize selectedDate with pending navigation date if available, otherwise use today
    var selectedDate by rememberSaveable {
        mutableStateOf(
            if (!pendingNavigationDate.isNullOrEmpty()) {
                try {
                    val parsedDate = LocalDate.parse(pendingNavigationDate)
                    Timber.d(
                        "ItemsListScreen: Using pending navigation date: $pendingNavigationDate"
                    )
                    // Clear the pending navigation date after using it
                    sharedPrefs?.edit()?.remove("pending_navigation_date")?.apply()
                    parsedDate
                } catch (e: Exception) {
                    Timber.w(
                        "ItemsListScreen: Failed to parse pending navigation date: $pendingNavigationDate"
                    )
                    LocalDate.now()
                }
            } else {
                Timber.d("ItemsListScreen: No pending navigation date, using today")
                LocalDate.now()
            }
        )
    }

    val formattedDate = DateUtils.formatRelativeDate(context, selectedDate.toString())
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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

    // Fetch items when the category or selected date changes
    LaunchedEffect(key1 = category, key2 = selectedDate) {
        viewModel.fetchItems(category.url, selectedDate)
    }

    Scaffold(
        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = formattedDate,
                    )
                },
                // show  interstitial ad when the back button is pressed
                navigationIcon = {
                    IconButton(onClick = navigateBackWithAd) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Filled.Today,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Menu()
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { CollapsibleBannerAdView() }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val uiState = viewModel.uiState.collectAsState().value) {
                is UiState.Loading -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center).padding(paddingValues)
                    )
                }

                is UiState.Error -> {
                    ErrorMessage(
                        message =
                            stringResource(
                                R.string.failed_to_fetch_games_please_try_again_later
                            ),
                        onRetry = { viewModel.fetchItems(category.url, selectedDate) },
                        modifier = Modifier.align(Alignment.Center).padding(paddingValues),
                    )
                }

                is UiState.Success -> {
                    val items = uiState.data
                    if (items.isEmpty()) {
                        ErrorMessage(
                            message =
                                stringResource(
                                    R.string.no_games_found_for_the_selected_date
                                ),
                            onRetry = { viewModel.fetchItems(category.url, selectedDate) },
                            modifier = Modifier.align(Alignment.Center).padding(paddingValues),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items) { item ->
                                // Check if the item is a favorite
                                var isFavorite by remember { mutableStateOf(false) }
                                LaunchedEffect(item) { isFavorite = viewModel.isFavorite(item) }

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
                            }
                        }
                    }
                }

                else -> {
                    /* No Action */
                }
            }
        }

        // Date picker dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedDateMillis = datePickerState.selectedDateMillis
                            if (selectedDateMillis != null) {
                                selectedDate =
                                    LocalDate.ofEpochDay(
                                        selectedDateMillis / (24 * 60 * 60 * 1000)
                                    )
                            }
                            showDatePicker = false
                        }
                    ) { Text(stringResource(R.string.ok)) }
                },
                content = { DatePicker(state = datePickerState) },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}
