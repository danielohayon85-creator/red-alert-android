package com.shomerapp.alerts.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AlertSessionDao {
    @Query("SELECT * FROM alert_session WHERE id = ${AlertSessionEntity.SINGLETON_ID}")
    suspend fun get(): AlertSessionEntity?

    @Upsert
    suspend fun save(entity: AlertSessionEntity)

    @Query("DELETE FROM alert_session")
    suspend fun clear()
}
