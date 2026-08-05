package com.shomerapp.alerts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shomerapp.alerts.service.AlertForegroundService
import com.shomerapp.alerts.ui.RootViewModel
import com.shomerapp.alerts.ui.navigation.AzakonNavHost
import com.shomerapp.alerts.ui.onboarding.OnboardingScreen
import com.shomerapp.alerts.ui.theme.AzakonTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keeps the native splash up through the first DataStore read instead of dismissing into
        // a blank frame — mutated from inside the composable below, read by the system each frame.
        var keepSplashOn = true
        splashScreen.setKeepOnScreenCondition { keepSplashOn }

        setContent {
            AzakonTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val rootViewModel: RootViewModel = hiltViewModel()
                    val onboardingCompleted by rootViewModel.onboardingCompleted.collectAsStateWithLifecycle()
                    keepSplashOn = onboardingCompleted == null
                    val context = LocalContext.current

                    // Temporary diagnostic aid (ShomerApplication.installCrashLogger) — surfaces
                    // the previous run's crash immediately on relaunch instead of requiring a dig
                    // through Settings → Diagnostics, since that's the moment it's actually useful.
                    val crashLogFile = remember { File(context.filesDir, ShomerApplication.CRASH_LOG_FILE_NAME) }
                    var crashLog by remember { mutableStateOf(crashLogFile.takeIf { it.exists() }?.readText()) }
                    val clipboard = LocalClipboardManager.current
                    crashLog?.let { log ->
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text(stringResource(R.string.crash_dialog_title)) },
                            text = { Text(log.take(1200)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(log))
                                    crashLogFile.delete()
                                    crashLog = null
                                }) { Text(stringResource(R.string.crash_dialog_copy_and_close)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { crashLogFile.delete(); crashLog = null }) { Text(stringResource(R.string.crash_dialog_close)) }
                            },
                        )
                    }

                    when (onboardingCompleted) {
                        null -> Unit // still covered by the native splash — see keepSplashOn above
                        false -> OnboardingScreen(onCompleted = {})
                        true -> {
                            LaunchedEffect(Unit) {
                                // First moment the app knows onboarding is done — don't wait for a
                                // reboot (BootReceiver) or the watchdog's next 15-minute tick.
                                ContextCompat.startForegroundService(context, Intent(context, AlertForegroundService::class.java))
                            }
                            AzakonNavHost()
                        }
                    }
                }
            }
        }
    }
}
