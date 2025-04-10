package com.soccertips.predictx.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppUpdateAndReviewScreen(
    showSnackbar: Boolean,
    onDismissSnackbar: () -> Unit,
    onCompleteUpdate: () -> Unit,
    onRequestReview: () -> Unit
) {
    LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = onRequestReview) {
                Text("Request In-App Review")
            }
        }
    }

    if (showSnackbar) {
        Snackbar(
            action = {
                Button(onClick = {
                    onCompleteUpdate()
                    onDismissSnackbar()
                }) {
                    Text("RESTART")
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Update downloaded. Restart to install.")
        }
    }
}

@Preview
@Composable
private fun AppUpdateAndReviewScreenPreview() {
    AppUpdateAndReviewScreen(
        showSnackbar = true,
        onDismissSnackbar = {},
        onCompleteUpdate = {},
        onRequestReview = {}
    )

}