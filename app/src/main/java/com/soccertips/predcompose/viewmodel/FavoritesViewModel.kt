package com.soccertips.predcompose.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predcompose.data.local.dao.FavoriteDao
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import com.soccertips.predcompose.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteItemDao: FavoriteDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<FavoriteItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<FavoriteItem>>> = _uiState.asStateFlow()

    // StateFlow to track the number of favorite items
    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val favoriteItems = favoriteItemDao.getAllFavorites()
                val sortedItems = favoriteItems.sortedBy { item ->
                    val date = item.mDate?.let { LocalDate.parse(it) } ?: LocalDate.MIN
                    date
                }
                _uiState.value = UiState.Success(sortedItems)
                _favoriteCount.value = favoriteItems.size // Update the count
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }

    fun removeFromFavorites(item: FavoriteItem) {
        viewModelScope.launch {
            try {
                favoriteItemDao.delete(fixtureId = item.fixtureId.toString())
                loadFavorites() // Reload favorites to update the count
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }

    fun formatRelativeDate(dateString: String?): String {
        if (dateString == null) return "Unknown Date"

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val date = LocalDate.parse(dateString, formatter)
        val today = LocalDate.now()

        return when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) // Format as "Oct 25, 2023"
        }
    }
}