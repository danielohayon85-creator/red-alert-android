package com.shomerapp.alerts.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * There's no Flow/callback API for "did the user grant DND access / battery exemption / FSI" —
 * the only reliable way to know is to re-check when the user comes back from Settings. Re-checks
 * on every ON_RESUME so each onboarding permission step reflects reality without the user having
 * to tap anything extra.
 */
@Composable
fun rememberPermissionState(check: () -> Boolean): State<Boolean> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(check()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.value = check()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}
