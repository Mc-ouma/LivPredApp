package com.soccertips.predcompose.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predcompose.NotificationReceiver
import com.soccertips.predcompose.data.local.dao.FavoriteDao
import com.soccertips.predcompose.data.local.entities.FavoriteItem
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.util.WorkManagerWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.S)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    application: Application,
    private val favoriteItemDao: FavoriteDao,
    private val workManagerWrapper: WorkManagerWrapper,
   private val context: Context
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
                favoriteItemDao.insert(item)
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
                favoriteItemDao.delete(fixtureId = item.fixtureId.toString())
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


    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleNotification(items: List<FavoriteItem>) {
        items.forEach { item ->
            val currentTime = item.mTime ?: return@forEach
            val currentDate = item.mDate ?: return@forEach

            // Parse the date and time
            val dateTime = LocalDateTime.parse(
                "$currentDate $currentTime",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            )
            val notificationTime = dateTime.minusMinutes(15)
            val delay = Duration.between(LocalDateTime.now(), notificationTime).toMillis()

            if (delay > 0) {
                val alarmManager =
                    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (alarmManager.canScheduleExactAlarms()) {
                    try {


                        val intent = Intent(
                            getApplication<Application>(),
                            NotificationReceiver::class.java
                        ).apply {
                            putExtra("fixtureId", item.fixtureId)
                            putExtra("currentTime", currentTime)
                            putExtra("currentDate", currentDate)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            item.fixtureId.toInt(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + delay,
                            pendingIntent
                        )
                    } catch (e: SecurityException) {
                        Timber.e("Cannot Schedule exact alarm: ${e.message}")

                    }
                } else {
                    Timber.e("App does not have permission to schedule exact alarms")
                }
            }
        }
    }

    internal fun cancelNotification(fixtureId: String) {
        workManagerWrapper.cancelUniqueWork("checkDueItems_$fixtureId")
    }
}