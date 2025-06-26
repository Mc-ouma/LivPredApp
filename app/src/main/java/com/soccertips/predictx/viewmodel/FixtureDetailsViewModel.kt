package com.soccertips.predictx.viewmodel

import android.content.Context
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predictx.R
import com.soccertips.predictx.data.model.Event
import com.soccertips.predictx.data.model.events.FixtureEvent
import com.soccertips.predictx.data.model.headtohead.FixtureDetails
import com.soccertips.predictx.data.model.lineups.TeamLineup
import com.soccertips.predictx.data.model.prediction.Response
import com.soccertips.predictx.repository.FixtureDetailsRepository
import com.soccertips.predictx.ui.FixtureDetailsUiState
import com.soccertips.predictx.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import com.soccertips.predictx.data.model.statistics.Response as StatsResponse

@HiltViewModel
class FixtureDetailsViewModel @Inject constructor(
    private val fixtureDetailsRepository: FixtureDetailsRepository,
    context: Context,
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

    // Cache duration in milliseconds (e.g., 30 minutes for less frequent updates)
    private val cacheDuration = 30 * 60 * 1000L
    // Use shorter cache duration for live matches data (5 minutes)
    private val liveCacheDuration = 5 * 60 * 1000L

    // Define a data class to hold cached data and its expiry timestamp with statistics
    data class CachedData<T>(
        val data: T,
        val expiryTime: Long,
        val timestamp: Long = System.currentTimeMillis(),
        var hitCount: Int = 0
    )

    // Cache statistics
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)

    // Create a cache for each data type with optimized sizes
    private val fixtureDetailsCache = LruCache<String, CachedData<FixtureDetailsUiState>>(30)
    private val headToHeadCache = LruCache<String, CachedData<UiState<List<FixtureDetails>>>>(20)
    private val lineupsCache = LruCache<String, CachedData<UiState<List<TeamLineup>>>>(20)
    private val fixtureStatsCache = LruCache<String, CachedData<UiState<List<StatsResponse>>>>(20)
    private val fixtureEventsCache = LruCache<String, CachedData<UiState<List<FixtureEvent>>>>(20)
    private val predictionsCache = LruCache<String, CachedData<UiState<List<Response>>>>(20)

    // Track active fetch jobs to prevent duplicate requests
    private val activeJobs = mutableMapOf<String, Job>()

    // Generic function to fetch data and use cache with improved concurrency handling
    private suspend fun <T> fetchData(
        cache: LruCache<String, CachedData<UiState<T>>>,
        cacheKey: String,
        dataFetcher: suspend () -> UiState<T>,
        stateFlow: MutableStateFlow<UiState<T>>,
        isLiveData: Boolean = false
    ) {
        // Check for an active job with this key and return if already running
        synchronized(activeJobs) {
            if (activeJobs[cacheKey]?.isActive == true) {
                Timber.d("Skipping duplicate fetch for key: $cacheKey - already in progress")
                return
            }
        }

        val cachedData = cache.get(cacheKey)
        val currentTime = System.currentTimeMillis()
        val cachePeriod = if (isLiveData) liveCacheDuration else cacheDuration

        if (cachedData != null && cachedData.expiryTime > currentTime) {
            // Use cached data if it's not expired
            cachedData.hitCount++
            cacheHits.incrementAndGet()
            stateFlow.value = cachedData.data
            Timber.d("Data fetched from cache for key: $cacheKey (hit #${cachedData.hitCount})")
            return
        }

        // Record cache miss
        cacheMisses.incrementAndGet()

        // Create and track a new job
        val fetchJob = viewModelScope.launch {
            try {
                // Set loading state
                stateFlow.value = UiState.Loading

                // Fetch new data
                val result = dataFetcher()

                // Cache successful results
                if (result is UiState.Success) {
                    cache.put(cacheKey, CachedData(result, currentTime + cachePeriod))
                    Timber.d("Data fetched from network and cached for key: $cacheKey")
                }

                stateFlow.value = result
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                stateFlow.value = UiState.Error(e.message ?: "Failed to fetch data")
                Timber.e(e, "Error fetching data for key: $cacheKey")
            } finally {
                // Remove job from active jobs when complete
                synchronized(activeJobs) {
                    activeJobs.remove(cacheKey)
                }
            }
        }

        // Store the job
        synchronized(activeJobs) {
            activeJobs[cacheKey] = fetchJob
        }
    }

    fun fetchFixtureDetails(fixtureId: String, context: Context) {
        viewModelScope.launch {
            val cacheKey = "fixture_details_$fixtureId"

            // Check for active job
            synchronized(activeJobs) {
                if (activeJobs[cacheKey]?.isActive == true) {
                    Timber.d("Skipping duplicate fixture details fetch - already in progress")
                    return@launch
                }
            }

            val cachedData = fixtureDetailsCache.get(cacheKey)
            val currentTime = System.currentTimeMillis()

            if (cachedData != null && cachedData.expiryTime > currentTime) {
                // Use cached data if it's not expired
                cachedData.hitCount++
                cacheHits.incrementAndGet()
                _uiState.value = cachedData.data
                Timber.d("Fixture details fetched from cache (hit #${cachedData.hitCount})")
                return@launch
            }

            cacheMisses.incrementAndGet()
            _uiState.value = FixtureDetailsUiState.Loading

            val fetchJob = viewModelScope.launch {
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
                        cacheKey,
                        CachedData(successState, currentTime + cacheDuration)
                    )
                    _uiState.value = successState
                    Timber.d("Fixture details fetched successfully: $fixtureId")

                    // Prefetch related data in parallel after getting fixture details
                    prefetchRelatedData(fixtureId, fixtureDetails.teams.home.id.toString(),
                                      fixtureDetails.teams.away.id.toString(), context)

                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _uiState.value = FixtureDetailsUiState.Error(
                        e.message ?: "Failed to fetch fixture details"
                    )
                    Timber.e(e, "Failed to fetch fixture details: $fixtureId")
                } finally {
                    synchronized(activeJobs) {
                        activeJobs.remove(cacheKey)
                    }
                }
            }

            synchronized(activeJobs) {
                activeJobs[cacheKey] = fetchJob
            }
        }
    }

    // Prefetch related data in parallel to improve perceived performance
    private fun prefetchRelatedData(fixtureId: String, homeTeamId: String, awayTeamId: String, context: Context) {
        viewModelScope.launch {
            supervisorScope {
                // Use async to launch parallel requests but don't await results
                // They will be loaded into cache for fast access when user navigates to those tabs
                async { fetchFixtureStats(fixtureId, homeTeamId, awayTeamId, context) }
                async { fetchFixtureEvents(fixtureId, context) }
                async { fetchFormAndPredictions(fixtureId, context) }
                async { fetchHeadToHead(homeTeamId, awayTeamId, context) }
                async { fetchLineups(fixtureId, context) }
            }
        }
    }

    fun fetchFixtureStats(fixtureId: String, homeTeamId: String, awayTeamId: String, context: Context) {
        viewModelScope.launch {
            fetchData(
                cache = fixtureStatsCache,
                cacheKey = "fixture_stats_$fixtureId",
                dataFetcher = {
                    supervisorScope {
                        try {
                            // Launch both requests in parallel
                            val homeStatsDeferred = async {
                                fixtureDetailsRepository.getFixtureStats(fixtureId, homeTeamId).response
                            }
                            val awayStatsDeferred = async {
                                fixtureDetailsRepository.getFixtureStats(fixtureId, awayTeamId).response
                            }

                            // Await both results
                            val homeTeamStats = homeStatsDeferred.await()
                            val awayTeamStats = awayStatsDeferred.await()

                            val combinedStats = homeTeamStats + awayTeamStats
                            if (combinedStats.isNotEmpty()) {
                                UiState.Success(combinedStats)
                            } else {
                                UiState.Error(context.getString(R.string.no_data_available))
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            UiState.Error("Failed to fetch fixture stats: ${e.message}")
                        }
                    }
                },
                stateFlow = _fixtureStatsState,
                isLiveData = true // Stats can change during a match
            )
        }
    }

    fun fetchFixtureEvents(fixtureId: String, context: Context) {
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
                            UiState.Error(context.getString(R.string.no_data_available))
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        UiState.Error("Failed to fetch fixture events: ${e.message}")
                    }
                },
                stateFlow = _fixtureEventsState,
                isLiveData = true // Events change during a match
            )
        }
    }

    fun fetchHeadToHead(homeTeamId: String, awayTeamId: String, context: Context) {
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
                            UiState.Error(
                                context.getString(
                                    R.string.no_head_to_head_data_available_for_teams_and,
                                    homeTeamId,
                                    awayTeamId
                                )
                            )
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        UiState.Error("Failed to fetch head to head: ${e.message}")
                    }
                },
                stateFlow = _headToHeadState
                // Not live data - head-to-head history doesn't change during a match
            )
        }
    }

    fun fetchLineups(fixtureId: String, context: Context) {
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
                            UiState.Error(context.getString(R.string.no_data_available))
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        UiState.Error("Failed to fetch lineups: ${e.message}")
                    }
                },
                stateFlow = _lineupsState,
                isLiveData = false // Lineups don't change frequently after announced
            )
        }
    }

    fun fetchFormAndPredictions(fixtureId: String, context: Context) {
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
                            UiState.Error(context.getString(R.string.no_predictions_available))
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        UiState.Error("Failed to fetch predictions: ${e.message}")
                    }
                },
                stateFlow = _predictionsState
                // Not live data - predictions don't change during the match
            )
        }
    }

    // Get cache statistics for debugging/analytics
    fun getCacheStatistics(): Map<String, Any> {
        return mapOf(
            "hits" to cacheHits.get(),
            "misses" to cacheMisses.get(),
            "hitRatio" to if (cacheHits.get() + cacheMisses.get() > 0) {
                cacheHits.get().toFloat() / (cacheHits.get() + cacheMisses.get())
            } else 0f,
            "fixtureDetailsCacheSize" to fixtureDetailsCache.size(),
            "headToHeadCacheSize" to headToHeadCache.size(),
            "lineupsCacheSize" to lineupsCache.size(),
            "fixtureStatsCacheSize" to fixtureStatsCache.size(),
            "fixtureEventsCacheSize" to fixtureEventsCache.size(),
            "predictionsCacheSize" to predictionsCache.size()
        )
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
                    val elapsed = "${it.time.elapsed}${it.time.extra?.let { "+$it" } ?: ""}'"
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
                    val elapsed = "${it.time.elapsed}${it.time.extra?.let { "+$it" } ?: ""}'"
                    playerName to elapsed
                }
            }

    fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return "TBD" // Return a default value for invalid timestamps

        val formatter =
            DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.ofEpochSecond(timestamp))
    }

    fun getMatchStatusText(
        status: String?,
        elapsed: Int,
        timestamp: Long,
    ): String =
        when (status) {
            null -> "TBD" // Handle null status
            "Match Finished", "FT" -> ""
            else -> {
                if (elapsed > 0) {
                    "$elapsed'"
                } else if (timestamp > 0) {
                    formatTimestamp(timestamp)
                } else {
                    "TBD" // Fallback for invalid timestamps
                }
            }
        }

    override fun onCleared() {
        super.onCleared()
        // Cancel any active jobs when the ViewModel is cleared to prevent memory leaks
        synchronized(activeJobs) {
            activeJobs.values.forEach { it.cancel() }
            activeJobs.clear()
        }

        // Log cache statistics
        Timber.d("Cache statistics: ${getCacheStatistics()}")
    }
}
