package com.shomerapp.alerts.ui.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Standard banner ad. Placement is restricted by design (user decision, deviates from the
 * original spec's "no third-party SDK" rule §7.1.E — see README.md "פרסומות"):
 * only on MainScreen / HistoryScreen / SettingsScreen. NEVER on AlertActivity or during the
 * mandatory onboarding steps (permissions, sound test) — an ad must never be able to delay
 * someone reaching a shelter or skipping a safety-critical setup step.
 *
 * Ad unit id below is Google's public TEST banner id — replace with the real AdMob ad unit
 * id before release (README.md).
 */
private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = TEST_BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
