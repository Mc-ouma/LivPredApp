package com.soccertips.predictx.viewmodel

import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predictx.data.model.Event
import com.soccertips.predictx.data.model.events.FixtureEvent
import com.soccertips.predictx.data.model.headtohead.FixtureDetails
import com.soccertips.predictx.data.model.lineups.TeamLineup
import com.soccertips.predictx.data.model.prediction.Response
import com.soccertips.predictx.repository.FixtureDetailsRepository
import com.soccertips.predictx.ui.FixtureDetailsUiState
import com.soccertips.predictx.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.soccertips.predictx.data.model.statistics.Response as StatsResponse

@HiltViewModel
class FixtureDetailsViewModel @Inject constructor(
    private val fixtureDetailsRepository: FixtureDetailsRepository,
) : ViewModel() {

    private val _uiState: MutableStateFlow<FixtureDetailsUiState> =
        MutableStateFlow(FixtureDetailsUiState.Loading)
    val uiState: StateFlow<FixtureDetailsUiState> = _uiState.asStateFlow()

    private val _headToHeadState =
        MutableStateFlow<UiState<List<FixtureDetails>>>(UiState.Loading)
    val headToHeadState: StateFlow<UiState<List<FixtureDetails>>> =
        _headToHeadState.asStateFlow()

    private val _lineupsState = MutableStateFlow<UiState<List<TeamLineup>>>(UiState.Loading)
    val lineupsState: StateFlow<UiState<List<TeamLineup>>> = _lineupsState.asStateFlow()

    private val _fixtureStatsState =
        MutableStateFlow<UiState<List<StatsResponse>>>(UiState.Loading)
    val fixtureStatsState: StateFlow<UiState<List<StatsResponse>>> =
        _fixtureStatsState.asStateFlow()

    private val _fixtureEventsState = MutableStateFlow<UiState<List<FixtureEvent>>>(UiState.Loading)
    val fixtureEventsState: StateFlow<UiState<List<FixtureEvent>>> =
        _fixtureEventsState.asStateFlow()

    private val _predictionsState = MutableStateFlow<UiState<List<Response>>>(UiState.Loading)
    val predictionsState: StateFlow<UiState<List<Response>>> = _predictionsState.asStateFlow()

    // Cache duration in milliseconds (e.g., 15 minutes)
    private val cacheDuration = 15 * 60 * 1000L

    // Define a data class to hold cached data and its expiry timestamp
    data class CachedData<T>(val data: T, val expiryTime: Long)

    // Create a cache for each data type
    private val fixtureDetailsCache = LruCache<String, CachedData<FixtureDetailsUiState>>(10)
    private val headToHeadCache = LruCache<String, CachedData<UiState<List<FixtureDetails>>>>(10)
    private val lineupsCache = LruCache<String, CachedData<UiState<List<TeamLineup>>>>(10)
    private val fixtureStatsCache = LruCache<String, CachedData<UiState<List<StatsResponse>>>>(10)
    private val fixtureEventsCache = LruCache<String, CachedData<UiState<List<FixtureEvent>>>>(10)
    private val predictionsCache = LruCache<String, CachedData<UiState<List<Response>>>>(10)

    // Generic function to fetch data and use cache
    private suspend fun <T> fetchData(
        cache: LruCache<String, CachedData<UiState<T>>>,
        cacheKey: String,
        dataFetcher: suspend () -> UiState<T>,
        stateFlow: MutableStateFlow<UiState<T>>
    ) {
        val cachedData = cache.get(cacheKey)
        val currentTime = System.currentTimeMillis()

        if (cachedData != null && cachedData.expiryTime > currentTime) {
            // Use cached data if it's not expired
            stateFlow.value = cachedData.data
            Timber.d("Data fetched from cache for key: $cacheKey")
        } else {
            // Fetch new data
            stateFlow.value = UiState.Loading
            try {
                val result = dataFetcher()
                cache.put(cacheKey, CachedData(result, currentTime + cacheDuration))
                stateFlow.value = result
                Timber.d("Data fetched from network for key: $cacheKey")
            } catch (e: Exception) {
                stateFlow.value = UiState.Error(e.message ?: "Failed to fetch data")
                Timber.e(e, "Error fetching data for key: $cacheKey")
            }
        }
    }


    fun fetchFixtureDetails(fixtureId: String) {
        viewModelScope.launch {
            val cachedData = fixtureDetailsCache.get(fixtureId)
            val currentTime = System.currentTimeMillis()

            if (cachedData != null && cachedData.expiryTime > currentTime) {
                // Use cached data if it's not expired
                _uiState.value = cachedData.data
                Timber.d("Fixture details fetched from cache for fixtureId: $fixtureId")
            } else {
                // Fetch new data
                _uiState.value = FixtureDetailsUiState.Loading
                try {
                    val fixtureDetails = fixtureDetailsRepository.getFixtureDetails(fixtureId)
                    val successState = FixtureDetailsUiState.Success(
                        fixtureDetails = fixtureDetails,
                        predictions = null,
                        fixtureStats = null,
                        headToHead = null,
                        lineups = null,
                        fixtureEvents = null
                    )
                    fixtureDetailsCache.put(
                        fixtureId,
                        CachedData(successState, currentTime + cacheDuration)
                    )
                    _uiState.value = successState
                    Timber.d("Fixture details fetched successfully for fixtureId: $fixtureId")
                } catch (e: Exception) {
                    _uiState.value =
                        FixtureDetailsUiState.Error(e.message ?: "Failed to fetch fixture details")
                    Timber.e(e, "Failed to fetch fixture details for fixtureId: $fixtureId")
                }
            }
        }
    }

    fun fetchFixtureStats(fixtureId: String, homeTeamId: String, awayTeamId: String) {
        viewModelScope.launch {
            fetchData(
                cache = fixtureStatsCache,
                cacheKey = "fixture_stats_$fixtureId",
                dataFetcher = {
                    try {
                        val homeTeamStats =
                            fixtureDetailsRepository.getFixtureStats(fixtureId, homeTeamId).response
                        val awayTeamStats =
                            fixtureDetailsRepository.getFixtureStats(fixtureId, awayTeamId).response

                        val combinedStats = homeTeamStats + awayTeamStats
                        if (combinedStats.isNotEmpty()) {
                            UiState.Success(combinedStats)
                        } else {
                            UiState.Error("No fixture stats available yet")
                        }
                    } catch (e: Exception) {
                        UiState.Error("Failed to fetch fixture stats")

                    }
                },
                stateFlow = _fixtureStatsState
            )
        }
    }

    fun fetchFixtureEvents(fixtureId: String) {
        viewModelScope.launch {
            fetchData(
                cache = fixtureEventsCache,
                cacheKey = "fixture_events_$fixtureId",
                dataFetcher = {
                    try {
                        val fixtureEvents =
                            fixtureDetailsRepository.getFixtureEvents(fixtureId).response
                        if (fixtureEvents.isNotEmpty()) {
                            UiState.Success(fixtureEvents)
                        } else {
                            UiState.Error("No fixture events available yet")
                        }
                    } catch (e: Exception) {
                        UiState.Error("Failed to fetch fixture events")
                    }
                },
                stateFlow = _fixtureEventsState
            )
        }
    }

    fun fetchHeadToHead(homeTeamId: String, awayTeamId: String) {
        viewModelScope.launch {
            fetchData(
                cache = headToHeadCache,
                cacheKey = "head_to_head_$homeTeamId-$awayTeamId",
                dataFetcher = {
                    try {
                        val headToHead = fixtureDetailsRepository.getHeadToHeadFixtures(
                            "$homeTeamId-$awayTeamId",
                            "10"
                        ).response
                        if (headToHead.isNotEmpty()) {
                            UiState.Success(headToHead)
                        } else {
                            UiState.Error("No head to head data available yet")
                        }
                    } catch (e: Exception) {
                        UiState.Error("Failed to fetch head to head")
                    }
                },
                stateFlow = _headToHeadState
            )
        }
    }

    fun fetchLineups(fixtureId: String) {
        viewModelScope.launch {
            fetchData(
                cache = lineupsCache,
                cacheKey = "lineups_$fixtureId",
                dataFetcher = {
                    try {
                        val lineups = fixtureDetailsRepository.getLineups(fixtureId).response
                        if (lineups.isNotEmpty()) {
                            UiState.Success(lineups)
                        } else {
                            UiState.Error("No lineups available yet")
                        }
                    } catch (e: Exception) {
                        UiState.Error("Failed to fetch lineups")
                    }
                },
                stateFlow = _lineupsState
            )
        }
    }

    fun fetchFormAndPredictions(fixtureId: String) {
        viewModelScope.launch {
            fetchData(
                cache = predictionsCache,
                cacheKey = "predictions_$fixtureId",
                dataFetcher = {
                    try {
                        val predictions =
                            fixtureDetailsRepository.getPredictions(fixtureId).response
                        if (predictions.isNotEmpty()) {
                            UiState.Success(predictions)
                        } else {
                            UiState.Error("No predictions available yet")
                        }
                    } catch (e: Exception) {
                        UiState.Error("Failed to fetch predictions")
                    }
                },
                stateFlow = _predictionsState
            )
        }
    }

    fun getHomeGoalScorers(
        events: List<Event>,
        homeTeamId: Int
    ): List<Pair<String, String>> =
        events
            .filter { it.type == "Goal" && it.team.id == homeTeamId }
            .mapNotNull {
                val playerName = it.player.name
                if (playerName.isBlank()) {
                    null
                } else {
                    //val lastName = playerName.split(" ").lastOrNull() ?: ""
                    val elapsed = "${it.time.elapsed}${it.time.extra?.let { "+$it" } ?: ""}’"
                    playerName to elapsed
                }
            }

    fun getAwayGoalScorers(
        events: List<Event>,
        awayTeamId: Int,
    ): List<Pair<String, String>> =
        events
            .filter { it.type == "Goal" && it.team.id == awayTeamId }
            .mapNotNull {
                val playerName = it.player.name
                if (playerName.isBlank()) {
                    null
                } else {
                    //val lastName = playerName.split(" ").lastOrNull() ?: ""
                    val elapsed = "${it.time.elapsed}${it.time.extra?.let { "+$it" } ?: ""}’"
                    playerName to elapsed
                }
            }

    fun formatTimestamp(timestamp: Long): String {
        val formatter =
            DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.ofEpochSecond(timestamp))
    }

    fun getMatchStatusText(
        status: String,
        elapsed: Int,
        timestamp: Long,
    ): String =
        when (status) {
            "Match Finished", "FT" -> ""
            else -> {
                if (elapsed > 0) {
                    "$elapsed’"
                } else {
                    formatTimestamp(timestamp)
                }
            }
        }
}
