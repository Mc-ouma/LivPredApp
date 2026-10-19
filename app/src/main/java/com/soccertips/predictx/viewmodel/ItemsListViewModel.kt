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

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    // Fetch data using Stale-While-Revalidate with batch date grouping
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

        // If no cached success state is currently visible for this date, show Loading
        if (_dateUiStates.value[resolvedDate] !is UiState.Success) {
            updateDateState(resolvedDate, UiState.Loading)
        }

        viewModelScope.launch {
            try {
                repository.getCategoryDataFlow(categoryEndpoint).collect { response ->
                    processResponse(categoryEndpoint, response, resolvedDate)
                }
            } catch (e: Exception) {
                if (_dateUiStates.value[resolvedDate] !is UiState.Success) {
                    updateDateState(resolvedDate, UiState.Error(e.localizedMessage ?: "An unexpected error occurred."))
                }
            }
        }
    }

    private suspend fun processResponse(categoryEndpoint: String, response: com.soccertips.predictx.data.model.RootResponse, resolvedDate: LocalDate) = withContext(Dispatchers.Default) {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val currentTime = System.currentTimeMillis()

        // Parse and map all items in the category response in a single pass
        val parsedItems = response.serverResponse
            .filter { serverResponse ->
                val mDate = serverResponse.mDate ?: ""
                mDate != "0000-00-00" && mDate.isNotBlank()
            }
            .mapNotNull { serverResponse ->
                val mDate = serverResponse.mDate ?: return@mapNotNull null
                val parsedDate = try {
                    LocalDate.parse(mDate, DATE_FORMATTER)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing date: $mDate")
                    return@mapNotNull null
                }

                val color = when (serverResponse.outcome?.lowercase()) {
                    "win" -> Color.Green
                    "lose" -> Color.Red
                    else -> Color.Unspecified
                }

                val localTime = TimeZoneConverter.convertUtcToLocal(
                    serverResponse.mTime,
                    serverResponse.mDate
                )

                parsedDate to ServerResponse(
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

        // Group items by LocalDate and sort by time
        val groupedByDate = parsedItems
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.sortedBy { it.mTime } }

        // Update tomorrow has items flag
        val tomorrowItems = groupedByDate[tomorrow]
        _tomorrowHasItems.value = !tomorrowItems.isNullOrEmpty()

        // Pre-populate all dates in the standard pager range (past 5 days, today, tomorrow)
        val pastDays = 5
        val allDatesInRange = (0..pastDays).map { today.minusDays(it.toLong()) } + tomorrow

        val newStates = mutableMapOf<LocalDate, UiState<List<ServerResponse>>>()
        allDatesInRange.forEach { date ->
            val items = groupedByDate[date] ?: emptyList()
            val cacheKey = "${categoryEndpoint}_$date"
            cachedData.put(cacheKey, currentTime to items)
            newStates[date] = UiState.Success(items)
        }

        // Also include any other dates present in the response outside the default range
        groupedByDate.forEach { (date, items) ->
            if (!newStates.containsKey(date)) {
                val cacheKey = "${categoryEndpoint}_$date"
                cachedData.put(cacheKey, currentTime to items)
                newStates[date] = UiState.Success(items)
            }
        }

        withContext(Dispatchers.Main) {
            _dateUiStates.value = _dateUiStates.value + newStates
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
