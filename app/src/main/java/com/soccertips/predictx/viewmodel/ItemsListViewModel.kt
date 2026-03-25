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
import com.soccertips.predictx.utils.TimeZoneConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
class ItemsListViewModel
@Inject
constructor(private val repository: PredictionRepository, private val favoriteDao: FavoriteDao) :
        ViewModel() {

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
    // Per-date UI state so each pager page has independent state
    private val _dateUiStates = MutableStateFlow<Map<LocalDate, UiState<List<ServerResponse>>>>(emptyMap())
    val dateUiStates: StateFlow<Map<LocalDate, UiState<List<ServerResponse>>>> = _dateUiStates.asStateFlow()

    private val _tomorrowHasItems = MutableStateFlow(false)
    val tomorrowHasItems: StateFlow<Boolean> = _tomorrowHasItems.asStateFlow()

    // Cache of favorite fixture IDs for efficient lookups - reactively updated
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        // Observe favorites and update the cached set reactively
        viewModelScope.launch {
            favoriteDao.getAllFavoritesFlow().collect { favorites ->
                _favoriteIds.value = favorites.map { it.fixtureId }.toSet()
            }
        }
    }

    // Fetch data only if not already cached for the given date
    fun fetchItems(categoryEndpoint: String, date: LocalDate?) {
        val resolvedDate = date ?: LocalDate.now()
        val cacheKey = "${categoryEndpoint}_$resolvedDate"
        val cachedItems = cachedData.get(cacheKey)

        if (cachedItems != null) {
            val (timestamp, items) = cachedItems
            val currentTime = System.currentTimeMillis()
            if (currentTime - timestamp < cacheExpirationDuration) {
                updateDateState(resolvedDate, UiState.Success(items))
                return
            } else {
                cachedData.remove(cacheKey) // Remove expired cache
            }
        }

        viewModelScope.launch {
            updateDateState(resolvedDate, UiState.Loading)
            try {
                val response = repository.getCategoryData(categoryEndpoint)

                val items =
                        withContext(Dispatchers.IO) {
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            response.serverResponse
                                    .filter { serverResponse ->
                                        val mDate = serverResponse.mDate ?: ""
                                        mDate != "0000-00-00" &&
                                                try {
                                                    LocalDate.parse(mDate, formatter) == resolvedDate
                                                } catch (e: Exception) {
                                                    Timber.e(e, "Error parsing date: $mDate")
                                                    false // Ignore items with invalid dates
                                                }
                                    }
                                    .map { serverResponse ->
                                        val color =
                                                when (serverResponse.outcome?.lowercase()) {
                                                    "win" -> Color.Green
                                                    "lose" -> Color.Red
                                                    else -> Color.Unspecified
                                                }

                                        // Convert UTC time to local timezone
                                        val localTime = TimeZoneConverter.convertUtcToLocal(
                                            serverResponse.mTime,
                                            serverResponse.mDate
                                        )

                                        ServerResponse(
                                                fixtureId = serverResponse.fixtureId ?: "",
                                                pick = serverResponse.pick ?: "Unknown",
                                                homeTeam = serverResponse.homeTeam ?: "Unknown",
                                                awayTeam = serverResponse.awayTeam ?: "Unknown",
                                                mDate = serverResponse.mDate ?: "Unknown",
                                                league = serverResponse.league ?: "Unknown",
                                                mTime = localTime,
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
                                .sortedBy { it.mTime }
                        }

                cachedData.put(cacheKey, System.currentTimeMillis() to items)
                updateDateState(resolvedDate, UiState.Success(items))
            } catch (e: Exception) {
                updateDateState(resolvedDate, UiState.Error(e.localizedMessage ?: "An unexpected error occurred."))
            }
        }
    }

    private fun updateDateState(date: LocalDate, state: UiState<List<ServerResponse>>) {
        _dateUiStates.value = _dateUiStates.value + (date to state)
    }

    fun checkTomorrowItems(categoryEndpoint: String) {
        val tomorrow = LocalDate.now().plusDays(1)
        val cacheKey = "${categoryEndpoint}_$tomorrow"
        val cachedItems = cachedData.get(cacheKey)

        if (cachedItems != null) {
            val (timestamp, items) = cachedItems
            if (System.currentTimeMillis() - timestamp < cacheExpirationDuration) {
                _tomorrowHasItems.value = items.isNotEmpty()
                return
            }
        }

        viewModelScope.launch {
            try {
                val response = repository.getCategoryData(categoryEndpoint)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val hasItems = withContext(Dispatchers.IO) {
                    response.serverResponse.any { serverResponse ->
                        val mDate = serverResponse.mDate ?: ""
                        mDate != "0000-00-00" && try {
                            LocalDate.parse(mDate, formatter) == tomorrow
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
                _tomorrowHasItems.value = hasItems
            } catch (e: Exception) {
                _tomorrowHasItems.value = false
            }
        }
    }

    // Toggle favorite status for an item
    fun toggleFavorite(item: ServerResponse) {
        viewModelScope.launch {
            val favoriteItem =
                    FavoriteItem(
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

    // Check if an item is a favorite - non-blocking lookup from cached set
    fun isFavorite(item: ServerResponse): Boolean {
        return item.fixtureId in _favoriteIds.value
    }
}
