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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import com.niagaraclone.launcher.data.model.IconShape
import com.niagaraclone.launcher.data.model.IconStyle
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.ui.theme.StarYellow
import com.niagaraclone.launcher.ui.theme.TextPrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRowItem(
    app: AppInfo,
    settings: LauncherSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFavoriteStar: Boolean = false
) {
    Row(
        modifier = modifier
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
            ),
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

        IconStyle.THEMED_MONOCHROME, IconStyle.THEMED_DUOTONE -> {
            val isDuotone = settings.iconStyle == IconStyle.THEMED_DUOTONE
            val containerShape = when (settings.iconShape) {
                IconShape.CIRCLE -> CircleShape
                IconShape.SQUIRCLE -> RoundedCornerShape(30)
                IconShape.BARE -> CircleShape
            }

            val containerModifier = if (settings.iconShape != IconShape.BARE) {
                modifier
                    .background(
                        color = themeAccent.copy(alpha = if (isDuotone) 0.28f else 0.16f),
                        shape = containerShape
                    )
                    .border(
                        width = 1.dp,
                        color = themeAccentLight.copy(alpha = 0.35f),
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

                        // Android 13+ (API 33+) native monochrome layer if available
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            drawable is AdaptiveIconDrawable &&
                            drawable.monochrome != null
                        ) {
                            val mono = drawable.monochrome!!.mutate()
                            mono.setBounds(0, 0, width, height)
                            mono.colorFilter = PorterDuffColorFilter(
                                settings.activeAccentLightHex.toInt(),
                                PorterDuff.Mode.SRC_IN
                            )
                            mono.draw(canvas.nativeCanvas)
                            mono.colorFilter = null
                        } else {
                            // High-quality desaturation + theme accent multiplier
                            val tint = settings.activeAccentLightHex
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
                            drawable.setBounds(0, 0, width, height)
                            drawable.colorFilter = ColorMatrixColorFilter(matrix)
                            drawable.draw(canvas.nativeCanvas)
                            drawable.colorFilter = null
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
