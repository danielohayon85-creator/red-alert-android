package com.shomerapp.alerts.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table (§10 "ריסטארט של הטלפון באמצע התרעה פעילה → שחזור מצב האירוע מ-Room"):
 * mirrors the current [com.shomerapp.alerts.domain.model.AlertSessionState] so a process restart
 * mid-alert can rebuild it instead of losing the active session. Cleared on reaching Idle.
 */
@Entity(tableName = "alert_session")
data class AlertSessionEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val phase: String,
    val startedAtEpochMillis: Long,
    val prewarningStartedAtEpochMillis: Long?,
    val settlementsCsv: String,
    val title: String,
    val desc: String,
    val durationSeconds: Int,
    val acknowledgedByUser: Boolean,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
