package com.soccertips.predcompose.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteItem(
    @PrimaryKey val fixtureId: String,
    val homeTeam: String?,
    val awayTeam: String?,
    val league: String?,
    val mDate: String?,
    val mTime: String?,
    val mStatus: String?,
    val outcome: String?,
    val pick: String?,
    val color: Int,
    val hLogoPath: String?,
    val aLogoPath: String?,
    val leagueLogo: String?

    )

