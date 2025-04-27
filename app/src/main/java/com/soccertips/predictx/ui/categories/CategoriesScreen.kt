package com.soccertips.predictx.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.soccertips.predictx.viewmodel.CategoriesViewModel


// CategoriesScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (uiState) {
        is UiState.Loading -> {
            LoadingIndicator()
        }

        is UiState.Error -> {
            ErrorMessage(
                (uiState as UiState.Error).message,
                onRetry = { viewModel.retryLoadCategories() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        is UiState.Success -> {
            val categories = (uiState as UiState.Success<List<Category>>).data
            CategoriesContent(
                navController = navController,
                categories = categories,
            )
        }

        UiState.Empty -> EmptyScreen(
            paddingValues = PaddingValues(16.dp),
            message = "Error, Try Restarting App"
        )

        else -> Unit
    }
}


@Composable
fun CategoriesContent(navController: NavController, categories: List<Category>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize(),
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
