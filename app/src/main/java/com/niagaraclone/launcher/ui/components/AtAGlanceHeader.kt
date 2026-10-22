package com.niagaraclone.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.model.FocusProfile
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.SystemStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top Header for Still — Minimal Launcher.
 * Features large digital clock, date with day of week, and optional small battery indicator.
 */
@Composable
fun AtAGlanceHeader(
    systemStatus: SystemStatus,
    settings: LauncherSettings = LauncherSettings(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textColor = Color(settings.themeMode.textHex)
    val textSecondaryColor = Color(settings.themeMode.textSecondaryHex)
    val accentLight = Color(settings.activeAccentLightHex)

    val formattedTime = remember(systemStatus.timeText, settings.clock24Hour) {
        try {
            val format = if (settings.clock24Hour) "HH:mm" else "h:mm"
            SimpleDateFormat(format, Locale.getDefault()).format(Date())
        } catch (_: Exception) {
            systemStatus.timeText.ifEmpty { "12:00" }
        }
    }

    val formattedDate = remember(systemStatus.dateText) {
        try {
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        } catch (_: Exception) {
            systemStatus.dateText.ifEmpty { "Thursday, October 22" }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // 1. Digital Clock
        if (settings.showClock) {
            Text(
                text = formattedTime,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    fontSize = settings.clockFontSizeSp.sp,
                    letterSpacing = (-1.5).sp,
                    color = textColor
                ),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    launchClock(context)
                }
            )
        }

        // 2. Date with Day of Week
        if (settings.showDate) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.2.sp
                ),
                color = textSecondaryColor,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    launchCalendar(context)
                }
            )
        }

        // 3. Optional Small Battery Indicator & Active Focus Pill
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settings.showBattery && systemStatus.batteryPercent >= 0) {
                Icon(
                    imageVector = if (systemStatus.isCharging) {
                        Icons.Outlined.BatteryChargingFull
                    } else {
                        Icons.Outlined.BatteryFull
                    },
                    contentDescription = null,
                    tint = textSecondaryColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${systemStatus.batteryPercent}%${if (systemStatus.isCharging) " • Charging" else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textSecondaryColor
                )
            }

            if (settings.activeFocusProfile != FocusProfile.DEFAULT) {
                if (settings.showBattery && systemStatus.batteryPercent >= 0) {
                    Text(
                        text = " • ",
                        color = textSecondaryColor,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "${settings.activeFocusProfile.label} Mode",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = accentLight
                )
            }
        }
    }
}

private fun launchClock(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALCULATOR)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}

private fun launchCalendar(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
