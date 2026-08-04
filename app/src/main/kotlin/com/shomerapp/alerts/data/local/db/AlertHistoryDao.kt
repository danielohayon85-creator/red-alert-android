package com.shomerapp.alerts.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertHistoryDao {
    @Insert
    suspend fun insert(entity: AlertHistoryEntity)

    @Query("SELECT * FROM alert_history ORDER BY startedAtEpochMillis DESC LIMIT :limit")
    fun recent(limit: Int = 100): Flow<List<AlertHistoryEntity>>
}
