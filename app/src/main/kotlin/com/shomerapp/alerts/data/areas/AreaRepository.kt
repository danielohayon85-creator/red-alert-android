package com.shomerapp.alerts.data.areas

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Loads areas.json (area name -> settlement names, ~30 areas / ~1400 settlements) and provides
 * normalized settlement lookup. Pure/no Android dependency — the asset is read by the DI module,
 * this class only parses the JSON string, so it's directly unit-testable.
 *
 * Some settlements belong to more than one alert area (§4), so lookup returns a list, never a
 * single area.
 */
class AreaRepository(areasJson: String, json: Json = Json { ignoreUnknownKeys = true }) {
    private val settlementsByArea: Map<String, List<String>> = json.decodeFromString(areasJson)

    // Strips -,() and whitespace — oref's `data` field spelling doesn't always match areas.json exactly (§4).
    private val normalizationRegex = Regex("[\\-,()\\s]+")

    fun normalize(name: String): String = name.replace(normalizationRegex, "").trim()

    private val areasBySettlement: Map<String, List<String>> by lazy {
        val index = mutableMapOf<String, MutableList<String>>()
        settlementsByArea.forEach { (area, settlements) ->
            settlements.forEach { settlement ->
                index.getOrPut(normalize(settlement)) { mutableListOf() }.add(area)
            }
        }
        index
    }

    fun areasForSettlement(rawSettlementName: String): List<String> =
        areasBySettlement[normalize(rawSettlementName)].orEmpty()

    fun allAreas(): List<String> = settlementsByArea.keys.sorted()

    fun settlementsInArea(area: String): List<String> = settlementsByArea[area].orEmpty()

    fun allSettlements(): List<String> = settlementsByArea.values.flatten().distinct()
}
