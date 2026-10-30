package com.soccertips.predictx.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.lang.reflect.Type

/**
 * Universal Deserializer for [RootResponse].
 *
 * Reliably handles all backend JSON formats:
 * 1. Root JSON Array: `[ { "home_team": "...", ... }, ... ]` (Static JSON files like `json_betofday.json`)
 * 2. Object with "server_response": `{ "server_response": [ ... ] }` (DailyPredictz PHP scripts)
 * 3. Object with "data", "matches", "predictions", or "results": `{ "data": [ ... ] }` (API proxies)
 */
class RootResponseDeserializer : JsonDeserializer<RootResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): RootResponse {
        if (json == null || json.isJsonNull) {
            return RootResponse(emptyList())
        }

        val listType = object : TypeToken<List<ServerResponse>>() {}.type

        try {
            // Case 1: Top-level JSON Array: [ { ... }, { ... } ]
            if (json.isJsonArray) {
                val items: List<ServerResponse> = context?.deserialize(json, listType) ?: emptyList()
                return RootResponse(items)
            }

            // Case 2: JSON Object with wrapped array
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                val arrayElement = when {
                    obj.has("server_response") && obj.get("server_response").isJsonArray -> obj.get("server_response")
                    obj.has("data") && obj.get("data").isJsonArray -> obj.get("data")
                    obj.has("matches") && obj.get("matches").isJsonArray -> obj.get("matches")
                    obj.has("results") && obj.get("results").isJsonArray -> obj.get("results")
                    obj.has("predictions") && obj.get("predictions").isJsonArray -> obj.get("predictions")
                    else -> null
                }

                if (arrayElement != null) {
                    val items: List<ServerResponse> = context?.deserialize(arrayElement, listType) ?: emptyList()
                    return RootResponse(items)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "RootResponseDeserializer failed to parse JSON: $json")
        }

        return RootResponse(emptyList())
    }
}
