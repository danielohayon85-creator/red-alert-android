package com.shomerapp.alerts.ui.settings.sound

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shomerapp.alerts.audio.SoundPreviewPlayer
import com.shomerapp.alerts.data.local.AppPreferences
import com.shomerapp.alerts.data.local.SoundPreferences
import com.shomerapp.alerts.domain.AlertSimulator
import com.shomerapp.alerts.domain.model.AlertKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoundSettingsViewModel @Inject constructor(
    private val soundPreferences: SoundPreferences,
    private val previewPlayer: SoundPreviewPlayer,
    private val alertSimulator: AlertSimulator,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val immediateSoundUri = soundPreferences.immediateSoundUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val immediateSoundConfirmed = soundPreferences.immediateSoundConfirmed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val prewarningSoundUri = soundPreferences.prewarningSoundUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val prewarningSoundConfirmed = soundPreferences.prewarningSoundConfirmed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onImmediateSoundPicked(uri: Uri) = viewModelScope.launch { soundPreferences.setImmediateSound(uri.toString()) }
    fun onPrewarningSoundPicked(uri: Uri) = viewModelScope.launch { soundPreferences.setPrewarningSound(uri.toString()) }

    fun confirmImmediateTested() = viewModelScope.launch { soundPreferences.confirmImmediateSoundTested() }
    fun confirmPrewarningTested() = viewModelScope.launch { soundPreferences.confirmPrewarningSoundTested() }

    // Lives here, not per-card local state — both sound cards share this one player singleton, so
    // starting a preview on one card must visually stop the other's "playing" indicator too.
    private val _previewingUri = MutableStateFlow<Uri?>(null)
    val previewingUri: StateFlow<Uri?> = _previewingUri.asStateFlow()

    fun previewSound(uri: Uri) {
        _previewingUri.value = uri
        previewPlayer.play(uri)
    }

    fun stopPreview() {
        _previewingUri.value = null
        previewPlayer.stop()
    }

    override fun onCleared() {
        previewPlayer.stop()
        super.onCleared()
    }

    fun simulateFullAlert() {
        viewModelScope.launch {
            val settlements = appPreferences.selectedSettlements.first().toList().ifEmpty { listOf("תל אביב") }
            alertSimulator.simulate(AlertKind.IMMEDIATE, settlements, durationMinutes = 1)
        }
    }
}
