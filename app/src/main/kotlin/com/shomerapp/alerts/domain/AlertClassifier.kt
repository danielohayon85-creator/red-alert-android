package com.shomerapp.alerts.domain

import com.shomerapp.alerts.domain.model.AlertKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AlertRulesConfig(
    val titleRules: List<TitleRule>,
    val defaultKind: String,
)

@Serializable
data class TitleRule(
    val kind: String,
    val match: String,
    val patterns: List<String>,
)

/**
 * Routes by `title` string first, `cat` only ever as a secondary signal — never as the primary
 * key — because cat's meaning has changed over time (§4, e.g. cat 13 flipped meaning in 2025).
 * Rules load from alert_rules.json (not hardcoded) so they can be updated without a release.
 * Pure/no Android dependency on purpose: the asset is read by the DI module, this class only
 * parses the JSON string, so it's directly unit-testable.
 */
class AlertClassifier(rulesJson: String, json: Json = Json { ignoreUnknownKeys = true }) {
    private val config = json.decodeFromString(AlertRulesConfig.serializer(), rulesJson)
    private val defaultKind = AlertKind.valueOf(config.defaultKind)

    init {
        check(defaultKind != AlertKind.ALL_CLEAR && defaultKind != AlertKind.PREWARNING) {
            "defaultKind must never be ALL_CLEAR/PREWARNING — those must only ever come from an explicit title match (§4)"
        }
    }

    /** [cat] is accepted for future corroboration/anomaly logging but intentionally unused for routing. */
    fun classify(title: String, cat: String): AlertKind {
        val normalizedTitle = title.trim().replace(Regex("\\s+"), " ")
        for (rule in config.titleRules) {
            val kind = runCatching { AlertKind.valueOf(rule.kind) }.getOrNull() ?: continue
            val matched = when (rule.match) {
                "contains" -> rule.patterns.any { normalizedTitle.contains(it, ignoreCase = true) }
                "startsWith" -> rule.patterns.any { normalizedTitle.startsWith(it, ignoreCase = true) }
                else -> false
            }
            if (matched) return kind
        }
        return defaultKind
    }
}
