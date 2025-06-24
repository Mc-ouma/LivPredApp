package com.soccertips.predictx.ui.privacy

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.soccertips.predictx.consent.ConsentState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    navController: NavController,
    activity: Activity,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val consentState by viewModel.consentState.collectAsState()
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You can adjust your privacy settings here. Changes will take effect immediately.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                text = "Ad Preferences",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitchItem(
                title = "Personalized Ads",
                subtitle = "Allow ads based on your interests and app usage",
                checked = (consentState as? ConsentState.Detailed)?.personalizedAdsConsent ?: false,
                onCheckedChange = { checked ->
                    viewModel.updatePersonalizedAdsConsent(checked)
                }
            )

            SettingsSwitchItem(
                title = "Show Non-Personalized Ads",
                subtitle = "Display generic ads not tailored to your interests",
                checked = (consentState as? ConsentState.Detailed)?.nonPersonalizedAdsConsent ?: true,
                onCheckedChange = { checked ->
                    viewModel.updateNonPersonalizedAdsConsent(checked)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Data Collection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitchItem(
                title = "Analytics Collection",
                subtitle = "Allow collection of anonymous usage data to improve the app",
                checked = (consentState as? ConsentState.Detailed)?.analyticsConsent ?: false,
                onCheckedChange = { checked ->
                    viewModel.updateAnalyticsConsent(checked)
                }
            )

            SettingsSwitchItem(
                title = "Crash Reporting",
                subtitle = "Send crash reports to help fix issues",
                checked = (consentState as? ConsentState.Detailed)?.crashlyticsConsent ?: false,
                onCheckedChange = { checked ->
                    viewModel.updateCrashlyticsConsent(checked)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.showConsentForm(activity) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Consent Form")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset All Privacy Settings")
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset Privacy Settings") },
                text = { Text("This will reset all your privacy preferences and show the consent form again. Continue?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetConsent(activity)
                        showResetDialog = false
                    }) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
