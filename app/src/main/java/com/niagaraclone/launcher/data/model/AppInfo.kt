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
    val uniqueKey: String = "${packageName}/${componentName}_${userHandle.hashCode()}"
) {
    companion object {
        fun computeSectionLetter(label: String): Char {
            val firstChar = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
            return if (firstChar in 'A'..'Z') firstChar else '#'
        }
    }
}
