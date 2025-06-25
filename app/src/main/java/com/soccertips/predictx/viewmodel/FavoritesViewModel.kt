package com.soccertips.predictx.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import com.soccertips.predictx.R
import com.soccertips.predictx.data.local.dao.FavoriteDao
import com.soccertips.predictx.data.local.entities.FavoriteItem
import com.soccertips.predictx.notification.NotificationScheduler
import com.soccertips.predictx.notification.UpdateMatchNotificationWorker
import com.soccertips.predictx.ui.UiState
import com.soccertips.predictx.util.FavoriteCleanupWorker
import com.soccertips.predictx.util.WorkManagerWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.S)
@HiltViewModel
class FavoritesViewModel
@Inject
constructor(
        application: Application,
        private val favoriteItemDao: FavoriteDao,
        private val workManagerWrapper: WorkManagerWrapper,
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
        scheduleCleanupCheck()
    }

    private fun scheduleCleanupCheck() {
        // Schedule periodic cleanup
        val periodicCleanupWorkRequest =
                PeriodicWorkRequestBuilder<FavoriteCleanupWorker>(
                                6, // Check every 6 hours
                                TimeUnit.HOURS
                        )
                        .addTag("favorite_cleanup_periodic") // This method is part of the Builder
                        .build()

        workManagerWrapper.enqueueUniquePeriodicWork(
                "favorite_cleanup_periodic",
                periodicCleanupWorkRequest
        )

        // Enqueue an immediate cleanup task as well
        val immediateCleanupWorkRequest =
                OneTimeWorkRequestBuilder<FavoriteCleanupWorker>()
                        .addTag("favorite_cleanup_immediate") // This method is part of the Builder
                        .build()
        workManagerWrapper.enqueueUniqueOneTimeWork(
                "favorite_cleanup_immediate",
                immediateCleanupWorkRequest
        )
    }

    private fun scheduleNotificationUpdates(item: FavoriteItem) {
        val updateData = Data.Builder().putString("fixtureId", item.fixtureId).build()

        val updateWork =
                PeriodicWorkRequestBuilder<UpdateMatchNotificationWorker>(
                                // 15, // Repeat interval
                                // test
                                1, // Repeat interval
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
        workManagerWrapper.cancelUniqueWork(
                "update_notification_$fixtureId"
        ) // Cancel update worker
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun loadFavorites() {
        viewModelScope.launch {

            // use a flag to check if the notification is already scheduled in this session
            val sharedPrefs =
                    getApplication<Application>()
                            .getSharedPreferences("notification_tracking", Context.MODE_PRIVATE)

            // For testing: Always reset the notification scheduled flag
            sharedPrefs.edit { putBoolean("is_notification_scheduled", false) }
            val isNotificationScheduled = sharedPrefs.getBoolean("is_notification_scheduled", false)

            favoriteItemDao.getAllFavoritesFlow().distinctUntilChanged().collect { favoriteItems ->
                val sortedItems =
                        favoriteItems.sortedBy { item ->
                            val date = item.mDate?.let { LocalDate.parse(it) } ?: LocalDate.MIN
                            date
                        }
                _uiState.value = UiState.Success(sortedItems)

                // Schedule notifications since we reset the flag
                if (!isNotificationScheduled) {
                    Timber.d("Scheduling notifications for ${sortedItems.size} favorites")
                    scheduleNotification(sortedItems)
                    // Update the flag to indicate that notifications have been scheduled
                    sharedPrefs.edit { putBoolean("is_notification_scheduled", true) }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun restoreFavorites(item: FavoriteItem) {
        viewModelScope.launch {
            try {
                favoriteItemDao.insertFavoriteItem(item)
                val currentFavorites =
                        (uiState.value as? UiState.Success)?.data?.toMutableList()
                                ?: mutableListOf()
                currentFavorites.add(item)
                _uiState.value =
                        UiState.Success(
                                currentFavorites.sortedBy {
                                    it.mDate?.let { LocalDate.parse(it) } ?: LocalDate.MIN
                                }
                        )
                scheduleNotification(currentFavorites)
            } catch (e: Exception) {
                _uiState.value =
                        UiState.Error(getApplication<Application>().getString(R.string.an_unexpected_error_occurred))
                Timber.e("Error restoring favorites: ${e.localizedMessage}")
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
                        (uiState.value as? UiState.Success)?.data?.toMutableList()
                                ?: mutableListOf()
                currentFavorites.removeAll { it.fixtureId == item.fixtureId }
                _uiState.value =
                        UiState.Success(
                                currentFavorites.sortedBy {
                                    it.mDate?.let { LocalDate.parse(it) } ?: LocalDate.MIN
                                }
                        )
                showSnackbar(
                        message = getApplication<Application>().getString(R.string.removed_from_favorites),
                        actionLabel = getApplication<Application>().getString(R.string.undo),
                        onActionPerformed = { restoreFavorites(item) }
                )
                cancelNotification(item.fixtureId.toString())
            } catch (e: Exception) {
                _uiState.value =
                        UiState.Error(
                                e.localizedMessage
                                        ?: getApplication<Application>().getString(R.string.an_unexpected_error_occurred)
                        )
            }
        }
    }
}
