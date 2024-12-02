package com.soccertips.predcompose.ui.categories

import android.content.ContentValues.TAG
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.soccertips.predcompose.Menu
import com.soccertips.predcompose.model.Category
import com.soccertips.predcompose.navigation.Routes
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.ui.components.ErrorMessage
import com.soccertips.predcompose.ui.components.LoadingIndicator
import com.soccertips.predcompose.viewmodel.CategoriesViewModel
import timber.log.Timber

// CategoriesScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    // Collect the UI state from the ViewModel
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = { Text("Categories") },
                navigationIcon = { },
                actions = {
                    Menu()
                }
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (uiState) {
                is UiState.Loading -> {
                    // Show a loading indicator while categories are being fetched
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is UiState.Success -> {
                    // Extract the categories list from the success state
                    val categories = (uiState as UiState.Success<List<Category>>).data
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(
                            count = categories.size,
                            key = { index -> categories[index].endpoint },
                        ) { index ->
                            val category = categories[index]
                            CategoryCard(
                                category = category,
                                onClick = {
                                    navController.navigate(Routes.ItemsList.createRoute(category.endpoint))
                                    Timber.tag(TAG).d("CategoriesScreen:  ${category.endpoint}")
                                },
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    // Show an error message if something went wrong
                    ErrorMessage(
                        message = (uiState as UiState.Error).message,
                        onRetry = { viewModel.retryLoadCategories() },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    // Handle any other states (optional, since most states are covered)
                }
            }
        }
    }
}
