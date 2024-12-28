package com.soccertips.predcompose.data.model

import com.google.gson.annotations.SerializedName

data class RootResponse(
    @SerializedName("server_response")
    val serverResponse: List<ServerResponse>,
)
