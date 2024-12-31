package com.soccertips.predcompose.ui.categories

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
import com.soccertips.predcompose.data.model.Category
import com.soccertips.predcompose.navigation.Routes
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.ui.components.ErrorMessage
import com.soccertips.predcompose.ui.components.LoadingIndicator
import com.soccertips.predcompose.viewmodel.CategoriesViewModel


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
            CategoriesScreen(
                navController = navController,
                categories = categories,
            )
        }

        UiState.Empty -> TODO()
        UiState.Idle -> TODO()
    }
}

@Composable
fun CategoriesScreen(navController: NavController, categories: List<Category>) {
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
