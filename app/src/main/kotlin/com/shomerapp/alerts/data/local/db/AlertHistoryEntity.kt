package com.shomerapp.alerts.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_history")
data class AlertHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val title: String,
    val desc: String,
    val settlementsCsv: String,
    val startedAtEpochMillis: Long,
    val concludedAtEpochMillis: Long,
)
