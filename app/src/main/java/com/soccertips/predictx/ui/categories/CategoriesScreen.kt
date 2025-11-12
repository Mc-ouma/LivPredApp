package com.soccertips.predictx.ui.categories

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.soccertips.predictx.R
import com.soccertips.predictx.admob.RewardedAdManager
import com.soccertips.predictx.data.model.Announcement
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.ui.components.AnnouncementCard
import com.soccertips.predictx.ui.components.LoadingIndicator
import com.soccertips.predictx.ui.fixturedetails.EmptyScreen
import com.soccertips.predictx.ui.fixturedetails.ErrorScreen
import com.soccertips.predictx.viewmodel.CategoriesViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@EntryPoint
@InstallIn(ActivityComponent::class)
interface CategoriesRewardedAdManagerEntryPoint {
    fun rewardedAdManager(): RewardedAdManager
}

// CategoriesScreen.kt
@Composable
fun CategoriesScreen(
        navController: NavController,
        viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Get RewardedAdManager using EntryPoint
    val rewardedAdManager = remember {
        activity?.let {
            val entryPoint =
                    EntryPointAccessors.fromActivity(
                            it,
                            CategoriesRewardedAdManagerEntryPoint::class.java
                    )
            entryPoint.rewardedAdManager()
        }
    }

    // Preload rewarded ad when screen is displayed
    LaunchedEffect(rewardedAdManager) {
        rewardedAdManager?.let { adManager ->
            val isAdReady = adManager.isAdReady.value
            if (!isAdReady) {
                Timber.tag("CategoriesScreen").d("Preloading rewarded ad...")
                adManager.loadRewardedAd()
            }
        }
    }

    // Track dismissed announcements
    var dismissedAnnouncementIds by remember { mutableStateOf(setOf<String>()) }
    val visibleAnnouncements = announcements.filter { it.id !in dismissedAnnouncementIds }

    when (uiState) {
        is UiState.Loading -> {
            LoadingIndicator()
        }
        is UiState.Error -> {
            ErrorScreen(
                    paddingValues = PaddingValues(0.dp),
                    message = "No internet connection. Please check your network.",
                    onRetry = { viewModel.retryLoadCategories() }
            )
        }
        is UiState.Success -> {
            val categories = (uiState as UiState.Success<List<Category>>).data
            CategoriesContent(
                    navController = navController,
                    categories = categories,
                    announcements = visibleAnnouncements,
                    rewardedAdManager = rewardedAdManager,
                    onDismissAnnouncement = { announcementId ->
                        dismissedAnnouncementIds + announcementId
                    },
                    onAnnouncementActionClick = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error silently
                        }
                    }
            )
        }
        UiState.Empty ->
                EmptyScreen(
                        paddingValues = PaddingValues(0.dp),
                        message =
                                "No categories available. Join our Telegram channel for updates and support.",
                )
        else -> Unit
    }
}

