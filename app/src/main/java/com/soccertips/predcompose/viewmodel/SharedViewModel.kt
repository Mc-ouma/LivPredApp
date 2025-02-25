package com.soccertips.predcompose.viewmodel

import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predcompose.data.model.lastfixtures.FixtureDetails
import com.soccertips.predcompose.data.model.standings.TeamStanding
import com.soccertips.predcompose.repository.FixtureDetailsRepository
import com.soccertips.predcompose.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val repository: FixtureDetailsRepository
) : ViewModel() {

    var isSplashCompleted by mutableStateOf(false)
        private set

    fun markSplashCompleted() {
        isSplashCompleted = true
    }
    // Cache duration in milliseconds (e.g., 5 minutes)
    private val cacheDuration = 15 * 60 * 1000L

    // Define a data class to hold cached data and its expiry timestamp
    data class CachedData<T>(val data: T, val expiryTime: Long)

    // Create a cache for fixtures and standings
    private val fixturesCache = LruCache<String, CachedData<UiState<List<FixtureWithType>>>>(10)
    private val standingsCache = LruCache<String, CachedData<UiState<List<TeamStanding>>>>(10)

    private val _fixturesState = MutableStateFlow<UiState<List<FixtureWithType>>>(UiState.Loading)
    val fixturesState: StateFlow<UiState<List<FixtureWithType>>> = _fixturesState.asStateFlow()

    private val _standingsState = MutableStateFlow<UiState<List<TeamStanding>>>(UiState.Loading)
    val standingsState: StateFlow<UiState<List<TeamStanding>>> = _standingsState.asStateFlow()

    var isFixturesDataLoaded = false
    var isStandingsDataLoaded = false

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

    fun fetchFixtures(season: String, homeTeamId: String, awayTeamId: String, last: String) {
        viewModelScope.launch {
            fetchData(
                cache = fixturesCache,
                cacheKey = "fixtures_$season-$homeTeamId-$awayTeamId-$last",
                dataFetcher = {
                    try {
                        val homeResponse = repository.getLastFixtures(season, homeTeamId, last)
                        val awayResponse = repository.getLastFixtures(season, awayTeamId, last)

                        val homeFixtures =
                            homeResponse.response.map {
                                FixtureWithType(
                                    it,
                                    isHome = true,
                                    specialId = homeTeamId
                                )
                            }
                        val awayFixtures =
                            awayResponse.response.map {
                                FixtureWithType(
                                    it,
                                    isHome = false,
                                    specialId = awayTeamId
                                )
                            }

                        val combinedFixtures = homeFixtures + awayFixtures
                        UiState.Success(combinedFixtures)
                    } catch (e: Exception) {
                        UiState.Error(e.message ?: "An error occurred while fetching fixtures.")
                    }
                },
                stateFlow = _fixturesState
            )
        }
    }

    data class FixtureWithType(
        val fixture: FixtureDetails,
        val isHome: Boolean, // True for home team, false for away team
        val specialId: String
    )

    fun fetchStandings(leagueId: String, season: String) {
        viewModelScope.launch {
            fetchData(
                cache = standingsCache,
                cacheKey = "standings_$leagueId-$season",
                dataFetcher = {
                    try {
                        val standings = repository.getStandings(leagueId, season).response[0].league.standings.flatten()
                        UiState.Success(standings)
                    } catch (e: Exception) {
                        UiState.Error(e.message ?: "Failed to fetch standings")
                    }
                },
                stateFlow = _standingsState
            )
        }
    }
}
