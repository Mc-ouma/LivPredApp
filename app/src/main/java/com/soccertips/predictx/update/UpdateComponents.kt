package com.soccertips.predictx.update

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun UpdateHandler(updateManager: CustomAppUpdateManager) {
    val updateState by updateManager.updateState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Show update progress indicator when downloading
    UpdateProgressIndicator(
        isDownloading = updateState.isDownloading,
        modifier = Modifier.padding(16.dp)
    )

    // Handle update downloaded state
    LaunchedEffect(updateState.isDownloaded) {
        if (updateState.isDownloaded) {
            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = "Update downloaded. Restart to complete installation.",
                        actionLabel = "RESTART",
                        duration = SnackbarDuration.Indefinite
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    updateManager.completeUpdate()
                }
            }
        }
    }

    // Show mandatory update dialog
    if (updateState.isAvailable && updateState.isMandatory) {
        MandatoryUpdateDialog(
            onUpdateClick = {
                val activity = context as? ComponentActivity
                activity?.let { updateManager.startUpdateFlow(it) }
            }
        )
    }

    // Show optional update snackbar
    LaunchedEffect(updateState.isAvailable) {
        if (updateState.isAvailable && !updateState.isMandatory) {
            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = "App update available",
                        actionLabel = "UPDATE",
                        duration = SnackbarDuration.Long
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    val activity = context as? ComponentActivity
                    activity?.let { updateManager.startUpdateFlow(it) }
                }
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}

@Composable
private fun MandatoryUpdateDialog(onUpdateClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss mandatory update */ },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(text = "Update Required", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                text =
                    "A critical update is available. Please update the app to continue using it.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onUpdateClick, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Now")
            }
        },
        dismissButton = null // No dismiss button for mandatory updates
    )
}

@Composable
fun UpdateProgressIndicator(isDownloading: Boolean, modifier: Modifier = Modifier) {
    if (isDownloading) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Text(
                    text = "Downloading update...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
