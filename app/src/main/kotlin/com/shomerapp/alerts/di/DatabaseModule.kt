package com.shomerapp.alerts.di

import android.content.Context
import androidx.room.Room
import com.shomerapp.alerts.data.local.db.AlertHistoryDao
import com.shomerapp.alerts.data.local.db.AlertSessionDao
import com.shomerapp.alerts.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "azakon.db").build()

    @Provides
    fun provideAlertSessionDao(db: AppDatabase): AlertSessionDao = db.alertSessionDao()

    @Provides
    fun provideAlertHistoryDao(db: AppDatabase): AlertHistoryDao = db.alertHistoryDao()
}
