package com.shomerapp.alerts.ui.onboarding

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shomerapp.alerts.data.local.AppPreferences
import com.shomerapp.alerts.domain.OemGuidance
import com.shomerapp.alerts.domain.OemInstruction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** §7: one screen per step, in this exact order — steps 3 (notifications), 6 (battery) and 8
 *  (sound) may never be skipped. */
enum class OnboardingStep { WELCOME, AREAS, NOTIFICATIONS, DND, FULL_SCREEN_INTENT, BATTERY, OEM, SOUND, FINISH }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val oemInstruction: OemInstruction? = OemGuidance.instructionsFor(Build.MANUFACTURER)

    private val steps: List<OnboardingStep> = OnboardingStep.entries.filter { it != OnboardingStep.OEM || oemInstruction != null }

    private val _stepIndex = MutableStateFlow(0)
    private val _currentStep = MutableStateFlow(steps.first())
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    fun next() {
        val nextIndex = (_stepIndex.value + 1).coerceAtMost(steps.lastIndex)
        _stepIndex.value = nextIndex
        _currentStep.value = steps[nextIndex]
    }

    fun finish() {
        viewModelScope.launch { appPreferences.setOnboardingCompleted(true) }
    }
}
