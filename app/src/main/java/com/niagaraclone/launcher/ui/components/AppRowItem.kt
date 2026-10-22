package com.niagaraclone.launcher.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.data.model.AppLaunchRule
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary

/**
 * Minimalist App Item for Still — Minimal Launcher.
 * Clean, distraction-free typography with optional monochrome icon.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRowItem(
    app: AppInfo,
    settings: LauncherSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showIcons = settings.showAppIcons
    val accentLight = Color(settings.activeAccentLightHex)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcons) {
            SimpleMonochromeAppIcon(
                drawable = app.icon,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        // App Label
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = settings.textSizeSp.sp,
                fontWeight = if (showIcons) FontWeight.Normal else FontWeight.Medium,
                letterSpacing = if (showIcons) 0.sp else 0.4.sp
            ),
            color = Color(settings.themeMode.textHex),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Visual indicator for active launch restriction
        if (app.launchRule != AppLaunchRule.NORMAL) {
            val tagText = when (app.launchRule) {
                AppLaunchRule.DELAYED -> "Pause"
                AppLaunchRule.SCHEDULED -> "Scheduled"
                AppLaunchRule.LIMITED -> "Limit"
                AppLaunchRule.BLOCKED -> "Blocked"
                else -> ""
            }
            if (tagText.isNotEmpty()) {
                Text(
                    text = tagText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = accentLight.copy(alpha = 0.9f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(settings.activeAccentHex).copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Clean monochrome rendering of the application icon.
 */
@Composable
fun SimpleMonochromeAppIcon(
    drawable: Drawable?,
    modifier: Modifier = Modifier
) {
    if (drawable == null) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
        )
        return
    }

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)

            val matrix = ColorMatrix().apply {
                setSaturation(0f) // Desaturate to pure monochrome
            }
            drawable.colorFilter = ColorMatrixColorFilter(matrix)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas.nativeCanvas)
            drawable.colorFilter = null
        }
    }
}
