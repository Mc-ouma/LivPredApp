package com.soccertips.predcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predcompose.data.model.Event
import com.soccertips.predcompose.data.model.ResponseData
import com.soccertips.predcompose.data.model.events.FixtureEvent
import com.soccertips.predcompose.data.model.events.FixtureEventsResponse
import com.soccertips.predcompose.data.model.headtohead.HeadToHeadResponse
import com.soccertips.predcompose.data.model.lastfixtures.FixtureDetails
import com.soccertips.predcompose.data.model.lineups.FixtureLineupResponse
import com.soccertips.predcompose.data.model.lineups.TeamLineup
import com.soccertips.predcompose.data.model.prediction.PredictionResponse
import com.soccertips.predcompose.data.model.prediction.Response
import com.soccertips.predcompose.data.model.statistics.StatisticsResponse
import com.soccertips.predcompose.repository.FixtureDetailsRepository
import com.soccertips.predcompose.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val _uiState = MutableStateFlow<UiState<ResponseData>>(UiState.Loading)
    val uiState: StateFlow<UiState<ResponseData>> = _uiState.asStateFlow()

    private val _headToHeadState =
        MutableStateFlow<UiState<List<com.soccertips.predcompose.data.model.headtohead.FixtureDetails>>>(
            UiState.Loading,
        )
    val headToHeadState: StateFlow<UiState<List<com.soccertips.predcompose.data.model.headtohead.FixtureDetails>>> =
        _headToHeadState.asStateFlow()

    private val _lineupsState = MutableStateFlow<UiState<List<TeamLineup>>>(UiState.Loading)
    val lineupsState: StateFlow<UiState<List<TeamLineup>>> = _lineupsState.asStateFlow()

    private val _fixtureStatsState =
        MutableStateFlow<UiState<List<com.soccertips.predcompose.data.model.statistics.Response>>>(
            UiState.Loading,
        )
    val fixtureStatsState: StateFlow<UiState<List<com.soccertips.predcompose.data.model.statistics.Response>>> =
        _fixtureStatsState.asStateFlow()

    private val _fixtureEventsState = MutableStateFlow<UiState<List<FixtureEvent>>>(UiState.Loading)
    val fixtureEventsState: StateFlow<UiState<List<FixtureEvent>>> =
        _fixtureEventsState.asStateFlow()

    /* private val _standingsState = MutableStateFlow<UiState<List<TeamStanding>>>(UiState.Loading)
     val standingsState: StateFlow<UiState<List<TeamStanding>>> = _standingsState.asStateFlow()*/

    private val _predictionsState = MutableStateFlow<UiState<List<Response>>>(UiState.Loading)
    val predictionsState: StateFlow<UiState<List<Response>>> = _predictionsState.asStateFlow()


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
                cachedFixtureDetails = response
                lastFetchedFixtureId = fixtureId
                lastFetchTimestamp = System.currentTimeMillis()
                _uiState.value = UiState.Success(response)
            } catch (e: retrofit2.HttpException) {
                _uiState.value =
                    UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")

            } catch (e: Exception) {
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
                val predictionsResponse = predictionsResponseDeferred.await()
                val homeStatsResponse = homeStatsResponseDeferred.await()
                val awayStatsResponse = awayStatsResponseDeferred.await()
                val headToHeadResponse = headToHeadResponseDeferred.await()
                val lineupsResponse = lineupsResponseDeferred.await()
                val fixtureEventsResponse = fixtureEventsResponseDeferred.await()
                standingsResponseDeferred.await()

                handlePredictionsResponse(predictionsResponse)
                handleStatsResponse(homeStatsResponse, awayStatsResponse)
                handleHeadToHeadResponse(headToHeadResponse)
                handleLineupsResponse(lineupsResponse)
                handleFixtureEventsResponse(fixtureEventsResponse)
                // handleStandingsResponse(standingsResponse)

            } catch (e: Exception) {
                setErrorState(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }

    // Utility function to set loading state for all states
    private fun setLoadingState() {
        _predictionsState.value = UiState.Loading
        _fixtureStatsState.value = UiState.Loading
        _headToHeadState.value = UiState.Loading
        _lineupsState.value = UiState.Loading
        _fixtureEventsState.value = UiState.Loading
    }

    data class FixtureWithType(
        val fixture: FixtureDetails,
        val isHome: Boolean // True for home team, false for away team
    )



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


    // Centralized error handler
    private fun setErrorState(errorMessage: String) {
        _predictionsState.value = UiState.Error(errorMessage)
        _fixtureStatsState.value = UiState.Error(errorMessage)
        _headToHeadState.value = UiState.Error(errorMessage)
        _lineupsState.value = UiState.Error(errorMessage)
        _fixtureEventsState.value = UiState.Error(errorMessage)
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
                if (playerName.isBlank()) {
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
