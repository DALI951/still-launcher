package com.niagaraclone.launcher.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.AssetWallpaperLoader
import com.niagaraclone.launcher.data.model.ClockStyle
import com.niagaraclone.launcher.data.model.IconShape
import com.niagaraclone.launcher.data.model.IconStyle
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.WallpaperTheme
import com.niagaraclone.launcher.data.model.WaveVerticalPosition
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary

/**
 * Live Interactive Sandbox / Mini-Phone Preview.
 * Renders a realistic miniature phone frame that reflects all pending settings tweaks in real time.
 */
@Composable
fun LiveSandboxPreview(
    settings: LauncherSettings,
    modifier: Modifier = Modifier,
    customBitmap: Bitmap? = null
) {
    val context = LocalContext.current
    val themeAccent = Color(settings.activeAccentHex)
    val themeAccentLight = Color(settings.activeAccentLightHex)

    val assetBitmap = remember(settings.wallpaperTheme, settings.wallpaperTheme.assetFileName) {
        settings.wallpaperTheme.assetFileName?.let { fileName ->
            AssetWallpaperLoader.loadBitmap(context, fileName)
        }
    }

    // Outer Bezel Frame (Miniature Phone)
    Box(
        modifier = modifier
            .width(172.dp)
            .height(344.dp)
            .shadow(16.dp, RoundedCornerShape(26.dp))
            .border(2.5.dp, Color(0xFF2E313C), RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(Color.Black)
    ) {
        // Screen Wallpaper & Background
        if (settings.wallpaperTheme == WallpaperTheme.CUSTOM_PHOTO && customBitmap != null) {
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.scrimOpacityPercent / 100f))
            )
        } else if (settings.liveWallpaperEnabled && settings.wallpaperTheme.liveVideoFileName != null) {
            // Live Video Wallpaper in miniature phone preview
            LiveVideoWallpaperView(
                videoAssetFileName = settings.wallpaperTheme.liveVideoFileName!!,
                scrimAlpha = settings.scrimOpacityPercent / 100f,
                modifier = Modifier.fillMaxSize()
            )
        } else if (assetBitmap != null) {
            Image(
                bitmap = assetBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.scrimOpacityPercent / 100f))
            )
        } else if (settings.wallpaperTheme == WallpaperTheme.SYSTEM_WALLPAPER) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(settings.themePreset.scrimBaseHex).copy(alpha = settings.scrimOpacityPercent / 100f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(settings.wallpaperTheme.topColorHex),
                                Color(settings.wallpaperTheme.bottomColorHex)
                            )
                        )
                    )
            )
            if (settings.scrimOpacityPercent > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = (settings.scrimOpacityPercent / 100f) * 0.4f))
                )
            }
        }

        // Live Ambient Motion Layer in mini-phone preview (only if not a live video)
        if (settings.liveWallpaperEnabled && settings.wallpaperTheme != WallpaperTheme.SYSTEM_WALLPAPER && settings.wallpaperTheme.liveVideoFileName == null) {
            LiveAmbientWallpaper(
                settings = settings,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Speaker Pill & Camera Notch
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .size(width = 38.dp, height = 4.dp)
                .background(Color(0xFF383B46), CircleShape)
        )

        // Main Screen Content: Clock + Mock Favorites Apps
        val scaledMargin = (settings.edgePaddingDp * 0.32f).coerceIn(8f, 20f).dp
        val topOffset = (settings.favoritesTopOffsetDp * 0.28f).coerceIn(4f, 32f).dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (settings.waveOnLeftSide) 20.dp else scaledMargin,
                    end = if (settings.waveOnLeftSide) scaledMargin else 20.dp,
                    top = 18.dp + topOffset,
                    bottom = 12.dp
                )
        ) {
            // Live Preview Clock
            if (settings.showClock) {
                val previewClockSize = (settings.clockFontSizeSp * 0.40f).coerceIn(14f, 28f)
                val (fontFamily, fontWeight, letterSpacing) = when (settings.clockStyle) {
                    ClockStyle.NIAGARA_CLEAN -> Triple(FontFamily.SansSerif, FontWeight.Light, (-0.8).sp)
                    ClockStyle.ROUNDED_MINIMAL -> Triple(FontFamily.SansSerif, FontWeight.Normal, (-0.3).sp)
                    ClockStyle.MODERN_MONO -> Triple(FontFamily.Monospace, FontWeight.Medium, (-0.5).sp)
                    ClockStyle.SERIF_ELEGANT -> Triple(FontFamily.Serif, FontWeight.Light, (-0.3).sp)
                    ClockStyle.THIN_AESTHETIC -> Triple(FontFamily.SansSerif, FontWeight.ExtraLight, (-1.0).sp)
                    ClockStyle.BOLD_COMPACT -> Triple(FontFamily.SansSerif, FontWeight.Bold, (-0.8).sp)
                    ClockStyle.STACKED_MINIMAL -> Triple(FontFamily.SansSerif, FontWeight.Bold, (-1.0).sp)
                    ClockStyle.CLEAN_CONDENSED -> Triple(FontFamily.SansSerif, FontWeight.Light, (-1.5).sp)
                }

                if (settings.clockStyle == ClockStyle.STACKED_MINIMAL) {
                    Text(
                        text = "10",
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = previewClockSize.sp,
                            letterSpacing = letterSpacing,
                            lineHeight = (previewClockSize * 0.95f).sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "42",
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = previewClockSize.sp,
                            letterSpacing = letterSpacing,
                            lineHeight = (previewClockSize * 0.95f).sp,
                            color = themeAccentLight
                        )
                    )
                } else {
                    Text(
                        text = "10:42",
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontWeight = fontWeight,
                            fontSize = previewClockSize.sp,
                            letterSpacing = letterSpacing,
                            color = TextPrimary
                        )
                    )
                }
            }

            // Date Line
            if (settings.showDate) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Wed, Oct 22 • 22°C",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 7.5.sp,
                        color = TextSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Mock Favorites App Rows
            val mockApps = listOf(
                Pair("Phone", Icons.Outlined.Phone),
                Pair("Messages", Icons.Outlined.Chat),
                Pair("Camera", Icons.Outlined.PhotoCamera),
                Pair("Browser", Icons.Outlined.Language)
            )

            mockApps.forEach { (name, icon) ->
                MockAppRow(
                    name = name,
                    icon = icon,
                    settings = settings,
                    themeAccent = themeAccent,
                    themeAccentLight = themeAccentLight
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Miniature Alphabet Wave Preview on edge
        val waveFraction = (settings.waveHeightPercent / 100f).coerceIn(0.4f, 0.95f)
        val waveAlignment = when (settings.waveVerticalPosition) {
            WaveVerticalPosition.TOP -> Alignment.TopCenter
            WaveVerticalPosition.BOTTOM -> Alignment.BottomCenter
            WaveVerticalPosition.CENTER -> Alignment.Center
        }

        Box(
            modifier = Modifier
                .align(if (settings.waveOnLeftSide) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .width(16.dp)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(waveFraction)
                    .align(waveAlignment)
                    .width(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                listOf("★", "A", "E", "M", "S", "Z", "⚙").forEach { sym ->
                    Text(
                        text = sym,
                        fontSize = 5.sp,
                        fontWeight = if (sym == "★" || sym == "⚙") FontWeight.Bold else FontWeight.Normal,
                        color = if (sym == "★" || sym == "⚙") themeAccentLight else Color(0x88FFFFFF)
                    )
                }
            }
        }
    }
}

@Composable
private fun MockAppRow(
    name: String,
    icon: ImageVector,
    settings: LauncherSettings,
    themeAccent: Color,
    themeAccentLight: Color
) {
    val miniIconSize = (settings.iconSizeDp * 0.52f).coerceIn(12f, 20f).dp
    val miniIconShape = when (settings.iconShape) {
        IconShape.CIRCLE -> CircleShape
        IconShape.SQUIRCLE -> RoundedCornerShape(28)
        IconShape.BARE -> RoundedCornerShape(0.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // UNIFIED_THEMED and THEMED_MONOCHROME are strictly bare glyphs with no container plate or border.
        // Only THEMED_DUOTONE draws an enclosing shape plate when iconShape != BARE.
        val hasPlate = settings.iconShape != IconShape.BARE && settings.iconStyle == IconStyle.THEMED_DUOTONE

        Box(
            modifier = Modifier
                .size(miniIconSize)
                .then(
                    if (hasPlate) {
                        Modifier
                            .clip(miniIconShape)
                            .background(themeAccent.copy(alpha = 0.38f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.40f), miniIconShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (settings.iconStyle == IconStyle.MINIMAL_DOTS) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(themeAccentLight, CircleShape)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when (settings.iconStyle) {
                        IconStyle.UNIFIED_THEMED, IconStyle.THEMED_MONOCHROME -> themeAccentLight
                        IconStyle.THEMED_DUOTONE -> Color.White
                        IconStyle.ORIGINAL -> themeAccentLight
                        IconStyle.MINIMAL_DOTS -> themeAccentLight
                    },
                    modifier = Modifier.size(miniIconSize * 0.65f)
                )
            }
        }

        // App Label
        Text(
            text = name,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = (settings.textSizeSp * 0.52f).coerceIn(7f, 10f).sp,
                color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
