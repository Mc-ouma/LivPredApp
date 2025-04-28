package com.soccertips.predictx.ui

import com.soccertips.predictx.data.model.ResponseData
import com.soccertips.predictx.data.model.events.FixtureEvent
import com.soccertips.predictx.data.model.lineups.TeamLineup

sealed class UiState<out T> {

    object Loading : UiState<Nothing>()

    data class Success<T>(
        val data: T,
    ) : UiState<T>()

    data class Error(
        val message: String,
    ) : UiState<Nothing>()

    object Empty : UiState<Nothing>()
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String,
        val onActionPerformed: (() -> Unit)?
    ) : UiState<Nothing>()
}

sealed class FixtureDetailsUiState {
    object Loading : FixtureDetailsUiState()
    data class Success(
        val fixtureDetails: ResponseData,
        val predictions: List<com.soccertips.predictx.data.model.prediction.Response>?,
        val fixtureStats: List<com.soccertips.predictx.data.model.statistics.Response>?,
        val headToHead: List<com.soccertips.predictx.data.model.headtohead.FixtureDetails>?,
        val lineups: List<TeamLineup>?,
        val fixtureEvents: List<FixtureEvent>?
    ) : FixtureDetailsUiState()

    data class Error(val message: String) : FixtureDetailsUiState()
}
