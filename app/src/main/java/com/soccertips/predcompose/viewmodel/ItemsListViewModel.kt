package com.soccertips.predcompose.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predcompose.model.ServerResponse
import com.soccertips.predcompose.repository.PredictionRepository
import com.soccertips.predcompose.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ItemsListViewModel @Inject constructor(
    private val repository: PredictionRepository,
) : ViewModel() {

    // Cache for fetched data with timestamps
    private val cachedData = mutableMapOf<String, Pair<Long, List<ServerResponse>>>()
    private val cacheExpirationDuration = 12 * 60 * 60 * 1000 // Cache expires in 12 hours (in milliseconds)

    private val _uiState = MutableStateFlow<UiState<List<ServerResponse>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ServerResponse>>> = _uiState.asStateFlow()

    // Fetch data only if not already cached for the given date
    fun fetchItems(categoryEndpoint: String, date: LocalDate?) {
        // Check if data for the selected date is already cached
        val cacheKey = "${categoryEndpoint}_$date"
        val cachedItems = cachedData[cacheKey]

        if (cachedItems != null) {
            val (timestamp, items) = cachedItems
            val currentTime = System.currentTimeMillis()
            if (currentTime - timestamp < cacheExpirationDuration) {
                // If data is cached and not expired, use the cached data
                _uiState.value = UiState.Success(items)
                return
            } else {
                // Remove expired cache
                cachedData.remove(cacheKey)
            }
        }

        // If no cached data or cache is expired, fetch from the repository
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.getCategoryData(categoryEndpoint)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val items = response.serverResponse
                    .filter { serverResponse ->
                        val mDate = serverResponse.mDate ?: ""
                        mDate != "0000-00-00" && LocalDate.parse(mDate, formatter) == date
                    }
                    .map { serverResponse ->
                        val color = when (serverResponse.outcome) {
                            "win" -> Color.Green
                            "lose" -> Color.Red
                            else -> Color.Unspecified
                        }
                        ServerResponse(
                            fixtureId = serverResponse.fixtureId ?: "",
                            pick = serverResponse.pick ?: "Unknown",
                            homeTeam = serverResponse.homeTeam ?: "Unknown",
                            awayTeam = serverResponse.awayTeam ?: "Unknown",
                            mDate = serverResponse.mDate ?: "Unknown",
                            league = serverResponse.league ?: "Unknown",
                            mTime = serverResponse.mTime ?: "Unknown",
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

                // Cache the fetched data with the current timestamp
                cachedData[cacheKey] = System.currentTimeMillis() to items

                _uiState.value = UiState.Success(items)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "An unexpected error occurred.")
            }
        }
    }
}
