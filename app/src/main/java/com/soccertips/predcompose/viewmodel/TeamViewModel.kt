package com.soccertips.predcompose.viewmodel

import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predcompose.model.lastfixtures.FixtureDetails
import com.soccertips.predcompose.model.team.squad.Response
import com.soccertips.predcompose.model.team.teamscreen.TeamStatistics
import com.soccertips.predcompose.repository.TeamsRepository
import com.soccertips.predcompose.ui.UiState
import com.soccertips.predcompose.viewmodel.TeamViewModel.FormattedDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.Locale
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

    private val _transfers =
        MutableStateFlow<UiState<List<com.soccertips.predcompose.model.team.transfer.Response2>>>(
            UiState.Loading
        )
    val transfers: StateFlow<UiState<List<com.soccertips.predcompose.model.team.transfer.Response2>>> =
        _transfers.asStateFlow()

    private val _fixtures = MutableStateFlow<UiState<List<FixtureDetails>>>(UiState.Loading)
    val fixtures: StateFlow<UiState<List<FixtureDetails>>> = _fixtures.asStateFlow()

    private val _teamData =
        MutableStateFlow<UiState<List<com.soccertips.predcompose.model.team.teamscreen.Response>>>(
            UiState.Loading
        )
    val teamData: StateFlow<UiState<List<com.soccertips.predcompose.model.team.teamscreen.Response>>> =
        _teamData.asStateFlow()
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

    fun getTeamData(teamId: Int) {
        fetchData(_teamData, "teamData_$teamId", { repository.getTeamData(teamId).response }) {
            isTeamDataLoaded = true
        }
    }

    fun getTeams(leagueId: String, season: String, teamId: String) {
        fetchData(
            _team,
            "team_$teamId",
            { repository.getTeams(leagueId, season, teamId).response }) {
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
            fetchData(
                _fixtures,
                "fixtures_$teamId",
                { repository.getNextFixtures(teamId, season, next).response }) {
                isFixturesDataLoaded = true
            }
        }
    }

    data class FormattedDateTime(
        val date: String,
        val time: String
    )

    fun formatDateTime(dateString: String, timezone: String): FormattedDateTime {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone(timezone)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault() // Convert to the user's local timezone
        timeFormat.timeZone = TimeZone.getDefault() // Convert to the user's local timezone

        return try {
            val date = inputFormat.parse(dateString)
            val calendar = Calendar.getInstance()
            val today = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrow = calendar.time

            val formattedDate = when {
                isSameDay(date, today) -> "Today"
                isSameDay(date, tomorrow) -> "Tomorrow"
                else -> dateFormat.format(date!!)
            }
            FormattedDateTime(
                date = formattedDate,
                time = timeFormat.format(date)
            )
        } catch (e: Exception) {
            FormattedDateTime(date = "Invalid Date", time = "Invalid Time")
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}