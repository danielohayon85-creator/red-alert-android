package com.shomerapp.alerts.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AlertSessionEntity::class, AlertHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertSessionDao(): AlertSessionDao
    abstract fun alertHistoryDao(): AlertHistoryDao
}
