package com.soccertips.predcompose.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.soccertips.predcompose.CheckDueItemsWorker
import com.soccertips.predcompose.data.local.dao.FavoriteDao
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import com.soccertips.predcompose.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteItemDao: FavoriteDao,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<FavoriteItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<FavoriteItem>>> = _uiState.asStateFlow()

    val favoriteCount = favoriteItemDao.getFavoriteCount()
        .distinctUntilChanged()

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
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }

    fun restoreFavorites(item: FavoriteItem) {
        viewModelScope.launch {
            try {
                favoriteItemDao.insert(item)
                loadFavorites() // Reload favorites to update the UI
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
                loadFavorites() // Reload favorites to update the UI
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }

    // Schedule a notification to remind the user of due items
    private fun scheduleNotification(items: List<FavoriteItem>) {
        // Schedule a notification to remind the user of due items
        items.forEach { item ->
            val currentTime = item.mTime ?: return@forEach
            val currentDate = item.mDate ?: return@forEach

            val data = workDataOf(
                "currentTime" to currentTime,
                "currentDate" to currentDate
            )
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = PeriodicWorkRequestBuilder<CheckDueItemsWorker>(1, TimeUnit.DAYS)
                .setInputData(data)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "checkDueItems_${item.fixtureId}",
                ExistingPeriodicWorkPolicy.REPLACE, workRequest
            )


        }

    }
}