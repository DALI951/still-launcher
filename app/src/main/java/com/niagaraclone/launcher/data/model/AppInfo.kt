package com.niagaraclone.launcher.data.model

import android.graphics.drawable.Drawable
import android.os.UserHandle

/**
 * Data representation of an installed launcher application.
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val componentName: String,
    val userHandle: UserHandle,
    val icon: Drawable? = null,
    val sectionLetter: Char = computeSectionLetter(label),
    val isFavorite: Boolean = false,
    val favoriteRank: Int = -1,
    val isHidden: Boolean = false,
    val launchRule: AppLaunchRule = AppLaunchRule.NORMAL,
    val uniqueKey: String = "${packageName}/${componentName}_${userHandle.hashCode()}"
) {
    val isYouTube: Boolean
        get() = packageName in listOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.google.android.apps.youtube.kids",
            "org.schabi.newpipe",
            "app.revanced.android.youtube",
            "com.vanced.android.youtube"
        )

    val isEssentialUtility: Boolean
        get() {
            val pkg = packageName.lowercase()
            val lbl = label.lowercase()
            return pkg.contains("dialer") || pkg.contains("phone") || pkg.contains("contacts") ||
                    pkg.contains("message") || pkg.contains("mms") || pkg.contains("sms") ||
                    pkg.contains("telecom") || pkg.contains("emergency") ||
                    pkg.contains("deskclock") || pkg.contains("clock") ||
                    pkg.contains("settings") || lbl == "phone" || lbl == "messages" ||
                    lbl == "clock" || lbl == "settings"
        }

    companion object {
        fun computeSectionLetter(label: String): Char {
            val firstChar = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
            return if (firstChar in 'A'..'Z') firstChar else '#'
        }
    }
}
