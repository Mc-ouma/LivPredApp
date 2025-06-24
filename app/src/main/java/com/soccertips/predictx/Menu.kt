package com.soccertips.predictx

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.soccertips.predictx.navigation.Routes

@Composable
fun Menu(navController: androidx.navigation.NavController? = null) {
    var expanded by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var showAboutUs by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showShare by remember { mutableStateOf(false) }
    var showRateus by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Localized description",
                    tint = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
                expanded = expanded,
                shape = MaterialTheme.shapes.small,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.share)) },
                    onClick = {
                        showShare = true
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                                Icons.Outlined.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
            )

            // Privacy Settings Option
            DropdownMenuItem(
                    text = { Text("Privacy Settings") },
                    onClick = {
                        expanded = false
                        navController?.navigate(Routes.PrivacySettings.route)
                    },
                    leadingIcon = {
                        Icon(
                                Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
            )

            DropdownMenuItem(
                    text = { Text(stringResource(R.string.rate_us)) },
                    onClick = {
                        showRateus = true
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                                Icons.Outlined.RateReview,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
            )
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.privacy_policy)) },
                    onClick = {
                        openPrivacyPolicy(context)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                                Icons.Outlined.Policy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
            )
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.telegram)) },
                    onClick = {
                        val intent =
                                Intent(Intent.ACTION_VIEW).apply {
                                    data = "https://t.me/+SlbFLBrgmVJiMQiG".toUri()
                                }
                        context.startActivity(intent)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                                Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
            )
            HorizontalDivider()
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.send_feedback)) },
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
                    text = { Text(stringResource(R.string.about_us)) },
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

    if (showFeedback) {
        Feedback(onDismiss = { showFeedback = false })
    }
    if (showRateus) {
        RateUs(onDismiss = { showRateus = false })
    }

    if (showAboutUs) {
        AboutUs(onDismiss = { showAboutUs = false })
    }

    if (showShare) {
        Share(
                text = "Check out AI ScoreCast, the best football prediction app. ",
                context = context
        )
    }
}

@Composable
fun Menu2(navController: androidx.navigation.NavController? = null) {
    var expanded by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var showAboutUs by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showShare by remember { mutableStateOf(false) }
    var showRateus by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Localized description",
                    tint = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
                expanded = expanded,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.rate_us)) },
                    onClick = {
                        showRateus = true
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                                Icons.Outlined.RateReview,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
            )
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.privacy_policy)) },
                    onClick = {
                        openPrivacyPolicy(context)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                                Icons.Outlined.Policy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
            )
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.telegram)) },
                    onClick = {
                        val intent =
                                Intent(Intent.ACTION_VIEW).apply {
                                    data = "https://t.me/+SlbFLBrgmVJiMQiG".toUri()
                                }
                        context.startActivity(intent)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                                Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
            )
            HorizontalDivider()
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.send_feedback)) },
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
                    text = { Text(stringResource(R.string.about_us)) },
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

    if (showFeedback) {
        Feedback(onDismiss = { showFeedback = false })
    }
    if (showRateus) {
        RateUs(onDismiss = { showRateus = false })
    }

    if (showAboutUs) {
        AboutUs(onDismiss = { showAboutUs = false })
    }
    if (showShare) {
        Share(
                text = "Check out AI ScoreCast, the best football prediction app. ",
                context = context
        )
    }
}

@Composable
fun RateUs(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.rate_us)) },
            text = {
                Text(
                        "If you enjoy using our app, please take a moment to rate us. Your feedback helps us improve and provide you with the best experience."
                )
            },
            confirmButton = {
                Text(
                        stringResource(R.string.ok),
                        modifier =
                                Modifier.clickable {
                                    val intent =
                                            Intent(Intent.ACTION_VIEW).apply {
                                                data =
                                                        "https://play.google.com/store/apps/details?id=com.soccertips.predictx".toUri()
                                            }
                                    context.startActivity(intent)
                                    onDismiss()
                                }
                )
            }
    )
}

