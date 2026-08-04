package com.shomerapp.alerts.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.shomerapp.alerts.R

/**
 * Bottom-nav destinations. Onboarding is intentionally not part of this set — it's a
 * one-time flow launched separately from [com.shomerapp.alerts.MainActivity], not a tab.
 */
sealed class Screen(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    data object Main : Screen("main", R.string.nav_main, Icons.Filled.Home)
    data object History : Screen("history", R.string.nav_history, Icons.Filled.History)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings)

    companion object {
        val bottomNavItems = listOf(Main, History, Settings)
    }
}
