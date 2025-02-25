package com.soccertips.predcompose.ui.fixturedetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soccertips.predcompose.ui.FixtureDetailsUiState
import com.soccertips.predcompose.ui.components.LoadingIndicator

@Composable
fun LoadingScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Companion.Center,
    ) {
        LoadingIndicator()
    }
}

@Composable
fun ErrorScreen(paddingValues: PaddingValues, message: String) {
    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Companion.Center,
    ) {
        Text(
            text = message,
            color = Color.Companion.Red,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun EmptyScreen(paddingValues: PaddingValues, message: String = "No data available") {
    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Companion.Center,
    ) {
        Text(
            text = message,
            color = Color.Companion.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun EmptyStateMessages(
    uiState: FixtureDetailsUiState
) {
    if (uiState is FixtureDetailsUiState.Success) {
        val successState = uiState
        Column(modifier = Modifier.Companion.padding(16.dp)) {
            if (successState.fixtureStats.isNullOrEmpty()) {
                Text(
                    "No fixture stats available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Companion.Gray
                )
            }
            if (successState.fixtureEvents.isNullOrEmpty()) {
                Text(
                    "No fixture events available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Companion.Gray
                )
            }
            if (successState.predictions.isNullOrEmpty()) {
                Text(
                    "No fixture predictions available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Companion.Gray
                )
            }
        }
    }
}