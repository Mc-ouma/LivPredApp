package com.soccertips.predcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predcompose.model.lastfixtures.FixtureDetails
import com.soccertips.predcompose.model.standings.TeamStanding
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

    // StateFlows for fixtures and standings
    private val _fixturesState = MutableStateFlow<UiState<List<FixtureWithType>>>(UiState.Loading)
    val fixturesState: StateFlow<UiState<List<FixtureWithType>>> = _fixturesState.asStateFlow()

    private val _standingsState = MutableStateFlow<UiState<List<TeamStanding>>>(UiState.Loading)
    val standingsState: StateFlow<UiState<List<TeamStanding>>> = _standingsState.asStateFlow()

    var isFixturesDataLoaded = false
    var isStandingsDataLoaded = false

    // Fetch fixtures
   fun fetchFixtures(season: String, homeTeamId: String, awayTeamId: String, last: String) {
    viewModelScope.launch {
        _fixturesState.value = UiState.Loading
        Timber.d("Fetching fixtures for season: $season, homeTeamId: $homeTeamId, awayTeamId: $awayTeamId, last: $last")
        try {
            val homeResponse = repository.getLastFixtures(season, homeTeamId, last)
            val awayResponse = repository.getLastFixtures(season, awayTeamId, last)

            Timber.d("Home response: $homeResponse")
            Timber.d("Away response: $awayResponse")

            val homeFixtures = homeResponse.response.map { FixtureWithType(it, isHome = true) }
            val awayFixtures = awayResponse.response.map { FixtureWithType(it, isHome = false) }

            val combinedFixtures = homeFixtures + awayFixtures
            _fixturesState.value = UiState.Success(combinedFixtures)
            isFixturesDataLoaded = true
            Timber.d("Combined fixtures: $combinedFixtures")

        } catch (e: Exception) {
            _fixturesState.value = UiState.Error(e.message ?: "An error occurred while fetching fixtures.")
            Timber.e(e, "Error fetching fixtures")
        }
    }
}

    data class FixtureWithType(
        val fixture: FixtureDetails,
        val isHome: Boolean // True for home team, false for away team
    )

    // Fetch standings
    fun fetchStandings(leagueId: String, season: String) {
        viewModelScope.launch {
            _standingsState.value = UiState.Loading
            try {
                val response = repository.getStandings(leagueId, season)
                val teamStandings = response.response.flatMap { it.league.standings.flatten() }
                _standingsState.value = UiState.Success(teamStandings)
                isStandingsDataLoaded = true
                Timber.tag("fetch standings").d("%s", teamStandings)
            } catch (e: Exception) {
                _standingsState.value =
                    UiState.Error(e.message ?: "An error occurred while fetching standings.")
            }
        }
    }
}
