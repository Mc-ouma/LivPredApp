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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soccertips.predictx.R

@Composable
fun AppUpdateAndReviewScreen(
        showSnackbar: Boolean,
        onDismissSnackbar: () -> Unit,
        onCompleteUpdate: () -> Unit,
        onRequestReview: () -> Unit
) {
    LocalContext.current

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = onRequestReview) {
                Text(stringResource(R.string.request_in_app_review))
            }
        }
    }

    if (showSnackbar) {
        Snackbar(
                action = {
                    Button(
                            onClick = {
                                onCompleteUpdate()
                                onDismissSnackbar()
                            }
                    ) { Text(stringResource(R.string.restart)) }
                },
                modifier = Modifier.padding(16.dp)
        ) { Text(stringResource(R.string.update_downloaded_restart)) }
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
