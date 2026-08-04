package com.shomerapp.alerts.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.soundDataStore by preferencesDataStore(name = "sound_prefs")

/**
 * Chosen alert sounds + the mandatory "user actually tested it" gate (§2.5: a sound never takes
 * effect until the user pressed "test" and confirmed "I heard it, it'll wake me up") + the
 * pre-alert alarm volume to restore afterward, persisted so it survives a crash mid-alert (§2.2).
 */
@Singleton
class SoundPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val IMMEDIATE_SOUND_URI = stringPreferencesKey("immediate_sound_uri")
        val IMMEDIATE_SOUND_CONFIRMED = booleanPreferencesKey("immediate_sound_confirmed")
        val PREWARNING_SOUND_URI = stringPreferencesKey("prewarning_sound_uri")
        val PREWARNING_SOUND_CONFIRMED = booleanPreferencesKey("prewarning_sound_confirmed")
        val VOLUME_TO_RESTORE = intPreferencesKey("alarm_volume_to_restore")
    }

    val immediateSoundUri: Flow<String?> = context.soundDataStore.data.map { it[Keys.IMMEDIATE_SOUND_URI] }
    val immediateSoundConfirmed: Flow<Boolean> = context.soundDataStore.data.map { it[Keys.IMMEDIATE_SOUND_CONFIRMED] ?: false }
    val prewarningSoundUri: Flow<String?> = context.soundDataStore.data.map { it[Keys.PREWARNING_SOUND_URI] }
    val prewarningSoundConfirmed: Flow<Boolean> = context.soundDataStore.data.map { it[Keys.PREWARNING_SOUND_CONFIRMED] ?: false }

    /** Null once there's nothing pending to restore — either no alert has run yet, or the last
     *  one ended cleanly and consumed this value. A non-null value found at app startup means
     *  the process died mid-alert (§2.2 crash recovery). */
    val volumeToRestore: Flow<Int?> = context.soundDataStore.data.map { it[Keys.VOLUME_TO_RESTORE] }

    /** Picking a new sound always clears the "tested" flag — an untested sound (even one that
     *  replaces a previously-tested one) must not be trusted for a real alert. */
    suspend fun setImmediateSound(uriString: String) = context.soundDataStore.edit {
        it[Keys.IMMEDIATE_SOUND_URI] = uriString
        it[Keys.IMMEDIATE_SOUND_CONFIRMED] = false
    }

    suspend fun confirmImmediateSoundTested() = context.soundDataStore.edit {
        it[Keys.IMMEDIATE_SOUND_CONFIRMED] = true
    }

    suspend fun setPrewarningSound(uriString: String) = context.soundDataStore.edit {
        it[Keys.PREWARNING_SOUND_URI] = uriString
        it[Keys.PREWARNING_SOUND_CONFIRMED] = false
    }

    suspend fun confirmPrewarningSoundTested() = context.soundDataStore.edit {
        it[Keys.PREWARNING_SOUND_CONFIRMED] = true
    }

    /** Only sets the value if nothing is already pending — see [AlarmVolumeController]: when
     *  PREWARNING chains straight into IMMEDIATE, the second raise must not overwrite the
     *  original pre-alert baseline with the already-elevated PREWARNING volume. */
    suspend fun saveVolumeToRestoreIfAbsent(volume: Int) = context.soundDataStore.edit {
        if (it[Keys.VOLUME_TO_RESTORE] == null) it[Keys.VOLUME_TO_RESTORE] = volume
    }

    suspend fun clearVolumeToRestore() = context.soundDataStore.edit {
        it.remove(Keys.VOLUME_TO_RESTORE)
    }
}
