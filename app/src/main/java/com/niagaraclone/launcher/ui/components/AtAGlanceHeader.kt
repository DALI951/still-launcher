package com.niagaraclone.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.model.SystemStatus
import com.niagaraclone.launcher.ui.theme.NiagaraBlueLight
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary

@Composable
fun AtAGlanceHeader(
    systemStatus: SystemStatus,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Minimalist Large Clock
        Text(
            text = systemStatus.timeText.ifEmpty { "--:--" },
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 58.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1.5).sp
            ),
            color = TextPrimary,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    launchClock(context)
                }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Date Line
        Text(
            text = systemStatus.dateText.ifEmpty { "Today" },
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp
            ),
            color = TextSecondary,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    launchCalendar(context)
                }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Row: Battery, Next Alarm, Weather
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Battery Status
            if (systemStatus.batteryLevel >= 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (systemStatus.isCharging) {
                            Icons.Outlined.BatteryChargingFull
                        } else {
                            Icons.Outlined.BatteryFull
                        },
                        contentDescription = "Battery",
                        tint = if (systemStatus.isCharging) NiagaraBlueLight else TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${systemStatus.batteryLevel}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = if (systemStatus.isCharging) NiagaraBlueLight else TextMuted
                    )
                }
            }

            // Next Alarm Status
            if (!systemStatus.nextAlarm.isNullOrEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        launchClock(context)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessAlarm,
                        contentDescription = "Alarm",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = systemStatus.nextAlarm,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = TextMuted
                    )
                }
            }

            // Weather pill if present
            if (!systemStatus.weatherSummary.isNullOrEmpty()) {
                Text(
                    text = systemStatus.weatherSummary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = TextMuted
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
    } catch (e: Exception) {
        Log.w("AtAGlanceHeader", "Failed to launch alarm activity, fallback to clock", e)
        try {
            val fallback = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage("com.google.android.deskclock")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (ignored: Exception) {}
    }
}

private fun launchCalendar(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w("AtAGlanceHeader", "Failed to launch calendar", e)
    }
}
