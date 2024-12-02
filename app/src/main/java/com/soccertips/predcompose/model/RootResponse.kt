package com.soccertips.predcompose.model

import com.google.gson.annotations.SerializedName

data class RootResponse(
    @SerializedName("server_response")
    val serverResponse: List<ServerResponse>,
)
