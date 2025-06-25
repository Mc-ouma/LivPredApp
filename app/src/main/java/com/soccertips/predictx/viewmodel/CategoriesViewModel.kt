package com.soccertips.predictx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.repository.FirebaseRepository
import com.soccertips.predictx.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class CategoriesViewModel @Inject constructor(private val firebaseRepository: FirebaseRepository) :
        ViewModel() {

    private val telegramMessage = "        context.getString(R.string.no_categories_available)"
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
                                    Timber.tag("Categories")
                                            .d("loadCategories: No categories found")
                                    _uiState.value =
                                            UiState.Error(
                                                    telegramMessage
                                            ) // (getFallbackCategories())
                                } else {
                                    Timber.tag("Categories")
                                            .d("loadCategories: Categories loaded successfully")
                                    _uiState.value = UiState.Success(categories)
                                }
                            },
                            onFailure = { error ->
                                Timber.tag("Categories")
                                        .e(error, "loadCategories: Error loading categories")
                                _uiState.value = UiState.Error(telegramMessage)
                                // _uiState.value = UiState.Success(getFallbackCategories())
                            }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(telegramMessage)
            }
        }
    }

    // Fallback categories to be used if Firebase data is not available
    /* private fun getFallbackCategories(): List<Category> =
    listOf(
        Category("https://dailypredictz.com/scripts/pscripts/json_over.php", context.getString(R.string.over_under)),
        Category("https://dailypredictz.com/scripts/pscripts/json_gg.php", context.getString(R.string.btts)),
        Category("https://dailypredictz.com/scripts/pscripts/json_2odds.php", context.getString(R.string.daily_2_odds)),
        Category("https://dailypredictz.com/scripts/pscripts/json_combo.php", context.getString(R.string.combo)),
        Category("https://dailypredictz.com/scripts/pscripts/json_htft.php", context.getString(R.string.ht_ft)),
        Category("https://dailypredictz.com/scripts/pscripts/json_home_away.php", context.getString(R.string.home_away)),
        Category("https://surebetsapp.com//app_json_scrits/json_toppicks.php", context.getString(R.string.daily_bonus)),
        Category("https://dailypredictz.com/scripts/pscripts/json_Toppicks.php", context.getString(R.string.extra_picks)),
    )*/

    // Function to retry loading categories
    fun retryLoadCategories() {
        _uiState.value = UiState.Loading
        loadCategories()
    }
}
