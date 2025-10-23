package com.soccertips.predictx.ui.categories

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.soccertips.predictx.data.model.Announcement
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.ui.components.AnnouncementCard
import com.soccertips.predictx.ui.components.LoadingIndicator
import com.soccertips.predictx.ui.fixturedetails.EmptyScreen
import com.soccertips.predictx.ui.fixturedetails.ErrorScreen
import com.soccertips.predictx.viewmodel.CategoriesViewModel

// CategoriesScreen.kt
@Composable
fun CategoriesScreen(
        navController: NavController,
        viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                    onDismissAnnouncement = { announcementId ->
                        dismissedAnnouncementIds = dismissedAnnouncementIds + announcementId
                    },
                    onAnnouncementActionClick = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
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
        onDismissAnnouncement: (String) -> Unit = {},
        onAnnouncementActionClick: (String) -> Unit = {}
) {
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
                        val encodedUrl = java.net.URLEncoder.encode(category.url, "UTF-8")
                        navController.navigate(Routes.ItemsList.createRoute(encodedUrl))
                    },
            )
        }
    }
}
