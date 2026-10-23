package com.niagaraclone.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.data.model.FocusProfile
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.delay

sealed class FrictionType {
    data class BreathingDelay(val secondsRemaining: Int) : FrictionType()
    data class BlockedByFocus(val profile: FocusProfile) : FrictionType()
}

/**
 * Distraction-Resistant Launch Barrier for Still — Minimal Launcher.
 * Intervenes before opening addictive applications, offering mindful pause or focus enforcement.
 */
@Composable
fun AppLaunchFrictionScreen(
    app: AppInfo,
    settings: LauncherSettings,
    frictionType: FrictionType,
    onProceedLaunch: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeAccent = Color(settings.activeAccentHex)
    val themeAccentLight = Color(settings.activeAccentLightHex)
    val buttonContentColor = if (themeAccent.luminance() > 0.45f) Color(0xFF121212) else Color.White

    var countdownSeconds by remember {
        mutableIntStateOf(settings.breathingDelaySeconds.coerceAtLeast(3))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breathingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Countdown timer for breathing pause
    LaunchedEffect(countdownSeconds) {
        if (frictionType is FrictionType.BreathingDelay && countdownSeconds > 0) {
            delay(1000)
            countdownSeconds -= 1
        }
    }

    BackHandler {
        onCancel()
    }

    Surface(
        color = Color(0xFF07080B),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            when (frictionType) {
                is FrictionType.BlockedByFocus -> {
                    // App is blocked by active Focus Profile
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0x22E74C3C), CircleShape)
                                .border(1.dp, Color(0x66E74C3C), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color(0xFFF5B7B1),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "${frictionType.profile.label} Mode Active",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "\"${app.label}\" is paused during your focus period to eliminate distraction and keep your attention calm.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        Button(
                            onClick = onCancel,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeAccent,
                                contentColor = buttonContentColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Return to Still",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = buttonContentColor
                            )
                        }
                    }
                }

                is FrictionType.BreathingDelay -> {
                    // Mindful Breathing Delay Barrier
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Animated Breathing Guide Disc
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(themeAccent.copy(alpha = 0.15f))
                                .border(2.dp, themeAccentLight.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.SelfImprovement,
                                    contentDescription = null,
                                    tint = themeAccentLight,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (countdownSeconds > 0) "${countdownSeconds}s" else "Breathe",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "Take a mindful pause",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp
                            ),
                            color = themeAccentLight,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Opening ${app.label}. Take a conscious breath before proceeding.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        // Actions: Continue vs Stay Present
                        val canProceed = countdownSeconds <= 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Stay Present",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (canProceed) {
                                        onProceedLaunch()
                                    }
                                },
                                enabled = canProceed,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeAccent,
                                    contentColor = buttonContentColor,
                                    disabledContainerColor = Color(0x18FFFFFF),
                                    disabledContentColor = Color(0x66FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = if (canProceed) "Open App" else "Wait (${countdownSeconds}s)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (canProceed) buttonContentColor else Color(0x88FFFFFF)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
