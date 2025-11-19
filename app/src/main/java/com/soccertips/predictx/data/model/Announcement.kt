package com.soccertips.predictx.data.model

data class Announcement(
        val id: String = "",
        val title: String = "",
        val message: String = "",
        val type: AnnouncementType = AnnouncementType.INFO,
        val actionText: String? = null,
        val actionUrl: String? = null,
        val isVisible: Boolean = false,
        val priority: Int = 0 // Higher priority shows first
)

enum class AnnouncementType {
    INFO, // Blue info icon
    WARNING, // Yellow warning icon
    ERROR, // Red error icon
    SUCCESS, // Green success icon
    UPDATE // Purple update icon
}
