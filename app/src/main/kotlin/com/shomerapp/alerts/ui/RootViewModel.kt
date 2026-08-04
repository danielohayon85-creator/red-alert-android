package com.shomerapp.alerts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shomerapp.alerts.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(appPreferences: AppPreferences) : ViewModel() {
    /** Null = "not loaded yet" (avoids flashing onboarding before DataStore's first read completes). */
    val onboardingCompleted = appPreferences.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
