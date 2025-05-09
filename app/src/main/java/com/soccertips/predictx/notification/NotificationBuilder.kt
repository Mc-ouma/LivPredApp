package com.soccertips.predictx.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.soccertips.predictx.MainActivity
import com.soccertips.predictx.R
import com.soccertips.predictx.data.local.entities.FavoriteItem
import com.soccertips.predictx.data.model.FixtureResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationBuilder(private val context: Context) {

    suspend fun buildMatchNotification(item: FavoriteItem): NotificationCompat.Builder {
        val homeTeamLogo = loadTeamLogo(item.hLogoPath)
        val awayTeamLogo = loadTeamLogo(item.aLogoPath)

        return NotificationCompat.Builder(context, NotificationHelper.MATCH_REMINDER_CHANNEL_ID)
            .setContentTitle("${item.homeTeam} vs ${item.awayTeam}")
            .setContentText("Match starts in 15 minutes!")
            .setSmallIcon(R.drawable.launcher)
            .setLargeIcon(homeTeamLogo)
            .setAutoCancel(true)
            .setStyle(createBigPictureStyle(item, homeTeamLogo, awayTeamLogo))
            .setContentIntent(createPendingIntent(item))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter(item.mTime?.let { calculateTimeoutDuration(it) } ?: 3600000L)
            .addAction(
                R.drawable.ic_view_match,
                "View Match Details",
                createPendingIntent(item)
            )
    }

    //New function
    fun buildMatchUpdateNotification(fixtureResponse: FixtureResponse): NotificationCompat.Builder {
        val fixture = fixtureResponse.response.first()
        return NotificationCompat.Builder(context, NotificationHelper.MATCH_UPDATES_CHANNEL_ID)
            .setContentTitle("${fixture.teams?.home?.name} vs ${fixture.teams.away.name}")
            .setContentText("Score: ${fixture.goals?.home} - ${fixture.goals?.away}") //Example of updated content
            .setSmallIcon(R.drawable.launcher)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(fixture.fixture.id.toString()))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    private suspend fun loadTeamLogo(logoUrl: String?): Bitmap? = withContext(Dispatchers.IO) {
        try {
            logoUrl?.let {
                Glide.with(context)
                    .asBitmap()
                    .load(it)
                    .submit()
                    .get()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createBigPictureStyle(
        item: FavoriteItem,
        homeTeamLogo: Bitmap?,
        awayTeamLogo: Bitmap?
    ): NotificationCompat.BigPictureStyle {
        return NotificationCompat.BigPictureStyle()
            .bigPicture(awayTeamLogo)
            .setSummaryText(
                "${item.league}\n${item.mDate} ${item.mTime}"
            )
    }

    private fun createPendingIntent(item: FavoriteItem): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fixtureId", item.fixtureId)
            putExtra("notification_opened", true)
        }

        return PendingIntent.getActivity(
            context,
            item.fixtureId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    //New function
    private fun createPendingIntent(fixtureId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fixtureId", fixtureId)
            putExtra("notification_opened", true)
        }

        return PendingIntent.getActivity(
            context,
            fixtureId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun calculateTimeoutDuration(matchTime: String): Long {
        // Add logic to calculate when the notification should timeout based on match time
        return 3600000L // Default 1 hour
    }
}