@Composable
fun AboutUs(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
            title = {
                Text(
                        "About AI ScoreCast",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                            painter = painterResource(id = R.drawable.launcher),
                            contentDescription = "App Icon",
                            modifier =
                                    Modifier.size(80.dp)
                                            .align(Alignment.CenterHorizontally)
                                            .clip(CircleShape)
                    )

                    Text(
                            "AI ScoreCast is your premier football prediction app, providing accurate betting tips and predictions for matches worldwide.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                    )

                    HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                            "Our Features:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                    )

                    FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeatureChip("Daily Tips")
                        FeatureChip("Over/Under")
                        FeatureChip("BTTS")
                        FeatureChip("Daily 2 Odds")
                        FeatureChip("Combo")
                        FeatureChip("HT/FT")
                    }

                    HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                    "Version",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                    text =
                                            try {
                                                        context.packageManager.getPackageInfo(
                                                                        context.packageName,
                                                                        0
                                                                )
                                                                .versionName
                                                    } catch (e: Exception) {
                                                        "Unknown"
                                                    }.toString(),
                                    style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(
                                    Icons.Outlined.Email,
                                    contentDescription = "Contact us",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier =
                                            Modifier.size(20.dp).clickable {
                                                val intent =
                                                        Intent(Intent.ACTION_SENDTO).apply {
                                                            data =
                                                                    "mailto:ouma.monicasales@gmail.com".toUri()
                                                        }
                                                context.startActivity(intent)
                                                onDismiss()
                                            }
                            )
                            Icon(
                                    Icons.Outlined.Policy,
                                    contentDescription = "Privacy Policy",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier =
                                            Modifier.size(20.dp).clickable {
                                                openPrivacyPolicy(context)
                                                onDismiss()
                                            }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                        onClick = onDismiss,
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                ),
                        shape = MaterialTheme.shapes.small
                ) { Text(stringResource(R.string.close)) }
            }
    )
}

@Composable
private fun FeatureChip(text: String) {
    Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
    ) {
        Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun openPrivacyPolicy(context: Context) {
    val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data =
                        "https://predictd.blogspot.com/2025/04/data-custom-classbody-data-custom.html".toUri()
            }
    context.startActivity(intent)
}

@Composable
fun Share(text: String, context: Context) {

    val sendIntent: Intent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                        Intent.EXTRA_TEXT,
                        text +
                                "https://play.google.com/store/apps/details?id=com.soccertips.predictx"
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

            Column(
                    modifier =
                            Modifier.animateContentSize(
                                            animationSpec =
                                                    spring(
                                                            dampingRatio =
                                                                    Spring.DampingRatioLowBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                    )
                                    )
                                    .fillMaxWidth()
                                    .clickable {
                                        if (item.title == "Other") {
                                            onOtherSelected() // Call the function with parentheses
                                        } else {
                                            expandedIndex = if (isExpanded) -1 else index
                                        }
                                    }
            ) {
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
    val items =
            listOf(
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
                            context.packageManager.getPackageInfo(context.packageName, 0)
                                    .versionName
                    val intent =
                            Intent(Intent.ACTION_SEND).apply {
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
            title = { Text(stringResource(R.string.other_feedback)) },
            text = {
                Column {
                    TextField(
                            value = email,
                            onValueChange = {
                                email = it
                                isEmailValid = emailPattern.matches(email)
                            },
                            label = { Text(stringResource(R.string.email_address)) },
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

                                    Text(stringResource(R.string.enter_email_placeholder))
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
                            label = { Text(stringResource(R.string.message)) },
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
                Text(
                        stringResource(R.string.submit),
                        modifier =
                                Modifier.clickable {
                                    if (isEmailValid) {
                                        onSubmit(email, message)
                                        onDismiss()
                                    }
                                }
                )
            },
            dismissButton = {
                Text(stringResource(R.string.cancel), modifier = Modifier.clickable { onDismiss() })
            }
    )
}

@Preview
@Composable
private fun AboutUsPreview() {
    AboutUs(onDismiss = {})
}

@Preview
@Composable
private fun MenuPreview() {
    Menu()
}
