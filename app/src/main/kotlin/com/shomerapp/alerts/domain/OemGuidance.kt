package com.shomerapp.alerts.domain

data class OemInstruction(val manufacturerLabel: String, val steps: List<String>)

/**
 * §6/§8: OEM battery managers killing background apps is "הסיבה מספר 1 שאפליקציות כאלה נכשלות
 * בשטח" — this only returns non-null for the manufacturers with a known aggressive killer, so
 * the onboarding step can skip itself entirely for everyone else. Pure/no Android dependency —
 * takes [android.os.Build.MANUFACTURER] as a plain string so it's unit-testable.
 */
object OemGuidance {
    fun instructionsFor(manufacturer: String): OemInstruction? {
        val m = manufacturer.lowercase()
        return when {
            "xiaomi" in m -> OemInstruction(
                "Xiaomi",
                listOf(
                    "הגדרות > אפליקציות > אזעקון > הרשאות > הפעל \"הפעלה אוטומטית\" (Autostart)",
                    "וודא שהאפליקציה לא מוגדרת ב\"חיסכון בסוללה\" כ־\"מוגבל\"",
                ),
            )
            "samsung" in m -> OemInstruction(
                "Samsung",
                listOf(
                    "הגדרות > טיפול בסוללה ומכשיר > סוללה > מגבלות שימוש > הסר את אזעקון מ\"אפליקציות שלא בשימוש\"",
                    "וודא שאזעקון לא ברשימת \"אפליקציות ישנות\" (Sleeping apps)",
                ),
            )
            "huawei" in m || "honor" in m -> OemInstruction(
                "Huawei",
                listOf("הגדרות > סוללה > הפעלה אוטומטית של אפליקציות > נהל ידנית > הפעל את כל האפשרויות עבור אזעקון"),
            )
            "oneplus" in m -> OemInstruction(
                "OnePlus",
                listOf("הגדרות > סוללה > אופטימיזציית סוללה > אזעקון > \"אל תבצע אופטימיזציה\""),
            )
            "oppo" in m -> OemInstruction(
                "Oppo",
                listOf("הגדרות > ניהול סוללה > אזעקון > אפשר פעילות ברקע"),
            )
            else -> null
        }
    }
}
