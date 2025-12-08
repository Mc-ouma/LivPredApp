package com.soccertips.predictx.ui.categories

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.soccertips.predictx.R
import com.soccertips.predictx.admob.InterstitialAdManager
import com.soccertips.predictx.admob.RewardedAdManager
import com.soccertips.predictx.data.model.Announcement
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.repository.RemoteConfigRepository
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
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@EntryPoint
@InstallIn(ActivityComponent::class)
interface CategoriesRewardedAdManagerEntryPoint {
    fun rewardedAdManager(): RewardedAdManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CategoriesInterstitialAdManagerEntryPoint {
    fun interstitialAdManager(): InterstitialAdManager
}

// CategoriesScreen.kt
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val adStrategy by viewModel.adStrategy.collectAsStateWithLifecycle()
    val passBalance by viewModel.passBalance.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Get rewarded ad manager using EntryPoint
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

    // Get interstitial ad manager using EntryPoint (for A/B test interstitial variant)
    val interstitialAdManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CategoriesInterstitialAdManagerEntryPoint::class.java
        ).interstitialAdManager()
    }

    // Preload the appropriate ad based on strategy
    LaunchedEffect(adStrategy, rewardedAdManager, interstitialAdManager) {
        when (adStrategy) {
            RemoteConfigRepository.AD_STRATEGY_REWARDED -> {
                rewardedAdManager?.let { adManager ->
                    val isAdReady = adManager.isAdReady.value
                    if (!isAdReady) {
                        Timber.tag("CategoriesScreen").d("Preloading rewarded ad for A/B test...")
                        adManager.loadRewardedAd()
                    }
                }
            }

            RemoteConfigRepository.AD_STRATEGY_INTERSTITIAL -> {
                // Preload interstitial ad for back navigation in ItemsListScreen
                val isAdReady = interstitialAdManager.isAdReady.value
                if (!isAdReady) {
                    Timber.tag("CategoriesScreen")
                        .d("Preloading interstitial ad for A/B test (will show on back navigation)...")
                    activity?.let { interstitialAdManager.setActivityContext(it) }
                    interstitialAdManager.loadAdIfNeeded()
                }
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
                adStrategy = adStrategy,
                passBalance = passBalance,
                rewardedAdManager = rewardedAdManager,
                viewModel = viewModel,
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
    adStrategy: String = RemoteConfigRepository.AD_STRATEGY_REWARDED,
    passBalance: Int = 0,
    rewardedAdManager: RewardedAdManager?,
    viewModel: CategoriesViewModel,
    onDismissAnnouncement: (String) -> Unit = {},
    onAnnouncementActionClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    // State for dialogs
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showEarnPassDialog by remember { mutableStateOf(false) }
    var showMaxPassesDialog by remember { mutableStateOf(false) }
    var pendingCategory by remember { mutableStateOf<Category?>(null) }
    var isLoadingAd by remember { mutableStateOf(false) }
    var showAdNotAvailableDialog by remember { mutableStateOf(false) }

    // Function to check if category is unlocked
    fun isCategoryUnlocked(categoryUrl: String): Boolean {
        return viewModel.isCategoryUnlocked(categoryUrl)
    }

    // Function to handle earning passes (from the earn button)
    val handleEarnPass: () -> Unit = {
        val isAdReady = rewardedAdManager?.isAdReady?.value ?: false

        if (!isAdReady) {
            // Ad not ready, try to load it
            isLoadingAd = true
            Timber.tag("CategoriesScreen").d("Earn button clicked - Ad not ready, loading...")
            rewardedAdManager?.loadRewardedAd()

            // Show "ad not available" message after timeout
            scope.launch {
                delay(3000)
                if (isLoadingAd) {
                    isLoadingAd = false
                    showAdNotAvailableDialog = true
                }
            }
        } else {
            // Ad is ready, show it
            activity?.let { act ->
                rewardedAdManager?.showRewardedAd(
                    activity = act,
                    onRewardEarned = { rewardItem ->
                        isLoadingAd = false
                        Timber.tag("CategoriesScreen")
                            .d(
                                "Reward earned from earn button: ${rewardItem.amount} ${rewardItem.type}"
                            )
                        // Add pass to balance
                        if (viewModel.addPass()) {
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.pass_earned_message,
                                    passBalance,
                                    viewModel.getMaxPasses()
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                            if (pendingCategory != null) {
                                showUnlockDialog = true
                            }
                        }
                    },
                    onFailure = {
                        isLoadingAd = false
                        Timber.tag("CategoriesScreen")
                            .e("Failed to show rewarded ad from earn button")
                        showAdNotAvailableDialog = true
                    }
                )
            }
        }
    }

    // Dialog for using a pass to unlock
    if (showUnlockDialog && pendingCategory != null) {
        AlertDialog(
            onDismissRequest = {
                showUnlockDialog = false
                pendingCategory = null
            },
            title = { Text(stringResource(R.string.unlock_category_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.unlock_category_message,
                        pendingCategory!!.name,
                        passBalance,
                        viewModel.getMaxPasses()
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val category = pendingCategory
                        if (category != null && viewModel.usePass(category.url)) {
                            // Pass used successfully, navigate to category
                            val encodedUrl =
                                java.net.URLEncoder.encode(category.url, "UTF-8")
                            navController.navigate(Routes.ItemsList.createRoute(encodedUrl))
                            Timber.tag("CategoriesScreen")
                                .d("Pass used for ${category.name}")
                        }
                        showUnlockDialog = false
                        pendingCategory = null
                    }
                ) { Text(stringResource(R.string.use_pass)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUnlockDialog = false
                        pendingCategory = null
                    }
                ) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Dialog for earning a pass (no passes available)
    if (showEarnPassDialog && pendingCategory != null) {
        AlertDialog(
            onDismissRequest = {
                showEarnPassDialog = false
                pendingCategory = null
                isLoadingAd = false
            },
            title = { Text(stringResource(R.string.no_passes_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.no_passes_message,
                        passBalance,
                        viewModel.getMaxPasses()
                    )
                )
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

                            showEarnPassDialog = false

                            // Show "ad not available" message after timeout
                            scope.launch {
                                delay(3000)
                                if (isLoadingAd) {
                                    isLoadingAd = false
                                    showAdNotAvailableDialog = true
                                }
                            }
                        } else {
                            // Ad is ready, show it
                            showEarnPassDialog = false
                            activity?.let { act ->
                                rewardedAdManager?.showRewardedAd(
                                    activity = act,
                                    onRewardEarned = { rewardItem ->
                                        isLoadingAd = false
                                        Timber.tag("CategoriesScreen")
                                            .d(
                                                "Reward earned: ${rewardItem.amount} ${rewardItem.type}"
                                            )
                                        // Add pass to balance
                                        if (viewModel.addPass()) {
                                            Toast.makeText(
                                                context,
                                                context.getString(
                                                    R.string.pass_earned_message,
                                                    passBalance,
                                                    viewModel.getMaxPasses()
                                                ),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            if (pendingCategory != null) {
                                                showUnlockDialog = true
                                            }
                                        }
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
                        Text(stringResource(R.string.watch_ad_to_earn))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEarnPassDialog = false
                        pendingCategory = null
                        isLoadingAd = false
                    }
                ) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Dialog for maximum passes reached
    if (showMaxPassesDialog) {
        AlertDialog(
            onDismissRequest = {
                showMaxPassesDialog = false
                pendingCategory = null
            },
            title = { Text(stringResource(R.string.max_passes_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.max_passes_message,
                        passBalance,
                        viewModel.getMaxPasses()
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMaxPassesDialog = false
                        // Show unlock dialog to encourage using passes
                        if (pendingCategory != null) {
                            showUnlockDialog = true
                        } else {
                            pendingCategory = null
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMaxPassesDialog = false
                        pendingCategory = null
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
        // Pass Counter (only show for rewarded ad strategy with locked categories)
        if (adStrategy == RemoteConfigRepository.AD_STRATEGY_REWARDED &&
            categories.any { it.requiresRewardAd }
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PassCounter(
                        passBalance = passBalance,
                        maxPasses = viewModel.getMaxPasses(),
                        onEarnClick = if (!isLoadingAd) handleEarnPass else null
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PassIndicators(passBalance = passBalance, maxPasses = viewModel.getMaxPasses())
                }
            }
        }

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
                    when (adStrategy) {
                        RemoteConfigRepository.AD_STRATEGY_REWARDED -> {
                            // Rewarded ad strategy: use pass system for locked categories
                            if (category.requiresRewardAd && !isCategoryUnlocked(category.url)
                            ) {
                                // Category is locked
                                pendingCategory = category
                                if (viewModel.hasPass()) {
                                    // Has pass, show unlock dialog
                                    showUnlockDialog = true
                                } else if (viewModel.canEarnMorePasses()) {
                                    // No pass but can earn more, show earn pass dialog
                                    showEarnPassDialog = true
                                } else {
                                    // At maximum passes, show max passes dialog
                                    showMaxPassesDialog = true
                                }
                            } else {
                                // Navigate directly if unlocked
                                val encodedUrl =
                                    java.net.URLEncoder.encode(category.url, "UTF-8")
                                navController.navigate(Routes.ItemsList.createRoute(encodedUrl))
                            }
                        }

                        RemoteConfigRepository.AD_STRATEGY_INTERSTITIAL -> {
                            // Interstitial strategy: navigate immediately
                            // Interstitial ad will show on back press in ItemsListScreen
                            val encodedUrl = java.net.URLEncoder.encode(category.url, "UTF-8")
                            navController.navigate(Routes.ItemsList.createRoute(encodedUrl))
                        }

                        else -> {
                            // Fallback to direct navigation
                            val encodedUrl = java.net.URLEncoder.encode(category.url, "UTF-8")
                            navController.navigate(Routes.ItemsList.createRoute(encodedUrl))
                        }
                    }
                },
            )
        }
    }
}

/** Displays the unlock pass counter with an icon and optional earn button */
@Composable
fun PassCounter(
    passBalance: Int,
    maxPasses: Int,
    onEarnClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pass counter badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ConfirmationNumber,
                    contentDescription = stringResource(R.string.unlock_passes),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$passBalance/$maxPasses",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Earn more button (only show if not at max and callback provided)
        if (passBalance < maxPasses && onEarnClick != null) {
            Surface(
                onClick = onEarnClick,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.earn_more_passes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/** Displays individual pass indicators (visual dots) */
@Composable
fun PassIndicators(passBalance: Int, maxPasses: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxPasses) { index ->
            Box(
                modifier =
                    Modifier.size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < passBalance) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
            )
        }
    }
}
