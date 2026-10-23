package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.DailyUsageSummary
import com.niagaraclone.launcher.data.ScreenTimeTracker
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyMindfulUsageSheet(
    usageSummary: DailyUsageSummary,
    settings: LauncherSettings,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val themeAccentLight = Color(settings.activeAccentLightHex)

    val hours = usageSummary.totalTimeMinutes / 60
    val mins = usageSummary.totalTimeMinutes % 60
    val timeFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1115),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x1A4E8CFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhoneAndroid,
                            contentDescription = null,
                            tint = themeAccentLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Today's Phone Usage",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Mindful screen time awareness",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Two Stat Cards: Screen Time & Unlocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Screen Time Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x14FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (usageSummary.hasUsageAccess) timeFormatted else "Permission needed",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = if (usageSummary.hasUsageAccess) 22.sp else 14.sp
                            ),
                            color = if (usageSummary.hasUsageAccess) Color.White else Color(0xFFFFA726)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Screen Time",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }
                }

                // Unlocks Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x14FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "${usageSummary.unlockCount}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Device Unlocks",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }
                }
            }

            if (!usageSummary.hasUsageAccess) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x1A4E8CFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334E8CFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Enable Usage Access",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Grant Usage Access permission so Still can display your daily screen time and help you stay intentional.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                ScreenTimeTracker.getInstance(context).openUsageAccessSettings(context)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4E8CFF)),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Grant Permission", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (usageSummary.topApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Most Used Today",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                usageSummary.topApps.forEach { detail ->
                    val appName = detail.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${detail.usageMinutes}m",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = themeAccentLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
