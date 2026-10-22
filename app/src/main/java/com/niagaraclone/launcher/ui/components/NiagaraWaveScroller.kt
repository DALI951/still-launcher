package com.niagaraclone.launcher.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.ThemePreset
import com.niagaraclone.launcher.data.model.WaveVerticalPosition
import com.niagaraclone.launcher.data.model.WaveVisibility
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Authentic Niagara Alphabet Wave Scroller with Touch Physics Overhaul.
 *
 * Provides a responsive 64.dp edge touch target with raised-cosine wave distortion:
 * - Δx_i = -A_max * ((1 + cos(π * d_i)) / 2) * waveProgress
 * - Δy_i = sign(y_i - y_touch) * S_max * sin(π * (1 - d_i)) * waveProgress
 * - Scale_i = 1.0 + 0.55 * (1 - d_i)^2 * waveProgress
 *
 * Includes '⚙' at bottom to quickly access launcher settings and full vertical alignment support.
 */
@Composable
fun NiagaraWaveScroller(
    symbols: List<Char>,
    availableSymbols: Set<Char>,
    onSymbolSelected: (Char) -> Unit,
    onScrubbingStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    settings: LauncherSettings = LauncherSettings(),
    themePreset: ThemePreset = settings.themePreset,
    isLeftSide: Boolean = settings.waveOnLeftSide,
    onOpenSettings: () -> Unit = {}
) {
    val view = LocalView.current
    val density = LocalDensity.current

    val themeAccent = Color(settings.activeAccentHex)
    val themeAccentLight = Color(settings.activeAccentLightHex)

    // Ensure '⚙' is appended at the bottom of the symbols list
    val effectiveSymbols = remember(symbols) {
        if (symbols.contains('⚙')) symbols else symbols + listOf('⚙')
    }

    var isTouching by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var activeIndex by remember { mutableIntStateOf(-1) }

    // Wave progress smoothly animates in and out when touched
    val waveProgress by animateFloatAsState(
        targetValue = if (isTouching) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "waveProgress"
    )

    // Visibility alpha depending on WaveVisibility setting
    val visibilityAlpha by animateFloatAsState(
        targetValue = when (settings.waveVisibility) {
            WaveVisibility.ALWAYS_VISIBLE -> 1f
            WaveVisibility.TOUCH_ONLY -> if (isTouching || waveProgress > 0.05f) 1f else 0f
            WaveVisibility.SUBTLE_DOTS -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "visibilityAlpha"
    )

    val maxAmplitudePx = with(density) { 68.dp.toPx() }
    val maxSpreadPx = with(density) { 14.dp.toPx() }
    val waveRadiusPx = with(density) { 140.dp.toPx() }

    val milestoneLetters = remember { setOf('★', '#', 'A', 'E', 'I', 'M', 'Q', 'U', 'Z', '⚙') }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(64.dp)
            .pointerInput(effectiveSymbols, settings.waveHeightPercent, settings.waveVerticalPosition) {
                val totalHeight = size.height.toFloat()
                val waveFraction = (settings.waveHeightPercent / 100f).coerceIn(0.4f, 0.95f)
                val waveHeight = totalHeight * waveFraction
                val waveTopOffset = when (settings.waveVerticalPosition) {
                    WaveVerticalPosition.TOP -> 0f
                    WaveVerticalPosition.BOTTOM -> totalHeight - waveHeight
                    WaveVerticalPosition.CENTER -> (totalHeight - waveHeight) / 2f
                }

                val itemHeight = waveHeight / effectiveSymbols.size.coerceAtLeast(1)

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isTouching = true
                    val clampedY = (down.position.y - waveTopOffset).coerceIn(0f, waveHeight)
                    touchY = clampedY
                    onScrubbingStateChanged(true)

                    val initialIndex = (clampedY / itemHeight)
                        .toInt()
                        .coerceIn(0, effectiveSymbols.lastIndex)

                    if (initialIndex != activeIndex) {
                        activeIndex = initialIndex
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onSymbolSelected(effectiveSymbols[initialIndex])
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            break
                        }

                        val moveY = (change.position.y - waveTopOffset).coerceIn(0f, waveHeight)
                        touchY = moveY
                        val newIndex = (moveY / itemHeight)
                            .toInt()
                            .coerceIn(0, effectiveSymbols.lastIndex)

                        if (newIndex != activeIndex) {
                            activeIndex = newIndex
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSymbolSelected(effectiveSymbols[newIndex])
                        }
                        change.consume()
                    }

                    isTouching = false
                    activeIndex = -1
                    onScrubbingStateChanged(false)
                }
            }
    ) {
        val totalAvailableHeight = constraints.maxHeight.toFloat()
        val waveFraction = (settings.waveHeightPercent / 100f).coerceIn(0.4f, 0.95f)
        val waveRenderHeight = totalAvailableHeight * waveFraction
        val waveTopOffset = when (settings.waveVerticalPosition) {
            WaveVerticalPosition.TOP -> 0f
            WaveVerticalPosition.BOTTOM -> totalAvailableHeight - waveRenderHeight
            WaveVerticalPosition.CENTER -> (totalAvailableHeight - waveRenderHeight) / 2f
        }

        val numSymbols = effectiveSymbols.size.coerceAtLeast(1)
        val symbolSpacing = waveRenderHeight / numSymbols

        // Floating frosted glass letter preview bubble appearing next to thumb
        AnimatedVisibility(
            visible = isTouching && activeIndex in effectiveSymbols.indices,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(if (isLeftSide) Alignment.TopStart else Alignment.TopEnd)
        ) {
            if (activeIndex in effectiveSymbols.indices) {
                val isSettingsSymbol = effectiveSymbols[activeIndex] == '⚙'
                val bubbleWidth = if (isSettingsSymbol) 120.dp else 56.dp
                val bubbleHeight = 56.dp
                val bubbleShape = if (isSettingsSymbol) RoundedCornerShape(18.dp) else CircleShape

                val bubbleOffsetY = (waveTopOffset + touchY - with(density) { 28.dp.toPx() })
                    .coerceIn(0f, totalAvailableHeight - with(density) { 56.dp.toPx() })

                val bubbleOffsetX = if (isLeftSide) {
                    with(density) { 68.dp.roundToPx() }
                } else {
                    -with(density) { (bubbleWidth.value + 12f).dp.roundToPx() }
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(bubbleOffsetX, bubbleOffsetY.roundToInt()) }
                        .size(width = bubbleWidth, height = bubbleHeight)
                        .shadow(16.dp, bubbleShape)
                        .background(Color(0xEE1C1E24), bubbleShape)
                        .border(1.5.dp, themeAccentLight.copy(alpha = 0.6f), bubbleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSettingsSymbol) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "⚙",
                                fontSize = 16.sp,
                                color = themeAccentLight
                            )
                            Text(
                                text = "Settings",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = effectiveSymbols[activeIndex].toString(),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Inner Wave Container rendered at waveTopOffset with waveRenderHeight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { waveRenderHeight.toDp() })
                .offset { IntOffset(0, waveTopOffset.roundToInt()) }
                .alpha(visibilityAlpha)
        ) {
            effectiveSymbols.forEachIndexed { i, symbol ->
                val symbolCenterY = (i + 0.5f) * symbolSpacing
                val dist = abs(symbolCenterY - touchY)
                val d_i = (dist / waveRadiusPx).coerceAtMost(1f)

                // Mathematical raised-cosine distortion
                val cosFactor = ((1.0 + cos(PI * d_i)) / 2.0).toFloat()
                val rawDeltaX = maxAmplitudePx * cosFactor * waveProgress
                val deltaX = if (isLeftSide) rawDeltaX else -rawDeltaX

                val sign = if (symbolCenterY >= touchY) 1f else -1f
                val deltaY = sign * maxSpreadPx * sin(PI * (1f - d_i)).toFloat() * waveProgress

                val scale = 1.0f + 0.55f * (1f - d_i) * (1f - d_i) * waveProgress
                val isCurrent = i == activeIndex
                val hasApps = symbol == '★' || symbol == '⚙' || symbol in availableSymbols

                val textColor = when {
                    isCurrent -> Color.White
                    waveProgress > 0.1f && d_i < 0.6f -> if (hasApps) themeAccentLight else TextPrimary
                    hasApps -> TextPrimary
                    else -> TextMuted
                }

                val baseOffsetY = i * symbolSpacing
                val finalOffsetY = baseOffsetY + deltaY

                val isMilestone = symbol in milestoneLetters
                val isDotsMode = settings.waveVisibility == WaveVisibility.SUBTLE_DOTS && !isTouching

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = deltaX.roundToInt(),
                                y = finalOffsetY.roundToInt()
                            )
                        }
                        .size(width = 44.dp, height = with(density) { symbolSpacing.toDp() })
                        .align(if (isLeftSide) Alignment.TopStart else Alignment.TopEnd)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDotsMode) {
                        if (isMilestone) {
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp)
                                    .background(
                                        if (hasApps) themeAccentLight else TextMuted.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                            )
                        }
                    } else {
                        Text(
                            text = symbol.toString(),
                            color = textColor,
                            fontSize = when (symbol) {
                                '★' -> (settings.waveLetterSizeSp + 1).sp
                                '⚙' -> (settings.waveLetterSizeSp).sp
                                else -> settings.waveLetterSizeSp.sp
                            },
                            fontWeight = if (isCurrent || hasApps) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(
                                when {
                                    isCurrent -> 1f
                                    hasApps -> (0.8f + 0.2f * (1f - d_i))
                                    else -> 0.35f
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
