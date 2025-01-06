package com.soccertips.predcompose.ui

import com.soccertips.predcompose.data.model.ResponseData

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
    ) : FixtureDetailsUiState()

    data class Error(
        val message: String,
    ) : FixtureDetailsUiState()
}
