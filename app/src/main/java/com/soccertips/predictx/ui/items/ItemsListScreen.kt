package com.soccertips.predictx.ui.items


import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.soccertips.predictx.Menu
import com.soccertips.predictx.R
import com.soccertips.predictx.admob.BannerAdView
import com.soccertips.predictx.admob.InterstitialAdManager
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.ui.components.DateUtils
import com.soccertips.predictx.ui.components.ErrorMessage
import com.soccertips.predictx.ui.components.LoadingIndicator
import com.soccertips.predictx.viewmodel.ItemsListViewModel
import java.time.LocalDate


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
    interstitialAdManager: InterstitialAdManager,
    context: android.content.Context = navController.context,
) {
    val category = remember(categoryId) {
        categories.find { it.url == categoryId }
    } ?: categories.first()

    val datePickerState = rememberDatePickerState(selectableDates = Last5DaysSelectableDates)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val formattedDate = DateUtils.formatRelativeDate(context, selectedDate.toString())
    var scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
                //show  interstitial ad when the back button is pressed
                navigationIcon = {
                    IconButton(onClick = {
                        if (interstitialAdManager.isAdLoaded()) {
                            try {
                                val activity = navController.context as? Activity
                                activity?.let {
                                    interstitialAdManager.showInterstitialAdWithCallback(it,
                                        onAdDismissed = {
                                            // Navigate back after ad is dismissed
                                            navController.popBackStack()
                                            // Optionally, you can navigate to a specific screen
                                            // navController.navigate(Routes.SomeScreen.route)
                                        }
                                    )
                                    // The navigation will happen after ad dismissal via the FullScreenContentCallback
                                } ?: run {
                                    // If activity casting fails, just navigate back
                                    navController.popBackStack()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                navController.popBackStack()
                            }
                        } else {
                            // If no ad is loaded, just navigate back
                            navController.popBackStack()
                        }
                    }) {
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
        bottomBar = {
            BannerAdView()
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val uiState = viewModel.uiState.collectAsState().value) {
                is UiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is UiState.Error -> {
                    ErrorMessage(
                        message = stringResource(R.string.failed_to_fetch_games_please_try_again_later),
                        onRetry = { viewModel.fetchItems(category.url, selectedDate) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is UiState.Success -> {
                    val items = uiState.data
                    if (items.isEmpty()) {
                        ErrorMessage(
                            message = stringResource(R.string.no_games_found_for_the_selected_date),
                            onRetry = { viewModel.fetchItems(category.url, selectedDate) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items) { item ->
                                // Check if the item is a favorite
                                var isFavorite by remember { mutableStateOf(false) }
                                LaunchedEffect(item) {
                                    isFavorite = viewModel.isFavorite(item)
                                }

                                ItemCard(
                                    item = item,
                                    onClick = {
                                        navController.navigate(
                                            Routes.FixtureDetails.createRoute(
                                                item.fixtureId ?: ""
                                            ),
                                        )
                                    },
                                    onFavoriteClick = {
                                        viewModel.toggleFavorite(item)
                                    },
                                    viewModel = viewModel
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                else -> { /* No Action */
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
                                    LocalDate.ofEpochDay(selectedDateMillis / (24 * 60 * 60 * 1000))
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                },
                content = {
                    DatePicker(state = datePickerState)
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}
