package com.niagaraclone.launcher.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.data.model.IconShape
import com.niagaraclone.launcher.data.model.IconStyle
import com.niagaraclone.launcher.data.model.IconThemingMode
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.ui.theme.StarYellow
import com.niagaraclone.launcher.ui.theme.TextPrimary
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRowItem(
    app: AppInfo,
    settings: LauncherSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFavoriteStar: Boolean = false,
    enableDragReorder: Boolean = false,
    onDragStart: (() -> Unit)? = null,
    onDrag: ((dragAmountY: Float) -> Unit)? = null,
    onDragEnd: ((hasDragged: Boolean) -> Unit)? = null
) {
    val rowModifier = if (enableDragReorder && onDrag != null) {
        var hasDragged by remember { mutableStateOf(false) }
        var accumulatedY by remember { mutableFloatStateOf(0f) }

        modifier
            .fillMaxWidth()
            .height(settings.rowHeightDp.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .pointerInput(app.uniqueKey) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        hasDragged = false
                        accumulatedY = 0f
                        onDragStart?.invoke()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedY += dragAmount.y
                        if (abs(accumulatedY) > 8f) {
                            hasDragged = true
                        }
                        onDrag(dragAmount.y)
                    },
                    onDragEnd = {
                        if (!hasDragged) {
                            onLongClick()
                        }
                        onDragEnd?.invoke(hasDragged)
                        hasDragged = false
                        accumulatedY = 0f
                    },
                    onDragCancel = {
                        onDragEnd?.invoke(hasDragged)
                        hasDragged = false
                        accumulatedY = 0f
                    }
                )
            }
            .padding(
                start = settings.edgePaddingDp.dp,
                end = (settings.edgePaddingDp + 16).dp
            )
    } else {
        modifier
            .fillMaxWidth()
            .height(settings.rowHeightDp.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(
                start = settings.edgePaddingDp.dp,
                end = (settings.edgePaddingDp + 16).dp
            )
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon rendered using theme settings
        ThemedAppIcon(
            drawable = app.icon,
            settings = settings,
            modifier = Modifier.size(settings.iconSizeDp.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        // App Label
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = settings.textSizeSp.sp,
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Optional favorite star icon
        if (showFavoriteStar && app.isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Favorite",
                tint = StarYellow,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
        }
    }
}

@Composable
fun ThemedAppIcon(
    drawable: Drawable?,
    settings: LauncherSettings,
    modifier: Modifier = Modifier
) {
    val themeAccent = Color(settings.activeAccentHex)
    val themeAccentLight = Color(settings.activeAccentLightHex)

    when (settings.iconStyle) {
        IconStyle.MINIMAL_DOTS -> {
            // Sleek glowing minimalist Niagara dot
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(themeAccentLight, CircleShape)
                )
            }
        }

        IconStyle.UNIFIED_THEMED, IconStyle.THEMED_MONOCHROME, IconStyle.THEMED_DUOTONE -> {
            val isDuotone = settings.iconStyle == IconStyle.THEMED_DUOTONE
            val isUnified = settings.iconStyle == IconStyle.UNIFIED_THEMED
            val containerShape = when (settings.iconShape) {
                IconShape.CIRCLE -> CircleShape
                IconShape.SQUIRCLE -> RoundedCornerShape(30)
                IconShape.BARE -> CircleShape
            }

            // UNIFIED_THEMED and THEMED_MONOCHROME are strictly BARE glyphs with no container plate or border.
            // Only THEMED_DUOTONE draws a background shape plate when iconShape != BARE.
            val hasContainerPlate = settings.iconShape != IconShape.BARE && isDuotone

            val containerModifier = if (hasContainerPlate) {
                modifier
                    .background(
                        color = themeAccent.copy(alpha = 0.38f),
                        shape = containerShape
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.45f),
                        shape = containerShape
                    )
                    .clip(containerShape)
                    .padding(4.dp)
            } else {
                modifier
            }

            if (drawable != null) {
                Canvas(modifier = containerModifier) {
                    drawIntoCanvas { canvas ->
                        val width = size.width.roundToInt()
                        val height = size.height.roundToInt()
                        val glyphColorInt = if (isDuotone) 0xFFFFFFFF.toInt() else settings.activeAccentLightHex.toInt()

                        val hasMonochromeStencil = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            drawable is AdaptiveIconDrawable &&
                            drawable.monochrome != null

                        when (settings.iconThemingMode) {
                            IconThemingMode.GENTLE_SMART -> {
                                if (hasMonochromeStencil) {
                                    // Developer-provided Android 13+ single-color alpha stencil: safe to flat-tint
                                    val mono = (drawable as AdaptiveIconDrawable).monochrome!!.mutate()
                                    mono.setBounds(0, 0, width, height)
                                    mono.colorFilter = PorterDuffColorFilter(
                                        glyphColorInt,
                                        PorterDuff.Mode.SRC_IN
                                    )
                                    mono.draw(canvas.nativeCanvas)
                                    mono.colorFilter = null
                                } else if (drawable is AdaptiveIconDrawable) {
                                    // GENTLE CRITERIA: Keep original foreground details, shapes and contrast intact!
                                    // NEVER flat-tint multi-color app icons with PorterDuff.Mode.SRC_IN!
                                    val fg = (drawable.foreground ?: drawable).mutate()
                                    fg.setBounds(0, 0, width, height)
                                    fg.draw(canvas.nativeCanvas)
                                } else {
                                    val icon = drawable.mutate()
                                    icon.setBounds(0, 0, width, height)
                                    icon.draw(canvas.nativeCanvas)
                                }
                            }

                            IconThemingMode.SUBTLE_TONAL -> {
                                if (hasMonochromeStencil) {
                                    val mono = (drawable as AdaptiveIconDrawable).monochrome!!.mutate()
                                    mono.setBounds(0, 0, width, height)
                                    mono.colorFilter = PorterDuffColorFilter(
                                        glyphColorInt,
                                        PorterDuff.Mode.SRC_IN
                                    )
                                    mono.draw(canvas.nativeCanvas)
                                    mono.colorFilter = null
                                } else {
                                    // Subtle desaturation matrix preserving internal shapes & contrast
                                    val matrix = ColorMatrix()
                                    matrix.setSaturation(0.65f)
                                    val icon = (if (drawable is AdaptiveIconDrawable) drawable.foreground ?: drawable else drawable).mutate()
                                    icon.setBounds(0, 0, width, height)
                                    icon.colorFilter = ColorMatrixColorFilter(matrix)
                                    icon.draw(canvas.nativeCanvas)
                                    icon.colorFilter = null
                                }
                            }

                            IconThemingMode.FULL_MONOCHROME -> {
                                if (hasMonochromeStencil) {
                                    val mono = (drawable as AdaptiveIconDrawable).monochrome!!.mutate()
                                    mono.setBounds(0, 0, width, height)
                                    mono.colorFilter = PorterDuffColorFilter(
                                        glyphColorInt,
                                        PorterDuff.Mode.SRC_IN
                                    )
                                    mono.draw(canvas.nativeCanvas)
                                    mono.colorFilter = null
                                } else {
                                    val tint = if (isDuotone) 0xFFE0E0E0L else settings.activeAccentLightHex
                                    val r = ((tint shr 16) and 0xFF) / 255f
                                    val g = ((tint shr 8) and 0xFF) / 255f
                                    val b = (tint and 0xFF) / 255f

                                    val matrix = ColorMatrix(
                                        floatArrayOf(
                                            0.33f * r, 0.59f * r, 0.11f * r, 0f, 0f,
                                            0.33f * g, 0.59f * g, 0.11f * g, 0f, 0f,
                                            0.33f * b, 0.59f * b, 0.11f * b, 0f, 0f,
                                            0f, 0f, 0f, 1f, 0f
                                        )
                                    )
                                    val icon = (if (drawable is AdaptiveIconDrawable) drawable.foreground ?: drawable else drawable).mutate()
                                    icon.setBounds(0, 0, width, height)
                                    icon.colorFilter = ColorMatrixColorFilter(matrix)
                                    icon.draw(canvas.nativeCanvas)
                                    icon.colorFilter = null
                                }
                            }

                            IconThemingMode.ORIGINAL -> {
                                val icon = (if (drawable is AdaptiveIconDrawable) drawable.foreground ?: drawable else drawable).mutate()
                                icon.setBounds(0, 0, width, height)
                                icon.draw(canvas.nativeCanvas)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = containerModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(themeAccentLight, CircleShape)
                    )
                }
            }
        }

        IconStyle.ORIGINAL -> {
            val clipShape = when (settings.iconShape) {
                IconShape.CIRCLE -> CircleShape
                IconShape.SQUIRCLE -> RoundedCornerShape(26)
                IconShape.BARE -> null
            }
            val originalModifier = if (clipShape != null) modifier.clip(clipShape) else modifier

            if (drawable != null) {
                Canvas(modifier = originalModifier) {
                    drawIntoCanvas { canvas ->
                        drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
                        drawable.draw(canvas.nativeCanvas)
                    }
                }
            } else {
                Box(
                    modifier = originalModifier.background(Color(0xFF2C323D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "•", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun AppIcon(
    drawable: Drawable?,
    modifier: Modifier = Modifier,
    settings: LauncherSettings = LauncherSettings()
) {
    ThemedAppIcon(
        drawable = drawable,
        settings = settings,
        modifier = modifier
    )
}
