package com.shomerapp.alerts.audio

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** §5: "אמת את תקינות ה-URI בכל עליית האפליקציה" — a persisted SAF/ringtone URI can go dead if
 *  the underlying file was deleted or moved after the user picked it. */
@Singleton
class SoundUriValidator @Inject constructor(@ApplicationContext private val context: Context) {
    fun isValid(uriString: String): Boolean = runCatching {
        context.contentResolver.openInputStream(Uri.parse(uriString))?.use { true } ?: false
    }.getOrDefault(false)
}
