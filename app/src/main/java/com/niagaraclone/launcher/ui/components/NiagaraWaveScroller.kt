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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.niagaraclone.launcher.data.model.ThemePreset
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Implements Niagara Launcher's signature wave alphabet scrollbar.
 *
 * Uses a raised-cosine wave distortion formula:
 * Δx_i = -A_max * ((1 + cos(π * d_i)) / 2) * waveProgress
 * Δy_i = sign(y_i - y_touch) * S_max * sin(π * (1 - d_i)) * waveProgress
 * Scale_i = 1.0 + 0.55 * (1 - d_i)^2 * waveProgress
 */
@Composable
fun NiagaraWaveScroller(
    symbols: List<Char>,
    availableSymbols: Set<Char>,
    onSymbolSelected: (Char) -> Unit,
    onScrubbingStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    themePreset: ThemePreset = ThemePreset.NIAGARA_BLUE,
    isLeftSide: Boolean = false
) {
    val view = LocalView.current
    val density = LocalDensity.current

    val themeAccent = Color(themePreset.accentHex)
    val themeAccentLight = Color(themePreset.accentLightHex)

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

    val maxAmplitudePx = with(density) { 68.dp.toPx() }
    val maxSpreadPx = with(density) { 14.dp.toPx() }
    val waveRadiusPx = with(density) { 140.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(52.dp)
            .pointerInput(symbols) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isTouching = true
                    touchY = down.position.y
                    onScrubbingStateChanged(true)

                    val itemHeight = size.height.toFloat() / symbols.size.coerceAtLeast(1)
                    val initialIndex = (touchY / itemHeight)
                        .toInt()
                        .coerceIn(0, symbols.lastIndex)

                    if (initialIndex != activeIndex) {
                        activeIndex = initialIndex
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onSymbolSelected(symbols[initialIndex])
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            break
                        }

                        touchY = change.position.y
                        val newIndex = (touchY / itemHeight)
                            .toInt()
                            .coerceIn(0, symbols.lastIndex)

                        if (newIndex != activeIndex) {
                            activeIndex = newIndex
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSymbolSelected(symbols[newIndex])
                        }
                        change.consume()
                    }

                    isTouching = false
                    activeIndex = -1
                    onScrubbingStateChanged(false)
                }
            }
    ) {
        val totalHeight = constraints.maxHeight.toFloat()
        val numSymbols = symbols.size.coerceAtLeast(1)
        val symbolSpacing = totalHeight / numSymbols

        // Floating letter preview bubble appearing to the side of user's thumb
        AnimatedVisibility(
            visible = isTouching && activeIndex in symbols.indices,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(if (isLeftSide) Alignment.TopStart else Alignment.TopEnd)
        ) {
            if (activeIndex in symbols.indices) {
                val bubbleOffsetY = (touchY - with(density) { 32.dp.toPx() })
                    .coerceIn(0f, totalHeight - with(density) { 64.dp.toPx() })

                val bubbleOffsetX = if (isLeftSide) {
                    with(density) { 96.dp.roundToPx() }
                } else {
                    -with(density) { 96.dp.roundToPx() }
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(bubbleOffsetX, bubbleOffsetY.roundToInt()) }
                        .size(64.dp)
                        .shadow(12.dp, CircleShape)
                        .background(themeAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbols[activeIndex].toString(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Render each symbol in the vertical wave
        symbols.forEachIndexed { i, symbol ->
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
            val hasApps = symbol == '★' || symbol in availableSymbols

            val textColor = when {
                isCurrent -> Color.White
                waveProgress > 0.1f && d_i < 0.6f -> if (hasApps) themeAccentLight else TextPrimary
                hasApps -> TextPrimary
                else -> TextMuted
            }

            val baseOffsetY = i * symbolSpacing
            val finalOffsetY = baseOffsetY + deltaY

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = deltaX.roundToInt(),
                            y = finalOffsetY.roundToInt()
                        )
                    }
                    .size(width = 36.dp, height = with(density) { symbolSpacing.toDp() })
                    .align(if (isLeftSide) Alignment.TopStart else Alignment.TopEnd)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol.toString(),
                    color = textColor,
                    fontSize = if (symbol == '★') 11.sp else 10.sp,
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
