package com.soccertips.predcompose.data.model

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName

data class ServerResponse(
    @SerializedName("m_time") var mTime: String? = null,
    @SerializedName("pick") var pick: String? = null,
    @SerializedName("country") var country: String? = null,
    @SerializedName("league") var league: String? = null,
    @SerializedName("m_date") var mDate: String? = null,
    @SerializedName("home_team") var homeTeam: String? = null,
    @SerializedName("away_team") var awayTeam: String? = null,
    @SerializedName("bet_odds") var betOdds: String? = null,
    @SerializedName("outcome") var outcome: String? = null,
    @SerializedName("ht_score") var htScore: String? = null,
    @SerializedName("result") var result: String? = null,
    @SerializedName("h_logo_path") var hLogoPath: String? = null,
    @SerializedName("a_logo_path") var aLogoPath: String? = null,
    @SerializedName("league_logo") var leagueLogo: String? = null,
    @SerializedName("fixture_id") var fixtureId: String? = null,
    @SerializedName("m_status") var mStatus: String? = null,
    val color: Color = Color.Unspecified,
)
