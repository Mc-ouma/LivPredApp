package com.soccertips.predcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predcompose.model.Event
import com.soccertips.predcompose.model.ResponseData
import com.soccertips.predcompose.model.events.FixtureEvent
import com.soccertips.predcompose.model.events.FixtureEventsResponse
import com.soccertips.predcompose.model.headtohead.HeadToHeadResponse
import com.soccertips.predcompose.model.lastfixtures.FixtureDetails
import com.soccertips.predcompose.model.lastfixtures.FixtureListResponse
import com.soccertips.predcompose.model.lineups.FixtureLineupResponse
import com.soccertips.predcompose.model.lineups.TeamLineup
import com.soccertips.predcompose.model.prediction.PredictionResponse
import com.soccertips.predcompose.model.prediction.Response
import com.soccertips.predcompose.model.standings.StandingsResponse
import com.soccertips.predcompose.model.standings.TeamStanding
import com.soccertips.predcompose.model.statistics.StatisticsResponse
import com.soccertips.predcompose.repository.FixtureDetailsRepository
import com.soccertips.predcompose.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class FixtureDetailsViewModel
@Inject
constructor(
    private val repository: FixtureDetailsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<ResponseData>>(UiState.Idle)
    val uiState: StateFlow<UiState<ResponseData>> = _uiState.asStateFlow()

    private val _headToHeadState =
        MutableStateFlow<UiState<List<com.soccertips.predcompose.model.headtohead.FixtureDetails>>>(
            UiState.Loading,
        )
    val headToHeadState: StateFlow<UiState<List<com.soccertips.predcompose.model.headtohead.FixtureDetails>>> =
        _headToHeadState.asStateFlow()

    private val _lineupsState = MutableStateFlow<UiState<List<TeamLineup>>>(UiState.Loading)
    val lineupsState: StateFlow<UiState<List<TeamLineup>>> = _lineupsState.asStateFlow()

    private val _fixtureStatsState =
        MutableStateFlow<UiState<List<com.soccertips.predcompose.model.statistics.Response>>>(
            UiState.Loading,
        )
    val fixtureStatsState: StateFlow<UiState<List<com.soccertips.predcompose.model.statistics.Response>>> =
        _fixtureStatsState.asStateFlow()

    private val _fixtureEventsState = MutableStateFlow<UiState<List<FixtureEvent>>>(UiState.Loading)
    val fixtureEventsState: StateFlow<UiState<List<FixtureEvent>>> =
        _fixtureEventsState.asStateFlow()

    private val _standingsState = MutableStateFlow<UiState<List<TeamStanding>>>(UiState.Loading)
    val standingsState: StateFlow<UiState<List<TeamStanding>>> = _standingsState.asStateFlow()

    private val _predictionsState = MutableStateFlow<UiState<List<Response>>>(UiState.Idle)
    val predictionsState: StateFlow<UiState<List<Response>>> = _predictionsState.asStateFlow()

    private val _formState = MutableStateFlow<UiState<List<FixtureWithType>>>(UiState.Idle)
    val formState: StateFlow<UiState<List<FixtureWithType>>> = _formState.asStateFlow()

    // Cache to avoid refetching the same fixture details
    private var lastFetchedFixtureId: String? = null
    private var cachedFixtureDetails: ResponseData? = null
    private var lastFetchTimestamp: Long? = null

    private val cacheExpirationDuration =
        5 * 60 * 1000 // Cache expires in 5 minutes (in milliseconds)

    fun fetchFixtureDetails(fixtureId: String) {
        if (fixtureId == lastFetchedFixtureId && cachedFixtureDetails != null && !isCacheExpired()) {
            // Use cached data
            _uiState.value = UiState.Success(cachedFixtureDetails!!)
            return
        }

        // Fetch new data if cache is expired or not available
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.getFixtureDetails(fixtureId)
                Timber.d("Fixture Details Data fetched successfully: $response")
                cachedFixtureDetails = response
                lastFetchedFixtureId = fixtureId
                lastFetchTimestamp = System.currentTimeMillis()
                _uiState.value = UiState.Success(response)
            } catch (e: retrofit2.HttpException) {
                Timber.e(e, "Error fetching fixture details")
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")

            } catch (e: Exception) {
                Timber.e(e, "Error fetching fixture details")
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")

            }
        }
    }

    private fun isCacheExpired(): Boolean {
        val currentTime = System.currentTimeMillis()
        return lastFetchTimestamp == null || (currentTime - lastFetchTimestamp!!) > cacheExpirationDuration
    }


    fun fetchFormAndPredictions(
        season: String,
        homeTeamId: String,
        awayTeamId: String,
        fixtureId: String,
        last: String,
        leagueId: String
    ) {
        viewModelScope.launch {
            // Set loading state for all requests
            setLoadingState()

            try {
                // Fetch all data concurrently using async for better performance
                val homeFormResponseDeferred =
                    async { repository.getFixturesFormHome(season, homeTeamId, last) }
                val awayFormResponseDeferred =
                    async { repository.getFixturesFormAway(season, awayTeamId, last) }
                val predictionsResponseDeferred = async { repository.getPredictions(fixtureId) }
                val homeStatsResponseDeferred =
                    async { repository.getFixtureStats(fixtureId, homeTeamId) }
                val awayStatsResponseDeferred =
                    async { repository.getFixtureStats(fixtureId, awayTeamId) }
                val headToHeadResponseDeferred =
                    async { repository.getHeadToHeadFixtures("$homeTeamId-$awayTeamId", last) }
                val lineupsResponseDeferred = async { repository.getLineups(fixtureId) }
                val fixtureEventsResponseDeferred = async { repository.getFixtureEvents(fixtureId) }
                val standingsResponseDeferred = async { repository.getStandings(leagueId, season) }

                // Await responses
                val homeFormResponse = homeFormResponseDeferred.await()
                val awayFormResponse = awayFormResponseDeferred.await()
                val predictionsResponse = predictionsResponseDeferred.await()
                val homeStatsResponse = homeStatsResponseDeferred.await()
                val awayStatsResponse = awayStatsResponseDeferred.await()
                val headToHeadResponse = headToHeadResponseDeferred.await()
                val lineupsResponse = lineupsResponseDeferred.await()
                val fixtureEventsResponse = fixtureEventsResponseDeferred.await()
                val standingsResponse = standingsResponseDeferred.await()

                // Process each response and update UI state
                handleFormResponse(
                    homeFormResponse,
                    awayFormResponse
                ) // Handle both home and away form responses
                handlePredictionsResponse(predictionsResponse)
                handleStatsResponse(homeStatsResponse, awayStatsResponse)
                handleHeadToHeadResponse(headToHeadResponse)
                handleLineupsResponse(lineupsResponse)
                handleFixtureEventsResponse(fixtureEventsResponse)
                handleStandingsResponse(standingsResponse)

            } catch (e: Exception) {
                Timber.e(e, "Error fetching form and predictions")
                setErrorState(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }

    // Utility function to set loading state for all states
    private fun setLoadingState() {
        _formState.value = UiState.Loading
        _predictionsState.value = UiState.Loading
        _fixtureStatsState.value = UiState.Loading
        _headToHeadState.value = UiState.Loading
        _lineupsState.value = UiState.Loading
        _fixtureEventsState.value = UiState.Loading
        _standingsState.value = UiState.Loading
    }

    data class FixtureWithType(
        val fixture: FixtureDetails,
        val isHome: Boolean // True for home team, false for away team
    )


    // Utility function to handle responses
    private fun handleFormResponse(
        homeFormResponse: FixtureListResponse,
        awayFormResponse: FixtureListResponse
    ) {
        // Extract form data
        val homeFormData = homeFormResponse.response
        val awayFormData = awayFormResponse.response

        // Combine both home and away form data with a marker
        val combinedFormData = mutableListOf<FixtureWithType>()

        // Add home fixtures with the marker
        homeFormData.forEach { fixture ->
            combinedFormData.add(FixtureWithType(fixture, isHome = true))
        }

        // Add away fixtures with the marker
        awayFormData.forEach { fixture ->
            combinedFormData.add(FixtureWithType(fixture, isHome = false))
        }

        // Update the UI state based on the combined data
        if (combinedFormData.isNotEmpty()) {
            _formState.value = UiState.Success(combinedFormData)
        } else {
            _formState.value = UiState.Error("😞 No data available.")
        }
    }


    private fun handlePredictionsResponse(predictionsResponse: PredictionResponse) {
        val predictionsData = predictionsResponse.response
        if (predictionsData.isNotEmpty()) {
            _predictionsState.value = UiState.Success(predictionsData)
        } else {
            _predictionsState.value = UiState.Error("😞 No predictions available.")
        }
    }

    private fun handleStatsResponse(
        homeStatsResponse: StatisticsResponse,
        awayStatsResponse: StatisticsResponse
    ) {
        val combinedStats = homeStatsResponse.response + awayStatsResponse.response
        if (combinedStats.isNotEmpty()) {
            _fixtureStatsState.value = UiState.Success(combinedStats)
        } else {
            _fixtureStatsState.value = UiState.Error("😞 No statistics available.")

        }

    }

    private fun handleHeadToHeadResponse(headToHeadResponse: HeadToHeadResponse) {
        val headToHeadData = headToHeadResponse.response
        if (headToHeadData.isNotEmpty()) {
            _headToHeadState.value = UiState.Success(headToHeadData)
        } else {
            _headToHeadState.value = UiState.Error("😞 No head-to-head data found.")
        }
    }

    private fun handleLineupsResponse(lineupsResponse: FixtureLineupResponse) {
        val lineupsData = lineupsResponse.response
        if (lineupsData.isNotEmpty()) {
            _lineupsState.value = UiState.Success(lineupsData)
        } else {
            _lineupsState.value = UiState.Error("😞 No lineups available.")
        }
    }

    private fun handleFixtureEventsResponse(fixtureEventsResponse: FixtureEventsResponse) {
        val fixtureEventsData = fixtureEventsResponse.response
        if (fixtureEventsData.isNotEmpty()) {
            _fixtureEventsState.value = UiState.Success(fixtureEventsData)
        } else {
            _fixtureEventsState.value = UiState.Error("😞 No fixture events available.")
        }
    }

    private fun handleStandingsResponse(standingsResponse: StandingsResponse) {
        val standingsData = standingsResponse.response
        val teamStandings = standingsData.flatMap { it.league.standings.flatten() }
        if (teamStandings.isNotEmpty()) {
            _standingsState.value = UiState.Success(teamStandings)
        } else {
            _standingsState.value = UiState.Error("😞 No standings data available.")
        }
    }

    // Centralized error handler
    private fun setErrorState(errorMessage: String) {
        _formState.value = UiState.Error(errorMessage)
        _predictionsState.value = UiState.Error(errorMessage)
        _fixtureStatsState.value = UiState.Error(errorMessage)
        _headToHeadState.value = UiState.Error(errorMessage)
        _lineupsState.value = UiState.Error(errorMessage)
        _fixtureEventsState.value = UiState.Error(errorMessage)
        _standingsState.value = UiState.Error(errorMessage)
    }


    fun getHomeGoalScorers(
        events: List<Event>,
        homeTeamId: Int
    ): List<Pair<String, String>> =
        events
            .filter { it.type == "Goal" && it.team.id == homeTeamId }
            .mapNotNull {
                val playerName = it.player.name
                if (playerName.isNullOrBlank()) {
                    null // Skip if playerName is null or blank
                } else {
                    val lastName = playerName.split(" ").lastOrNull() ?: "" // Safely split
                    val elapsed = "${it.time.elapsed}${it.time.extra?.let { "+$it" } ?: ""}’"
                    lastName to elapsed
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
                if (playerName.isNullOrBlank()) {
                    null
                } else {
                    val lastName = playerName.split(" ").lastOrNull() ?: ""
                    val elapsed = "${it.time.elapsed}${it.time.extra?.let { "+$it" } ?: ""}’"
                    lastName to elapsed
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
