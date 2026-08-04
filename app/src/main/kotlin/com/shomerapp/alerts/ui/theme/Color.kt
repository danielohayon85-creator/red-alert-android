package com.shomerapp.alerts.ui.theme

import androidx.compose.ui.graphics.Color

// Core dark-first palette (§8: most alerts happen at night, a bright screen slows reaction time)
val BackgroundDark = Color(0xFF0B0F14)
val SurfaceDark = Color(0xFF141B24)
val SurfaceVariantDark = Color(0xFF1E2733)
val OnSurfaceDark = Color(0xFFEAEFF5)
val OnSurfaceMutedDark = Color(0xFFA3AEBB)

val AmberPrimary = Color(0xFFF5A623)
val OnAmberPrimary = Color(0xFF201400)

// Status semantics — reused across status card, alert screens, history rows.
// Deliberately distinct from the official Home Front Command visual identity (§7.1.D).
val StatusActiveGreen = Color(0xFF3DDC84)
// Deliberately NOT AmberPrimary — that color means "tap me" on every button app-wide;
// reusing it here would make a passive network-status word look like an interactive control.
val StatusConnectingBlue = Color(0xFF5B9CF5)
val StatusInactiveRed = Color(0xFFE5484D)
val StatusAdvisoryAmber = Color(0xFFC77800)

val AlertImmediateRed = Color(0xFFB3261E)
val AlertPrewarningAmber = Color(0xFFC77800)
val AlertClearGreen = Color(0xFF2E7D32)