@Composable
fun CategoriesContent(
        modifier: Modifier = Modifier,
        navController: NavController,
        categories: List<Category>,
        announcements: List<Announcement> = emptyList(),
        rewardedAdManager: RewardedAdManager?,
        onDismissAnnouncement: (String) -> Unit = {},
        onAnnouncementActionClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    // SharedPreferences to track unlocked categories
    val sharedPrefs = remember {
        context.getSharedPreferences("category_unlock_prefs", android.content.Context.MODE_PRIVATE)
    }

    // State to show reward ad dialog
    var showRewardAdDialog by remember { mutableStateOf(false) }
    var pendingCategory by remember { mutableStateOf<Category?>(null) }
    var isLoadingAd by remember { mutableStateOf(false) }
    var showAdNotAvailableDialog by remember { mutableStateOf(false) }

    // Function to check if category is unlocked
    fun isCategoryUnlocked(categoryUrl: String): Boolean {
        val unlockTimestamp = sharedPrefs.getLong("unlock_${categoryUrl}", 0L)
        val currentTime = System.currentTimeMillis()
        val unlockDuration = 1000L //

        return (currentTime - unlockTimestamp) < unlockDuration
    }

    // Function to unlock category
    fun unlockCategory(categoryUrl: String) {
        sharedPrefs.edit { putLong("unlock_${categoryUrl}", System.currentTimeMillis()) }
        Timber.tag("CategoriesScreen").d("Category unlocked: $categoryUrl")
    }

    // Dialog for reward ad
    if (showRewardAdDialog && pendingCategory != null) {
        AlertDialog(
                onDismissRequest = {
                    showRewardAdDialog = false
                    pendingCategory = null
                    isLoadingAd = false
                },
                title = { Text(stringResource(R.string.watch_ad_to_unlock)) },
                text = {
                    Text(stringResource(R.string.watch_ad_unlock_24h, pendingCategory!!.name))
                },
                confirmButton = {
                    Button(
                            onClick = {
                                val isAdReady = rewardedAdManager?.isAdReady?.value ?: false

                                if (!isAdReady) {
                                    // Ad not ready, show loading state and load ad
                                    isLoadingAd = true
                                    Timber.tag("CategoriesScreen").d("Ad not ready, loading...")
                                    rewardedAdManager?.loadRewardedAd()

                                    // Wait a moment for ad to load, then try to show
                                    // If still not ready after timeout, show error
                                    showRewardAdDialog = false

                                    // Show "ad not available" message after brief delay if still
                                    // loading
                                    scope.launch {
                                        delay(3000) // Wait 3 seconds
                                        if (isLoadingAd) {
                                            isLoadingAd = false
                                            showAdNotAvailableDialog = true
                                        }
                                    }
                                } else {
                                    // Ad is ready, show it
                                    showRewardAdDialog = false
                                    activity?.let { act ->
                                        rewardedAdManager?.showRewardedAd(
                                                activity = act,
                                                onRewardEarned = { rewardItem ->
                                                    isLoadingAd = false
                                                    Timber.tag("CategoriesScreen")
                                                            .d(
                                                                    "Reward earned: ${rewardItem.amount} ${rewardItem.type}"
                                                            )
                                                    // Unlock the category for 24 hours
                                                    pendingCategory?.let { category ->
                                                        unlockCategory(category.url)
                                                        val encodedUrl =
                                                                java.net.URLEncoder.encode(
                                                                        category.url,
                                                                        "UTF-8"
                                                                )
                                                        navController.navigate(
                                                                Routes.ItemsList.createRoute(
                                                                        encodedUrl
                                                                )
                                                        )
                                                    }
                                                    pendingCategory = null
                                                },
                                                onFailure = {
                                                    isLoadingAd = false
                                                    Timber.tag("CategoriesScreen")
                                                            .e("Failed to show rewarded ad")
                                                    showAdNotAvailableDialog = true
                                                    pendingCategory = null
                                                }
                                        )
                                    }
                                }
                            },
                            enabled = !isLoadingAd
                    ) {
                        if (isLoadingAd) {
                            Text(stringResource(R.string.loading))
                        } else {
                            Text(stringResource(R.string.watch_ad))
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                            onClick = {
                                showRewardAdDialog = false
                                pendingCategory = null
                                isLoadingAd = false
                            }
                    ) { Text(stringResource(R.string.cancel)) }
                }
        )
    }

    // Dialog for ad not available
    if (showAdNotAvailableDialog) {
        AlertDialog(
                onDismissRequest = {
                    showAdNotAvailableDialog = false
                    pendingCategory = null
                },
                title = { Text(stringResource(R.string.ad_not_available)) },
                text = { Text(stringResource(R.string.ad_not_available_message)) },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showAdNotAvailableDialog = false
                                pendingCategory = null
                            }
                    ) { Text(stringResource(R.string.ok)) }
                }
        )
    }

    // Use a single LazyVerticalGrid to avoid nested scrollables and fixed heights
    LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Announcements span full width (all columns)
        items(
                count = announcements.size,
                key = { index -> announcements[index].id },
                span = { GridItemSpan(maxLineSpan) }
        ) { index ->
            val announcement = announcements[index]
            AnnouncementCard(
                    announcement = announcement,
                    onDismiss = { onDismissAnnouncement(announcement.id) },
                    onActionClick = onAnnouncementActionClick
            )
        }

        // Category items
        items(
                count = categories.size,
                key = { index -> categories[index].url },
        ) { index ->
            val category = categories[index]
            val isUnlocked = !category.requiresRewardAd || isCategoryUnlocked(category.url)

            CategoryCard(
                    category = category,
                    isUnlocked = isUnlocked,
                    onClick = {
                        if (category.requiresRewardAd && !isCategoryUnlocked(category.url)) {
                            // Show reward ad dialog if locked and not unlocked
                            pendingCategory = category
                            showRewardAdDialog = true
                        } else {
                            // Navigate directly if unlocked or doesn't require ad
                            val encodedUrl = java.net.URLEncoder.encode(category.url, "UTF-8")
                            navController.navigate(Routes.ItemsList.createRoute(encodedUrl))
                        }
                    },
            )
        }
    }
}
