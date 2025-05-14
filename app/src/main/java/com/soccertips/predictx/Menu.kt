package com.soccertips.predictx

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@Composable
fun Menu() {
    var expanded by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var showAboutUs by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showShare by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Localized description",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(expanded = expanded, shape = MaterialTheme.shapes.small, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    showShare = true
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Rate Us") },
                onClick = { },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.RateReview,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Privacy Policy") },
                onClick = { showPrivacyPolicy = true },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Policy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Send Feedback") },
                onClick = { showFeedback = true },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
            )
            DropdownMenuItem(
                text = { Text("About Us") },
                onClick = { showAboutUs = true },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
            )
        }
    }

    if (showPrivacyPolicy) {
        PrivacyPolicy(onDismiss = { showPrivacyPolicy = false })
    }

    if (showFeedback) {
        Feedback(onDismiss = { showFeedback = false })
    }

    if (showAboutUs) {
        AboutUs(onDismiss = { showAboutUs = false })
    }
    if (showShare) {
        Share(text = "Check out AI ScoreCast, the best football prediction app. ", context = context)
    }
}


@Composable
fun AboutUs(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Us") },
        icon = {
            Icon(
                Icons.Outlined.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Text(
                "AI ScoreCast is a football prediction app that provides daily betting tips and predictions for football matches. " +
                        "Our team of experts analyze football matches to provide you with the best betting tips and predictions. " +
                        "We provide tips for various markets including Over/Under, BTTS, Daily 2 Odds, Combo, HT/FT, Home/Away, Daily Bonus, and Extra Picks."
            )
        },
        confirmButton = {
            Text("OK", modifier = Modifier.clickable { onDismiss() })
        }
    )
}

@Composable
fun PrivacyPolicy(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy Policy") },
        text = {
            Text(
                "AI ScoreCast is a football prediction app that provides daily betting tips and predictions for football matches. " +
                        "Our team of experts analyze football matches to provide you with the best betting tips and predictions. " +
                        "We provide tips for various markets including Over/Under, BTTS, Daily 2 Odds, Combo, HT/FT, Home/Away, Daily Bonus, and Extra Picks."
            )
        },
        confirmButton = {
            Text("OK", modifier = Modifier.clickable { onDismiss() }
            )
        }
    )
}

@Composable
fun Share(text: String, context: Context) {

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(
            Intent.EXTRA_TEXT,
            text + "https://play.google.com/store/apps/details?id=com.soccertips.predictx"
        )
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

data class ExpandableItem(val title: String, val content: String, var isExpanded: Boolean = false)

@Composable
fun ExpandableList(items: List<ExpandableItem>, onOtherSelected: () -> Unit) {
    var expandedIndex by remember { mutableIntStateOf(-1) }
    LazyColumn {
        items(items.size) { index ->
            val item = items[index]
            var isExpanded = expandedIndex == index

            Column(modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                .fillMaxWidth()
                .clickable {
                    expandedIndex = if (isExpanded) -1 else index
                    if (item.title == "Other" && isExpanded) {
                        onOtherSelected()
                    }
                }) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
                if (isExpanded && item.title != "Other") {
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Feedback(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val items = listOf(
        ExpandableItem(
            "App not responding?",
            "Kindly ensure you the latest version of the app. Check for the latest version on the Play Store."
        ),
        ExpandableItem(
            "Games not Loading?",
            "Make sure you have an active internet connection. If the problem persists, please contact us."
        ),
        ExpandableItem(
            "Too many ads?",
            "We apologize for the inconvenience. We are working on reducing the number of ads in the app."
        ),
        ExpandableItem("Other", "Please describe the issue you are facing in detail.")
    )
    var showOtherDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = { onDismiss() }) {
        ExpandableList(items = items, onOtherSelected = { showOtherDialog = true })
    }
    if (showOtherDialog) {
        OtherFeedbackDialog(
            onDismiss = { showOtherDialog = false },
            onSubmit = { email, message ->


                val appVersion =
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                val intent = Intent(Intent.ACTION_SEND).apply {
                    data = "mailto:ouma.monicasales@gmail.com".toUri()
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                    putExtra(Intent.EXTRA_SUBJECT, "PredictX Feedback")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "$message\n\nSent from PredictX App\nVersion: $appVersion"
                    )
                }

                context.startActivity(Intent.createChooser(intent, "Send Email"))

            }
        )
    }
}

@Composable
fun OtherFeedbackDialog(onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isEmailValid by remember { mutableStateOf(true) }

    val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}".toRegex()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Other Feedback") },
        text = {
            Column {
                TextField(
                    value = email,
                    onValueChange = {
                        email = it
                        isEmailValid = emailPattern.matches(email)
                    },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    placeholder = {
                        if (isEmailValid) {

                            Text("Enter your email address")
                        }
                    },
                    isError = !isEmailValid
                )
                if (!isEmailValid) {
                    Text(
                        text = "Please enter a valid email address",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                TextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Outlined.Message,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        },
        confirmButton = {
            Text("Submit", modifier = Modifier.clickable {
                if (isEmailValid) {
                    onSubmit(email, message)
                    onDismiss()
                }
            })
        }
    )
}

@Preview
@Composable
private fun MenuPreview() {
    Menu()
}
