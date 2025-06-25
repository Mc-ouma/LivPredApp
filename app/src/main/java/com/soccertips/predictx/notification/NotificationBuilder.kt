package com.soccertips.predictx.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.soccertips.predictx.MainActivity
import com.soccertips.predictx.R
import com.soccertips.predictx.data.local.entities.FavoriteItem
import com.soccertips.predictx.data.model.FixtureResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class NotificationBuilder @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private const val GROUP_KEY_MATCHES = "com.soccertips.predictx.MATCH_NOTIFICATIONS"
        private const val GROUP_KEY_UPDATES = "com.soccertips.predictx.MATCH_UPDATES"
    }

    suspend fun buildMatchNotification(item: FavoriteItem): NotificationCompat.Builder {
        val homeTeamLogo = loadTeamLogo(item.hLogoPath)
        val awayTeamLogo = loadTeamLogo(item.aLogoPath)

        val largeIcon = createVersusIcon(homeTeamLogo, awayTeamLogo)

        return NotificationCompat.Builder(context, NotificationHelper.MATCH_REMINDER_CHANNEL_ID)
                .setContentTitle("${item.homeTeam} vs ${item.awayTeam}")
                .setContentText(context.getString(R.string.match_starts_in_15_minutes))
                .setSmallIcon(R.drawable.launcher)
                .setLargeIcon(largeIcon ?: homeTeamLogo)
                .setAutoCancel(true)
                .setStyle(createBigTextStyle(item))
                .setContentIntent(createPendingIntent(item))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setGroup(GROUP_KEY_MATCHES)
                .setTimeoutAfter(item.mTime?.let { calculateTimeoutDuration(it) } ?: 3600000L)
                .addAction(R.drawable.ic_view_match, "View Details", createPendingIntent(item))
    }

    fun buildMatchUpdateNotification(fixtureResponse: FixtureResponse): NotificationCompat.Builder {
        val fixture = fixtureResponse.response.first()
        val homeGoals = fixture.goals?.home ?: 0
        val awayGoals = fixture.goals?.away ?: 0
        val matchStatus = fixture.fixture.status.short

        val homeTeam = fixture.teams?.home?.name ?: ""
        val awayTeam = fixture.teams?.away?.name ?: ""

        // Create content text based on match status
        val contentText =
                when {
                    matchStatus == "1H" -> "1st Half: $homeGoals - $awayGoals"
                    matchStatus == "HT" -> "Half Time: $homeGoals - $awayGoals"
                    matchStatus == "2H" -> "2nd Half: $homeGoals - $awayGoals"
                    matchStatus in setOf("FT", "AET", "PEN") ->
                            "Final Score: $homeGoals - $awayGoals"
                    matchStatus in setOf("PST", "CANC", "SUSP", "ABD") ->
                            "Match ${getStatusDescription(matchStatus)}"
                    else -> "Score: $homeGoals - $awayGoals"
                }

        val style =
                NotificationCompat.BigTextStyle()
                        .setBigContentTitle("$homeTeam vs $awayTeam")
                        .bigText("$contentText\n${getStatusDescription(matchStatus)}")
                        .setSummaryText(fixture.league?.name)

        return NotificationCompat.Builder(context, NotificationHelper.MATCH_UPDATES_CHANNEL_ID)
                .setContentTitle("$homeTeam vs $awayTeam")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.launcher)
                .setAutoCancel(true)
                .setStyle(style)
                .setContentIntent(createPendingIntent(fixture.fixture.id.toString()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setGroup(GROUP_KEY_UPDATES)
    }

    fun buildSummaryNotification(matchCount: Int, groupKey: String): NotificationCompat.Builder {
        val channelId =
                if (groupKey == GROUP_KEY_MATCHES) {
                    NotificationHelper.MATCH_REMINDER_CHANNEL_ID
                } else {
                    NotificationHelper.MATCH_UPDATES_CHANNEL_ID
                }

        val title =
                if (groupKey == GROUP_KEY_MATCHES) {
                    context.getString(R.string.upcoming_matches)
                } else {
                    context.getString(R.string.match_updates)
                }

        return NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.matches_count, matchCount))
                .setSmallIcon(R.drawable.launcher)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
    }

    private suspend fun loadTeamLogo(logoUrl: String?): Bitmap? =
            withContext(Dispatchers.IO) {
                try {
                    logoUrl?.let { Glide.with(context).asBitmap().load(it).submit().get() }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load team logo: $logoUrl")
                    null
                }
            }

    private suspend fun createVersusIcon(homeLogo: Bitmap?, awayLogo: Bitmap?): Bitmap? {
        if (homeLogo == null || awayLogo == null) return null

        return withContext(Dispatchers.Default) {
            try {
                val size = 144 // Size for the combined icon (adjust as needed)
                val result = createBitmap(size, size)
                val canvas = Canvas(result)

                // Draw a semi-transparent background
                val backgroundPaint = Paint().apply { color = "#22000000".toColorInt() }
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), backgroundPaint)

                // Draw home team logo on the left
                val scaledHomeLogo = homeLogo.scale(size / 2, size / 2)
                canvas.drawBitmap(scaledHomeLogo, 0f, size / 4f, null)

                // Draw away team logo on the right
                val scaledAwayLogo = awayLogo.scale(size / 2, size / 2)
                canvas.drawBitmap(scaledAwayLogo, size / 2f, size / 4f, null)

                // Draw a "VS" text in the middle
                val textPaint =
                        Paint().apply {
                            color = Color.WHITE
                            textSize = size / 5f
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                canvas.drawText(context.getString(R.string.vs), size / 2f, size / 2f, textPaint)

                result
            } catch (e: Exception) {
                Timber.e(e, "Failed to create versus icon")
                null
            }
        }
    }

    private fun createBigTextStyle(item: FavoriteItem): NotificationCompat.BigTextStyle {
        val leagueInfo = item.league?.let { "League: $it" } ?: ""
        val matchTime = "Date: ${item.mDate} Time: ${item.mTime}"
        val pickInfo = item.pick?.let { "Your Pick: $it" } ?: ""

        return NotificationCompat.BigTextStyle()
                .setBigContentTitle("${item.homeTeam} vs ${item.awayTeam}")
                .bigText(
                        context.getString(R.string.match_starts_in_15_minutes) +
                                "\n$leagueInfo\n$matchTime${if (pickInfo.isNotEmpty()) "\n$pickInfo" else ""}"
                )
    }

    private fun createPendingIntent(item: FavoriteItem): PendingIntent {
        val intent =
                Intent(context, MainActivity::class.java).apply {
                    action = "com.soccertips.predictx.ACTION_VIEW_MATCH"
                    flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("fixtureId", item.fixtureId)
                    // Add explicit flag to indicate this is from a notification
                    putExtra("fromNotification", true)
                }

        return PendingIntent.getActivity(
                context,
                item.fixtureId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPendingIntent(fixtureId: String): PendingIntent {
        val intent =
                Intent(context, MainActivity::class.java).apply {
                    action = "com.soccertips.predictx.ACTION_VIEW_MATCH"
                    flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("fixtureId", fixtureId)
                    // Add explicit flag to indicate this is from a notification
                    putExtra("fromNotification", true)
                }

        return PendingIntent.getActivity(
                context,
                fixtureId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun calculateTimeoutDuration(matchTime: String): Long {
        // Parse the match time to determine when it should timeout
        try {
            val parts = matchTime.split(":")
            if (parts.size == 2) {
                // Assuming average match duration of 2 hours
                return 2 * 60 * 60 * 1000L
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing match time for timeout: $matchTime")
        }
        return 3 * 60 * 60 * 1000L // Default 3 hours if parsing fails
    }

    private fun getStatusDescription(status: String): String {
        return when (status) {
            "NS" -> "Not Started"
            "1H" -> "First Half"
            "HT" -> "Half Time"
            "2H" -> "Second Half"
            "ET" -> "Extra Time"
            "BT" -> "Break Time"
            "P" -> "Penalty"
            "SUSP" -> "Suspended"
            "INT" -> "Interrupted"
            "FT" -> "Full Time"
            "AET" -> "After Extra Time"
            "PEN" -> "Penalties"
            "PST" -> "Postponed"
            "CANC" -> "Cancelled"
            "ABD" -> "Abandoned"
            "AWD" -> "Technical Loss"
            "WO" -> "Walkover"
            "LIVE" -> "In Progress"
            else -> status
        }
    }
}
