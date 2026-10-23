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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.niagaraclone.launcher.data.DailyUsageSummary
import com.niagaraclone.launcher.data.model.FocusProfile
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.SystemStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val STOIC_QUOTES = listOf(
    "You have power over your mind - not outside events. Realize this, and you will find strength." to "Marcus Aurelius",
    "We suffer more often in imagination than in reality." to "Seneca",
    "No man is free who is not master of himself." to "Epictetus",
    "Nature does not hurry, yet everything is accomplished." to "Lao Tzu",
    "Dwell on the beauty of life. Watch the stars, and see yourself running with them." to "Marcus Aurelius",
    "He who is contented is rich." to "Lao Tzu",
    "It is not that we have a short time to live, but that we waste a lot of it." to "Seneca",
    "He who fears death will never do anything worthy of a man who is alive." to "Seneca",
    "Silence is a source of great strength." to "Lao Tzu",
    "Don't explain your philosophy. Embody it." to "Epictetus",
    "Waste no more time arguing about what a good man should be. Be one." to "Marcus Aurelius",
    "Caretake your minutes, and the hours will take care of themselves." to "Earl of Chesterfield",
    "The soul becomes dyed with the color of its thoughts." to "Marcus Aurelius",
    "Wealth consists not in having great possessions, but in having few wants." to "Epictetus",
    "Simplicity, patience, compassion. These three are your greatest treasures." to "Lao Tzu",
    "The happiness of your life depends upon the quality of your thoughts." to "Marcus Aurelius",
    "Life is very short and anxious for those who forget the past, neglect the present, and fear the future." to "Seneca",
    "It is quality rather than quantity that matters." to "Seneca",
    "To be evenminded is the greatest virtue." to "Heraclitus",
    "Peace comes from within. Do not seek it without." to "Buddha",
    "The mind is everything. What you think you become." to "Buddha",
    "Adopt the pace of nature: her secret is patience." to "Ralph Waldo Emerson",
    "Live in each season as it passes; breathe the air, drink the drink, taste the fruit." to "Henry David Thoreau",
    "Begin at once to live, and count each separate day as a separate life." to "Seneca",
    "When you arise in the morning think of what a privilege it is to be alive, to think, to enjoy, to love." to "Marcus Aurelius",
    "He has the most who is most content with the least." to "Diogenes",
    "Nothing endures but change." to "Heraclitus",
    "Small disciplines repeated with consistency every day lead to great achievements." to "John C. Maxwell",
    "Knowing others is intelligence; knowing yourself is true wisdom." to "Lao Tzu",
    "Very little is needed to make a happy life; it is all within yourself." to "Marcus Aurelius"
)

/**
 * Top Header for Still — Minimal Launcher.
 * Features large digital clock, date with day of week, and optional small battery indicator.
 */
@Composable
fun AtAGlanceHeader(
    systemStatus: SystemStatus,
    settings: LauncherSettings = LauncherSettings(),
    dailyUsageSummary: DailyUsageSummary = DailyUsageSummary(),
    onOpenUsageSheet: () -> Unit = {},
    onEditIntention: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textColor = Color(settings.themeMode.textHex)
    val textSecondaryColor = Color(settings.themeMode.textSecondaryHex)
    val accentLight = Color(settings.activeAccentLightHex)

    var showIntentionDialog by remember { mutableStateOf(false) }
    var intentionInput by remember(settings.dailyIntentionText) {
        mutableStateOf(settings.dailyIntentionText)
    }

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

    val dailyQuote = remember(systemStatus.dateText) {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        STOIC_QUOTES[Math.floorMod(dayOfYear, STOIC_QUOTES.size)]
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

        // 3. Screen Time & Device Unlocks (Minimalist Phone style)
        if (settings.showScreenTimeOnHome) {
            val usageText = if (dailyUsageSummary.hasUsageAccess) {
                val h = dailyUsageSummary.totalTimeMinutes / 60
                val m = dailyUsageSummary.totalTimeMinutes % 60
                val formatted = if (h > 0) "${h}h ${m}m" else "${m}m"
                "$formatted phone time • ${dailyUsageSummary.unlockCount} unlocks"
            } else {
                "${dailyUsageSummary.unlockCount} phone unlocks today"
            }
            Text(
                text = usageText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = textSecondaryColor.copy(alpha = 0.85f),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenUsageSheet
                    )
            )
        }

        // 4. Daily Mindful Intention (tap to set intention)
        if (settings.showDailyIntention) {
            val intentionDisplay = settings.dailyIntentionText.ifBlank { "Stay present and intentional" }
            Text(
                text = "✦ $intentionDisplay",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = accentLight.copy(alpha = 0.9f),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showIntentionDialog = true }
                    )
            )
        }

        // 5. Daily Mindful Philosophical Quote
        if (settings.showDailyQuote) {
            val (quoteText, quoteAuthor) = dailyQuote
            Text(
                text = "“$quoteText” — $quoteAuthor",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 15.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = textSecondaryColor.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 5.dp)
            )
        }

        // 6. Optional Small Battery Indicator & Active Focus Pill
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

    if (showIntentionDialog) {
        AlertDialog(
            onDismissRequest = { showIntentionDialog = false },
            title = {
                Text("Today's Intention", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "What is your main focus today? Keep it calm and intentional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = intentionInput,
                        onValueChange = { intentionInput = it },
                        placeholder = { Text("e.g. Finish writing, walk outside") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditIntention(intentionInput)
                        showIntentionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentLight)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntentionDialog = false }) {
                    Text("Cancel", color = textSecondaryColor)
                }
            },
            containerColor = Color(0xFF141820)
        )
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
