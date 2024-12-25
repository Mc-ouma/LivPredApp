package com.soccertips.predcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predcompose.model.lastfixtures.FixtureDetails
import com.soccertips.predcompose.model.team.squad.Response
import com.soccertips.predcompose.model.team.teamscreen.TeamStatistics
import com.soccertips.predcompose.repository.TeamsRepository
import com.soccertips.predcompose.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TeamViewModel
@Inject
constructor(
    private val repository: TeamsRepository,
) : ViewModel() {
    private val _team = MutableStateFlow<UiState<TeamStatistics>>(UiState.Loading)
    val team: StateFlow<UiState<TeamStatistics>> = _team.asStateFlow()

    private val _players = MutableStateFlow<UiState<List<Response>>>(UiState.Loading)
    val players: StateFlow<UiState<List<Response>>> = _players.asStateFlow()

    private val _transfers = MutableStateFlow<UiState<List<com.soccertips.predcompose.model.team.transfer.Response2>>>(UiState.Loading)
    val transfers: StateFlow<UiState<List<com.soccertips.predcompose.model.team.transfer.Response2>>> = _transfers.asStateFlow()

    private val _fixtures = MutableStateFlow<UiState<List<FixtureDetails>>>(UiState.Loading)
    val fixtures: StateFlow<UiState<List<FixtureDetails>>> = _fixtures.asStateFlow()

    private val cache = mutableMapOf<String, Any>()

    var isTeamDataLoaded = false
    var isPlayersDataLoaded = false
    var isTransfersDataLoaded = false
    var isFixturesDataLoaded = false

    var isLoading = false
        private set

   private inline fun <reified T> fetchData(
    stateFlow: MutableStateFlow<UiState<T>>,
    cacheKey: String,
    crossinline apiCall: suspend () -> T,
    noinline onError: (Exception) -> Unit = {},
    crossinline onSuccess: () -> Unit,
) {
    viewModelScope.launch {
        val cachedData = cache[cacheKey] as? T
        if (cachedData != null) {
            Timber.tag("ViewModelFetch").d("using cached data for $cacheKey")
            stateFlow.value = UiState.Success(cachedData)
            onSuccess()
            return@launch
        }

        stateFlow.value = UiState.Loading
        isLoading = true
        try {
            Timber.tag("ViewModelFetch").d("fetching fresh data for $cacheKey")
            val response = apiCall()
            cache[cacheKey] = response as Any
            stateFlow.value = UiState.Success(response)
            onSuccess()
        } catch (e: Exception) {
            stateFlow.value = UiState.Error(e.message ?: "An error occurred")
            onError(e)
        } finally {
            isLoading = false
        }
    }
}

    fun getTeams(leagueId: String, season: String, teamId: String) {
        fetchData(_team, "team_$teamId", { repository.getTeams(leagueId, season, teamId).response }) {
            isTeamDataLoaded = true
        }
    }

    fun getPlayers(teamId: String) {
        if (!isPlayersDataLoaded) {
            fetchData(_players, "players_$teamId", { repository.getPlayers(teamId).response }) {
                isPlayersDataLoaded = true
            }
        }
    }

    fun getTransfers(teamId: String, page: Int = 1) {
        fetchData(
            _transfers,
            "transfers_$teamId",
            {
                repository.getTransfers(teamId, page).response.sortedByDescending { it.update }
            }) {
            isTransfersDataLoaded = true
        }
    }

    fun getNextFixtures(teamId: String, season: String, next: String) {
        if (!isFixturesDataLoaded) {
            fetchData(_fixtures, "fixtures_$teamId", { repository.getNextFixtures(teamId, season, next).response }) {
                isFixturesDataLoaded = true
            }
        }
    }
}