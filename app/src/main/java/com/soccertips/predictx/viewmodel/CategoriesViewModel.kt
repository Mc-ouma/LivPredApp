package com.soccertips.predictx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predictx.data.model.Announcement
import com.soccertips.predictx.data.model.Category
import com.soccertips.predictx.repository.CategoryRepository
import com.soccertips.predictx.repository.RemoteConfigRepository
import com.soccertips.predictx.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class CategoriesViewModel
@Inject
constructor(
    private val categoryRepository: CategoryRepository,
    private val remoteConfigRepository: RemoteConfigRepository
) : ViewModel() {

    private val telegramMessage = "        context.getString(R.string.no_categories_available)"

    // Private mutable state that holds the UI state (loading, success, error)
    private val _uiState = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Category>>> = _uiState.asStateFlow()

    // State for announcements
    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    // Initialize by loading categories from a local source when the ViewModel is first created
    init {
        loadCategories()
        loadAnnouncements()
    }

    // Function to load announcements from Remote Config
    private fun loadAnnouncements() {
        viewModelScope.launch {
            try {
                remoteConfigRepository.getAnnouncements().collect { announcements ->
                    Timber.d("Announcements loaded: ${announcements.size}")
                    _announcements.value = announcements
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading announcements")
                _announcements.value = emptyList()
            }
        }
    }

    // Function to load categories from a local source
    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                categoryRepository.getCategories().collect { result ->
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

    // Function to retry loading categories
    fun retryLoadCategories() {
        _uiState.value = UiState.Loading
        loadCategories()
    }
}
