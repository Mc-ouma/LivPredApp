package com.soccertips.predictx.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.navigation.Routes
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.ui.components.ErrorMessage
import com.soccertips.predictx.ui.components.LoadingIndicator
import com.soccertips.predictx.ui.fixturedetails.EmptyScreen
import com.soccertips.predictx.ui.fixturedetails.ErrorScreen
import com.soccertips.predictx.viewmodel.CategoriesViewModel


// CategoriesScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
    ) { paddingValues ->
        when (uiState) {
            is UiState.Loading -> {
                LoadingIndicator()
            }

            is UiState.Error -> {
                ErrorScreen(
                    paddingValues = paddingValues,
                    message = "No internet connection. Please check your network.",
                    onRetry = { viewModel.retryLoadCategories() }
                )
            }

            is UiState.Success -> {
                val categories = (uiState as UiState.Success<List<Category>>).data
                CategoriesContent(
                    modifier = Modifier.padding(paddingValues),
                    navController = navController,
                    categories = categories,
                )
            }

            UiState.Empty -> EmptyScreen(
                paddingValues = paddingValues,
                message = "No categories available. Join our Telegram channel for updates and support.",
            )

            else -> Unit
        }
    }
}


@Composable
fun CategoriesContent(
    modifier: Modifier = Modifier,
    navController: NavController,
    categories: List<Category>
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxSize(),
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

@Preview
@Composable
private fun CategoryPreview() {
    val category = Category("Premier League", "premier-league")
    CategoryCard(category = category, onClick = {})

}