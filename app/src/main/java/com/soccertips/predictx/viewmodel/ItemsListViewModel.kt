package com.soccertips.predictx.viewmodel

import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predictx.data.local.dao.FavoriteDao
import com.soccertips.predictx.data.local.entities.FavoriteItem
import com.soccertips.predictx.data.model.ServerResponse
import com.soccertips.predictx.repository.PredictionRepository
import com.soccertips.predictx.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ItemsListViewModel @Inject constructor(
    private val repository: PredictionRepository,
    private val favoriteDao: FavoriteDao
) : ViewModel() {

    // Cache for fetched data with timestamps
    private val maxCacheSize = 20
    private val cacheExpirationDuration =
        12 * 60 * 60 * 1000 // Cache expires in 12 hours (in milliseconds)

    private val cachedData =
        object : LruCache<String, Pair<Long, List<ServerResponse>>>(maxCacheSize) {
            override fun entryRemoved(
                evicted: Boolean,
                key: String?,
                oldValue: Pair<Long, List<ServerResponse>>?,
                newValue: Pair<Long, List<ServerResponse>>?
            ) {
                if (evicted) {
                    // Log cache eviction
                    Timber.d("Cache evicted for key: $key")
                }
            }
        }
    private val _uiState = MutableStateFlow<UiState<List<ServerResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ServerResponse>>> = _uiState.asStateFlow()



    // Fetch data only if not already cached for the given date
    fun fetchItems(categoryEndpoint: String, date: LocalDate?) {
        val cacheKey = "${categoryEndpoint}_$date"
        val cachedItems = cachedData.get(cacheKey)

        if (cachedItems != null) {
            val (timestamp, items) = cachedItems
            val currentTime = System.currentTimeMillis()
            if (currentTime - timestamp < cacheExpirationDuration) {
                _uiState.value = UiState.Success(items)
                return
            } else {
                cachedData.remove(cacheKey) // Remove expired cache
            }
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.getCategoryData(categoryEndpoint)

                val items = withContext(Dispatchers.IO) {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    response.serverResponse.filter { serverResponse ->
                        val mDate = serverResponse.mDate ?: ""
                        mDate != "0000-00-00" &&
                                try {
                                    LocalDate.parse(mDate, formatter) == date
                                } catch (e: Exception) {
                                    Timber.e(e, "Error parsing date: $mDate")
                                    false // Ignore items with invalid dates
                                }
                    }.map { serverResponse ->
                        val color = when (serverResponse.outcome) {
                            "win" -> Color.Green
                            "lose" -> Color.Red
                            else -> Color.Unspecified
                        }
                        ServerResponse(
                            fixtureId = serverResponse.fixtureId ?: "",
                            pick = serverResponse.pick ?: "Unknown",
                            homeTeam = serverResponse.homeTeam ?: "Unknown",
                            awayTeam = serverResponse.awayTeam ?: "Unknown",
                            mDate = serverResponse.mDate ?: "Unknown",
                            league = serverResponse.league ?: "Unknown",
                            mTime = serverResponse.mTime ?: "Unknown",
                            betOdds = serverResponse.betOdds ?: "Unknown",
                            outcome = serverResponse.outcome ?: "Unknown",
                            htScore = serverResponse.htScore ?: "Unknown",
                            result = serverResponse.result ?: "Unknown",
                            hLogoPath = serverResponse.hLogoPath ?: "Unknown",
                            aLogoPath = serverResponse.aLogoPath ?: "Unknown",
                            leagueLogo = serverResponse.leagueLogo ?: "Unknown",
                            mStatus = serverResponse.mStatus ?: "Unknown",
                            color = color,
                        )
                    }
                }

                cachedData.put(cacheKey, System.currentTimeMillis() to items)
                _uiState.value = UiState.Success(items)

            } catch (e: Exception) {
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }

    // Toggle favorite status for an item
    fun toggleFavorite(item: ServerResponse) {
        viewModelScope.launch {
            val favoriteItem = FavoriteItem(
                fixtureId = item.fixtureId ?: "",
                homeTeam = item.homeTeam ?: "",
                awayTeam = item.awayTeam ?: "",
                league = item.league?.split(",")?.firstOrNull() ?: "",
                mTime = item.mTime,
                hLogoPath = item.hLogoPath,
                aLogoPath = item.aLogoPath,
                leagueLogo = item.leagueLogo,
                mDate = item.mDate,
                mStatus = item.mStatus,
                pick = item.pick,
                outcome = item.outcome,
                color = item.color.toArgb(),
                completedTimestamp = item.completedTimestamp

                )
            if (isFavorite(item)) {
                favoriteDao.deleteFavoriteItem(favoriteItem.fixtureId)
                /*_uiState.value = UiState.Success(
                    (_uiState.value as? UiState.Success)?.data?.filter { it.fixtureId != item.fixtureId }
                        ?: emptyList()
                )*/
            } else {
                favoriteDao.insertFavoriteItem(favoriteItem)
               /* _uiState.value = UiState.Success(
                    (_uiState.value as? UiState.Success)?.data?.plus(item) ?: listOf(item)
                )*/
            }
        }
    }

    // Check if an item is a favorite
    suspend fun isFavorite(item: ServerResponse): Boolean {
        return favoriteDao.getAllFavorites().any { it.fixtureId == item.fixtureId }
    }

}
