package com.soccertips.predcompose.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.PeriodicWorkRequestBuilder
import com.soccertips.predcompose.data.local.dao.FavoriteDao
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import com.soccertips.predcompose.notification.NotificationScheduler
import com.soccertips.predcompose.notification.UpdateMatchNotificationWorker
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.util.WorkManagerWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.S)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    application: Application,
    private val favoriteItemDao: FavoriteDao,
    private val workManagerWrapper: WorkManagerWrapper,
    context: Context,
    private val notificationScheduler: NotificationScheduler,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState<List<FavoriteItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<FavoriteItem>>> = _uiState.asStateFlow()

    val favoriteCount = favoriteItemDao.getFavoriteCount().distinctUntilChanged()

    // Use MutableSharedFlow for snackbar events
    private val _snackbarFlow = MutableStateFlow<SnackbarData?>(null)
    val snackbarFlow: StateFlow<SnackbarData?> = _snackbarFlow

    fun showSnackbar(
        message: String,
        actionLabel: String,
        onActionPerformed: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _snackbarFlow.value = (SnackbarData(message, actionLabel, onActionPerformed))

        }
    }

    fun resetSnackbar() {
        _snackbarFlow.value = null
    }

    data class SnackbarData(
        val message: String,
        val actionLabel: String,
        val onActionPerformed: (() -> Unit)?
    )

    init {
        loadFavorites()
    }

    private fun scheduleNotificationUpdates(item: FavoriteItem) {
        val updateData = Data.Builder()
            .putString("fixtureId", item.fixtureId)
            .build()

        val updateWork = PeriodicWorkRequestBuilder<UpdateMatchNotificationWorker>(
            15, // Repeat interval
            TimeUnit.MINUTES
        )
            .setInputData(updateData)
            .addTag("update_notification_${item.fixtureId}")
            .build()

        workManagerWrapper.enqueueUniquePeriodicWork(
            "update_notification_${item.fixtureId}",
            updateWork
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleNotification(items: List<FavoriteItem>) {
        items.forEach { item ->
            notificationScheduler.scheduleMatchNotification(item)
            scheduleNotificationUpdates(item) // Schedule updates
        }
    }

    internal fun cancelNotification(fixtureId: String) {
        workManagerWrapper.cancelUniqueWork("checkDueItems_$fixtureId")
        workManagerWrapper.cancelUniqueWork("update_notification_$fixtureId") // Cancel update worker
    }

    @RequiresApi(Build.VERSION_CODES.S)
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

    @RequiresApi(Build.VERSION_CODES.S)
    fun restoreFavorites(item: FavoriteItem) {
        viewModelScope.launch {
            try {
                favoriteItemDao.insertFavoriteItem(item)
                val currentFavorites =
                    (uiState.value as? UiState.Success)?.data?.toMutableList() ?: mutableListOf()
                currentFavorites.add(item)
                _uiState.value = UiState.Success(currentFavorites.sortedBy {
                    it.mDate?.let { LocalDate.parse(it) } ?: LocalDate.MIN
                })
                scheduleNotification(currentFavorites)
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun removeFromFavorites(item: FavoriteItem) {
        viewModelScope.launch {
            try {
                favoriteItemDao.deleteFavoriteItem(fixtureId = item.fixtureId.toString())
                notificationScheduler.cancelNotification(item.fixtureId.toString())
                val currentFavorites =
                    (uiState.value as? UiState.Success)?.data?.toMutableList() ?: mutableListOf()
                currentFavorites.removeAll { it.fixtureId == item.fixtureId }
                _uiState.value = UiState.Success(currentFavorites.sortedBy {
                    it.mDate?.let { LocalDate.parse(it) } ?: LocalDate.MIN
                })
                showSnackbar(
                    message = "Removed from favorites",
                    actionLabel = "Undo",
                    onActionPerformed = {
                        restoreFavorites(item)
                    }
                )
                cancelNotification(item.fixtureId.toString())
            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }


}