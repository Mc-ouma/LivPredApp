// FavoritesViewModel.kt
package com.soccertips.predcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.soccertips.predcompose.CheckDueItemsWorker
import com.soccertips.predcompose.data.local.dao.FavoriteDao
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.util.WorkManagerWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteItemDao: FavoriteDao,
    private val workManagerWrapper: WorkManagerWrapper
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<FavoriteItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<FavoriteItem>>> = _uiState.asStateFlow()

    val favoriteCount = favoriteItemDao.getFavoriteCount().distinctUntilChanged()

    // Use MutableSharedFlow for snackbar events
    private val _snackbarFlow = MutableSharedFlow<SnackbarData>(replay = 1)
    val snackbarFlow: SharedFlow<SnackbarData> = _snackbarFlow

    fun showSnackbar(
        message: String,
        actionLabel: String,
        onActionPerformed: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _snackbarFlow.emit(SnackbarData(message, actionLabel, onActionPerformed))

        }
    }

    data class SnackbarData(
        val message: String,
        val actionLabel: String,
        val onActionPerformed: (() -> Unit)?
    )

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
                scheduleNotification(sortedItems)
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
            val currentFavorites = (uiState.value as? UiState.Success)?.data?.toMutableList() ?: mutableListOf()
            currentFavorites.add(item)
            _uiState.value = UiState.Success(currentFavorites.sortedBy {
                it.mDate?.let { LocalDate.parse(it) } ?: LocalDate.MIN
            })
            scheduleNotification(currentFavorites)
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }
}

fun removeFromFavorites(item: FavoriteItem) {
    viewModelScope.launch {
        try {
            favoriteItemDao.delete(fixtureId = item.fixtureId.toString())
            val currentFavorites = (uiState.value as? UiState.Success)?.data?.toMutableList() ?: mutableListOf()
            currentFavorites.removeAll { it.fixtureId == item.fixtureId }
            _uiState.value = UiState.Success(currentFavorites.sortedBy {
                it.mDate?.let { LocalDate.parse(it) } ?: LocalDate.MIN
            })
            cancelNotification(item.fixtureId.toString())
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }
}


    private fun scheduleNotification(items: List<FavoriteItem>) {
        items.forEach { item ->
            val currentTime = item.mTime ?: return@forEach
            val currentDate = item.mDate ?: return@forEach

            val data = workDataOf("currentTime" to currentTime, "currentDate" to currentDate)
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest = PeriodicWorkRequestBuilder<CheckDueItemsWorker>(1, TimeUnit.DAYS)
                .setInputData(data)
                .setConstraints(constraints)
                .build()

            val uniqueWorkName = "checkDueItems_${item.fixtureId}"

            viewModelScope.launch(Dispatchers.IO) {
                val existingWork = workManagerWrapper.getWorkInfosForUniqueWork(uniqueWorkName)
                if (existingWork.isEmpty()) {
                    workManagerWrapper.enqueueUniquePeriodicWork(uniqueWorkName, workRequest)
                }
            }
        }
    }

    internal fun cancelNotification(fixtureId: String) {
        workManagerWrapper.cancelUniqueWork("checkDueItems_$fixtureId")
    }
}