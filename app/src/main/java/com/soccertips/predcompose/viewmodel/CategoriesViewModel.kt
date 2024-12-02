package com.soccertips.predcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predcompose.model.Category
import com.soccertips.predcompose.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel
@Inject
constructor() : ViewModel() {
    // Private mutable state that holds the UI state (loading, success, error)
    private val _uiState = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Category>>> = _uiState.asStateFlow()

    // Initialize by loading categories from a local source when the ViewModel is first created
    init {
        loadLocalCategories()
    }

    // Function to load categories from a local source
    private fun loadLocalCategories() {
        viewModelScope.launch {
            try {
                // Simulate loading local categories
                val categories = getLocalCategories()

                // Update the UI state with the list of categories
                _uiState.value = UiState.Success(categories)
            } catch (e: Exception) {
                // Handle errors and update the UI state with the error message
                _uiState.value = UiState.Error("Failed to load categories")
            }
        }
    }

    // List of locally defined categories
    private fun getLocalCategories(): List<Category> =
        listOf(
            Category(
                "json_over.php",
                "Over/Under",
            ),
            Category(
                "json_gg.php",
                "BTTS",
            ),
            Category(
                "json_2odds.php",
                "Daily 2 Odds",
            ),
            Category(
                "json_combo.php",
                "Combo",
            ),
            Category(
                "json_htft.php",
                "HT/FT",
            ),
            Category(
                "json_home_away.php",
                "Home/Away",
            ),
            Category(
                "json_tip_of_the_day.php",
                "Daily Bonus",
            ),
            Category(
                "json_Toppicks.php",
                "Extra Picks",
            ),

            )
    // Add more categories as needed

    // Function to retry loading categories
    fun retryLoadCategories() {
        _uiState.value = UiState.Loading
        loadLocalCategories()
    }
}
