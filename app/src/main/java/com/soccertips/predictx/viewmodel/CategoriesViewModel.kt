package com.soccertips.predictx.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.repository.FirebaseRepository
import com.soccertips.predictx.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel
@Inject
constructor(private val firebaseRepository: FirebaseRepository) : ViewModel() {
    // Private mutable state that holds the UI state (loading, success, error)
    private val _uiState = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Category>>> = _uiState.asStateFlow()

    // Initialize by loading categories from a local source when the ViewModel is first created
    init {
        loadCategories()
    }

    // Function to load categories from a local source
    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                firebaseRepository.getCategories().collect { result ->
                    result.fold(
                        onSuccess = { categories ->
                            if (categories.isEmpty()) {
                                Timber.tag("Categories").d("loadCategories: No categories found")
                                _uiState.value =
                                    UiState.Success(categories)//(getFallbackCategories())
                            } else {
                                Timber.tag("Categories")
                                    .d("loadCategories: Categories loaded successfully")
                                _uiState.value = UiState.Success(categories)
                            }
                        },
                        onFailure = { error ->
                            Timber.tag("Categories")
                                .e(error, "loadCategories: Error loading categories")
                            _uiState.value = UiState.Error("Failed to load categories")
                            // _uiState.value = UiState.Success(getFallbackCategories())
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load categories")

            }
        }
    }

    // Fallback categories to be used if Firebase data is not available
    /* private fun getFallbackCategories(): List<Category> =
         listOf(
             Category("https://dailypredictz.com/scripts/pscripts/json_over.php", "Over/Under"),
             Category("https://dailypredictz.com/scripts/pscripts/json_gg.php", "BTTS"),
             Category("https://dailypredictz.com/scripts/pscripts/json_2odds.php", "Daily 2 Odds"),
             Category("https://dailypredictz.com/scripts/pscripts/json_combo.php", "Combo"),
             Category("https://dailypredictz.com/scripts/pscripts/json_htft.php", "HT/FT"),
             Category("https://dailypredictz.com/scripts/pscripts/json_home_away.php", "Home/Away"),
             Category("https://surebetsapp.com//app_json_scrits/json_toppicks.php", "Daily Bonus"),
             Category("https://dailypredictz.com/scripts/pscripts/json_Toppicks.php", "Extra Picks"),
         )*/

    // Function to retry loading categories
    fun retryLoadCategories() {
        _uiState.value = UiState.Loading
        loadCategories()
    }
}
