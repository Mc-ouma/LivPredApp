package com.soccertips.predictx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soccertips.predictx.data.model.Announcement
import com.soccertips.predictx.data.model.AnnouncementType

@Composable
fun AnnouncementCard(
        announcement: Announcement,
        onDismiss: () -> Unit,
        onActionClick: ((String) -> Unit)? = null
) {
    val (containerColor, contentColor, icon) =
            when (announcement.type) {
                AnnouncementType.INFO ->
                        Triple(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                Icons.Default.Info
                        )
                AnnouncementType.WARNING ->
                        Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.Warning)
                AnnouncementType.ERROR ->
                        Triple(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer,
                                Icons.Default.Warning
                        )
                AnnouncementType.SUCCESS ->
                        Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.CheckCircle)
                AnnouncementType.UPDATE ->
                        Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), Icons.Default.Star)
            }

    Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (announcement.title.isNotBlank()) {
                    Text(
                            text = announcement.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                        text = announcement.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                )

                if (announcement.actionText != null && announcement.actionUrl != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                            onClick = { onActionClick?.invoke(announcement.actionUrl) },
                            colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                    ) { Text(announcement.actionText) }
                }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = contentColor
                )
            }
        }
    }
}
