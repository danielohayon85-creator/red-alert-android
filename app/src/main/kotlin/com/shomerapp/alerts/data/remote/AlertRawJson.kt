package com.shomerapp.alerts.data.remote

import com.shomerapp.alerts.domain.model.RawOrefAlert
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
private data class RawAlertJson(
    val id: String,
    val cat: String,
    val title: String,
    val data: JsonElement,
    val desc: String,
)

/**
 * `data` is normally a JSON array of settlement names, but older/alternate responses have
 * returned a single comma-separated string instead (§4 "מלכודות") — handle both.
 */
private fun parseCities(element: JsonElement): List<String> = when (element) {
    is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.ifEmpty { null } }
    is JsonPrimitive -> element.contentOrNull
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
    else -> emptyList()
}

/**
 * Parses an already BOM-stripped response body. Returns null for a blank body ("no alert" is an
 * empty/whitespace-only body per spec, not an empty JSON object — must be handled explicitly).
 * Throws [SerializationException] on a malformed non-blank body; callers decide how to surface that.
 */
fun parseOrefResponse(body: String, json: Json): RawOrefAlert? {
    if (body.isBlank()) return null
    val dto = json.decodeFromString<RawAlertJson>(body)
    return RawOrefAlert(
        id = dto.id,
        cat = dto.cat,
        title = dto.title,
        cities = parseCities(dto.data),
        desc = dto.desc,
    )
}
