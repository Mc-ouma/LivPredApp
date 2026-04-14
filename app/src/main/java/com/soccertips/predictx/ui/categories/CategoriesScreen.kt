package com.soccertips.predictx.ui.categories

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.soccertips.predictx.R
import com.soccertips.predictx.admob.InterstitialAdManager
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
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

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
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Get interstitial ad manager for preloading
    val interstitialAdManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CategoriesInterstitialAdManagerEntryPoint::class.java
        ).interstitialAdManager()
    }

    // Preload interstitial ad when CategoriesScreen is displayed
    // This ensures the ad is ready when user navigates to ItemsListScreen
    LaunchedEffect(interstitialAdManager) {
        activity?.let { act ->
            interstitialAdManager.setActivityContext(act)

            val isAdReady = interstitialAdManager.isAdReady.value
            val isLoading = interstitialAdManager.isCurrentlyLoading()

            if (!isAdReady && !isLoading) {
                Timber.tag("CategoriesScreen").d("Preloading interstitial ad for later use...")
                interstitialAdManager.loadAdIfNeeded()
            } else {
                Timber.tag("CategoriesScreen").d("Interstitial ad already ready=$isAdReady or loading=$isLoading")
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
                message = stringResource(R.string.no_internet_connection),
                onRetry = { viewModel.retryLoadCategories() }
            )
        }

        is UiState.Success -> {
            val categories = (uiState as UiState.Success<List<Category>>).data
            CategoriesContent(
                navController = navController,
                categories = categories,
                announcements = visibleAnnouncements,
                onDismissAnnouncement = { announcementId ->
                    dismissedAnnouncementIds + announcementId
                },
                onAnnouncementActionClick = { url ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        // Use a chooser to avoid SecurityException from apps with non-exported activities
                        context.startActivity(Intent.createChooser(intent, null))
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
    onDismissAnnouncement: (String) -> Unit = {},
    onAnnouncementActionClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isSubscribed = remember {
        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("is_subscribed", false)
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

            CategoryCard(
                category = category,
                onClick = {
                    // Navigate directly to category - interstitial ad shows on back
                    val encodedUrl = java.net.URLEncoder.encode(category.url, "UTF-8")
                    navController.navigate(Routes.ItemsList.createRoute(encodedUrl))
                },
            )
        }

        // Upgrade banner for non-subscribers at the bottom
        if (!isSubscribed) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                UpgradeBannerCard(
                    onClick = { navController.navigate(Routes.Subscription.route) }
                )
            }
        }
    }
}

@Composable
private fun UpgradeBannerCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.upgrade_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.upgrade_banner_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onClick) {
                Text(stringResource(R.string.upgrade_button))
            }
        }
    }
}
