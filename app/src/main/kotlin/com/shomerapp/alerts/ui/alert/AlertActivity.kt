package com.shomerapp.alerts.ui.alert

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.shomerapp.alerts.domain.AlertSessionManager
import com.shomerapp.alerts.domain.model.AlertSessionState
import com.shomerapp.alerts.ui.theme.AzakonTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pure observer of [AlertViewModel]/[AlertSessionManager]'s shared state — deliberately reads NO
 * intent extras. That's what makes the §4.1 "seamless PREWARNING -> IMMEDIATE transition"
 * trivial: [onNewIntent] only needs to bring this singleTop activity back to the foreground; the
 * content it shows is whatever the singleton session state already is, which the
 * notification/service updated before launching this activity.
 */
@AndroidEntryPoint
class AlertActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: AlertSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        setContent {
            AzakonTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val viewModel: AlertViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    AlertScreen(uiState = uiState, onConfirmSafe = viewModel::onConfirmedSafe, onAcknowledge = viewModel::onAcknowledge)
                }
            }
        }

        finishWhenIdle()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // No further action needed — see class doc. The screen content already follows the
        // shared session state reactively.
    }

    private fun finishWhenIdle() {
        lifecycleScope.launch {
            sessionManager.state.collectLatest { state ->
                if (state is AlertSessionState.Idle) finish()
            }
        }
    }

    private fun showOverLockScreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }
}
