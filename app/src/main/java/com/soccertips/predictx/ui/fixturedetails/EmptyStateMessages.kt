package com.soccertips.predictx.ui.fixturedetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soccertips.predictx.ui.FixtureDetailsUiState
import com.soccertips.predictx.ui.components.LoadingIndicator

@Composable
fun LoadingScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator()
    }
}

@Composable
fun ErrorScreen(paddingValues: PaddingValues, message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "😕",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }

        }
    }
}

@Composable
fun EmptyScreen(paddingValues: PaddingValues, message: String = "No data available") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = Color.Gray,
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
        Column(modifier = Modifier.padding(16.dp)) {
            if (successState.fixtureStats.isNullOrEmpty()) {
                Text(
                    "No fixture stats available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            if (successState.fixtureEvents.isNullOrEmpty()) {
                Text(
                    "No fixture events available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            if (successState.predictions.isNullOrEmpty()) {
                Text(
                    "No fixture predictions available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview(uiMode = 1)
@Composable
private fun ErrorScreenPreview() {
    ErrorScreen(
        paddingValues = PaddingValues(16.dp),
        message = "An error occurred while fetching data.",
        onRetry = {}
    )
    
}
@Preview(uiMode = 1)
@Composable
private fun EmptyScreenPreview() {
    EmptyScreen(
        paddingValues = PaddingValues(16.dp),
        message = "No data available"
    )
}