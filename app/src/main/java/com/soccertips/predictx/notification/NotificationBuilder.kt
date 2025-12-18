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
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import coil.imageLoader
import coil.request.ImageRequest
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
        return try {
            val homeTeamLogo = loadTeamLogo(item.hLogoPath)
            val awayTeamLogo = loadTeamLogo(item.aLogoPath)

            val largeIcon = createVersusIcon(homeTeamLogo, awayTeamLogo)

            NotificationCompat.Builder(context, NotificationHelper.MATCH_REMINDER_CHANNEL_ID)
                .setContentTitle(
                    "${item.homeTeam ?: "Unknown"} vs ${item.awayTeam ?: "Unknown"}"
                )
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
                .addAction(
                    R.drawable.ic_view_match,
                    context.getString(R.string.view_details),
                    createPendingIntent(item)
                )
        } catch (e: Exception) {
            Timber.e(
                e,
                "Failed to build match notification for fixture ${item.fixtureId}, creating fallback notification"
            )
            // Create a fallback notification with minimal data
            NotificationCompat.Builder(context, NotificationHelper.MATCH_REMINDER_CHANNEL_ID)
                .setContentTitle(
                    "${item.homeTeam ?: "Unknown"} vs ${item.awayTeam ?: "Unknown"}"
                )
                .setContentText(context.getString(R.string.match_starts_in_15_minutes))
                .setSmallIcon(R.drawable.launcher)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setGroup(GROUP_KEY_MATCHES)
                .setContentIntent(createSafePendingIntent(item))
        }
    }

    fun buildMatchUpdateNotification(fixtureResponse: FixtureResponse): NotificationCompat.Builder {
        return try {
            val fixture = fixtureResponse.response.first()
            val homeGoals = fixture.goals.home
            val awayGoals = fixture.goals.away
            val matchStatus = fixture.fixture.status.short

            val homeTeam = fixture.teams.home.name
            val awayTeam = fixture.teams.away.name

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
                    .setSummaryText(fixture.league.name)

            NotificationCompat.Builder(context, NotificationHelper.MATCH_UPDATES_CHANNEL_ID)
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
        } catch (e: Exception) {
            Timber.e(e, "Failed to build match update notification, creating fallback")
            // Create a fallback notification with minimal data
            NotificationCompat.Builder(context, NotificationHelper.MATCH_UPDATES_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.match_update_title))
                .setContentText(context.getString(R.string.match_update_fallback))
                .setSmallIcon(R.drawable.launcher)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setGroup(GROUP_KEY_UPDATES)
                .setContentIntent(createDefaultPendingIntent())
        }
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

    /** Build a congratulatory notification for perfect betting days */
    fun buildBettingSuccessNotification(
        date: String,
        categoryName: String,
        categoryUrl: String,
        totalMatches: Int,
        winningMatches: Int,
        successRate: Int,
        matchDetails: List<String>
    ): NotificationCompat.Builder {
        return try {
            val title = context.getString(R.string.perfect_day_in_category, categoryName)
            val shortText =
                context.getString(
                    R.string.all_matches_won_with_rate,
                    winningMatches,
                    successRate
                )

            // Create detailed content for expanded view
            val detailedContent =
                StringBuilder()
                    .apply {
                        append(
                            context.getString(R.string.category_label, categoryName) +
                                    "\n"
                        )
                        append(
                            context.getString(
                                R.string.success_rate_label,
                                successRate
                            ) + "\n"
                        )
                        append(
                            context.getString(
                                R.string.perfect_day_stats,
                                winningMatches,
                                totalMatches
                            ) + "\n"
                        )
                        append(context.getString(R.string.date_label, date) + "\n\n")
                        append(context.getString(R.string.match_results_label) + "\n")
                        matchDetails.take(10).forEach { detail
                            -> // Limit to 10 matches for readability
                            append("$detail\n")
                        }
                        if (matchDetails.size > 10) {
                            append(
                                context.getString(
                                    R.string.and_more_matches,
                                    matchDetails.size - 10
                                )
                            )
                        }
                    }
                    .toString()

            val bigTextStyle =
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(detailedContent)
                    .setSummaryText(
                        context.getString(
                            R.string.category_matches_won,
                            categoryName,
                            winningMatches
                        )
                    )

            // Create intent to open results screen for this specific category
            val viewResultsIntent = createBettingSuccessIntent(categoryUrl, date)

            // Create share intent
            val shareText =
                context.getString(
                    R.string.betting_success_share_text,
                    categoryName,
                    winningMatches,
                    successRate,
                    date
                )
            val shareIntent = createShareIntent(shareText)

            NotificationCompat.Builder(context, NotificationHelper.BETTING_SUCCESS_CHANNEL_ID)
                .setSmallIcon(R.drawable.launcher)
                .setContentTitle(title)
                .setContentText(shortText)
                .setStyle(bigTextStyle)
                .setAutoCancel(true)
                .setContentIntent(viewResultsIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(context.getColor(R.color.success_green))
                .addAction(R.drawable.ic_visibility, "View Results", viewResultsIntent)
                .addAction(R.drawable.ic_share, "Share Success", shareIntent)
                .setLights(Color.GREEN, 1000, 1000)
                .setVibrate(longArrayOf(0, 500, 200, 500))
        } catch (e: Exception) {
            Timber.e(e, "Failed to build betting success notification")
            // Create a simple fallback notification
            NotificationCompat.Builder(context, NotificationHelper.BETTING_SUCCESS_CHANNEL_ID)
                .setSmallIcon(R.drawable.launcher)
                .setContentTitle(context.getString(R.string.perfect_betting_day))
                .setContentText(
                    context.getString(
                        R.string.all_matches_won_with_rate,
                        winningMatches,
                        successRate
                    )
                )
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(createDefaultPendingIntent())
        }
    }

    private suspend fun loadTeamLogo(logoUrl: String?): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                logoUrl?.let {
                    val imageRequest = ImageRequest.Builder(context).data(it).build()
                    val drawable = context.imageLoader.execute(imageRequest).drawable
                    drawable?.toBitmap()
                }
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
        return try {
            val leagueInfo = item.league?.let { "League: $it" } ?: ""
            val matchTime = "Date: ${item.mDate ?: "TBD"} Time: ${item.mTime ?: "TBD"}"
            val pickInfo = item.pick?.let { "Your Pick: $it" } ?: ""

            NotificationCompat.BigTextStyle()
                .setBigContentTitle(
                    "${item.homeTeam ?: "Unknown"} vs ${item.awayTeam ?: "Unknown"}"
                )
                .bigText(
                    context.getString(R.string.match_starts_in_15_minutes) +
                            "\n$leagueInfo\n$matchTime${if (pickInfo.isNotEmpty()) "\n$pickInfo" else ""}"
                )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create big text style for fixture ${item.fixtureId}")
            // Return a simple style as fallback
            NotificationCompat.BigTextStyle()
                .setBigContentTitle(
                    "${item.homeTeam ?: "Unknown"} vs ${item.awayTeam ?: "Unknown"}"
                )
                .bigText(context.getString(R.string.match_starts_in_15_minutes))
        }
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
        return try {
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

            PendingIntent.getActivity(
                context,
                fixtureId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create pending intent for fixture $fixtureId")
            createDefaultPendingIntent()
        }
    }

    /** Creates a safe PendingIntent for match notifications with enhanced error handling */
    private fun createSafePendingIntent(item: FavoriteItem): PendingIntent {
        return try {
            createPendingIntent(item)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create safe pending intent for fixture ${item.fixtureId}")
            createDefaultPendingIntent()
        }
    }

    /** Creates a default PendingIntent as a fallback when specific intents fail */
    private fun createDefaultPendingIntent(): PendingIntent {
        return try {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    action = "com.soccertips.predictx.ACTION_MAIN"
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("fromNotification", true)
                }

            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create default pending intent")
            // As a last resort, create a basic intent to the main activity
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    /** Create a PendingIntent for betting success notification that opens results */
    private fun createBettingSuccessIntent(categoryUrl: String, date: String): PendingIntent {
        return try {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    action = "com.soccertips.predictx.ACTION_VIEW_BETTING_SUCCESS"
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("fromNotification", true)
                    putExtra("betting_date", date)
                    putExtra("category_url", categoryUrl)
                    putExtra("show_results", true)
                }

            PendingIntent.getActivity(
                context,
                ("betting_success_${categoryUrl}_$date").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Exception) {
            Timber.e(
                e,
                "Failed to create betting success intent for category: $categoryUrl, date: $date"
            )
            createDefaultPendingIntent()
        }
    }

    /** Create a PendingIntent for sharing success */
    private fun createShareIntent(shareText: String): PendingIntent {
        return try {
            val shareIntent =
                Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Success")

            PendingIntent.getActivity(
                context,
                shareText.hashCode(),
                chooserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create share intent")
            createDefaultPendingIntent()
        }
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
