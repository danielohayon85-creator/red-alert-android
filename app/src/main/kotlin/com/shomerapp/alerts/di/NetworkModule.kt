package com.shomerapp.alerts.di

import android.content.Context
import com.shomerapp.alerts.data.areas.AreaRepository
import com.shomerapp.alerts.data.remote.AlertFetcher
import com.shomerapp.alerts.data.remote.AlertFetcherSwitch
import com.shomerapp.alerts.domain.AlertClassifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingsModule {
    /** AlertFetcherSwitch is what everything actually depends on so Stage 6's Debug Panel can
     *  flip mock mode; this just satisfies plain [AlertFetcher] injection sites (the polling
     *  repository doesn't need to know mock mode exists). */
    @Binds
    abstract fun bindAlertFetcher(switch: AlertFetcherSwitch): AlertFetcher
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideAreaRepository(@ApplicationContext context: Context, json: Json): AreaRepository {
        val raw = context.assets.open("areas.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return AreaRepository(raw, json)
    }

    @Provides
    @Singleton
    fun provideAlertClassifier(@ApplicationContext context: Context, json: Json): AlertClassifier {
        val raw = context.assets.open("alert_rules.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return AlertClassifier(raw, json)
    }
}
