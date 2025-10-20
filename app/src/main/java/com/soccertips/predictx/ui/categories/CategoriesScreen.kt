package com.soccertips.predictx.ui.categories

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    LazyColumn(modifier = modifier.fillMaxSize()) {
        // Show announcements at the top
        items(items = announcements, key = { it.id }) { announcement ->
            AnnouncementCard(
                    announcement = announcement,
                    onDismiss = { onDismissAnnouncement(announcement.id) },
                    onActionClick = onAnnouncementActionClick
            )
        }

        // Categories grid
        item {
            LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxWidth().height(600.dp), // Adjust based on your needs
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
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
    }
}
