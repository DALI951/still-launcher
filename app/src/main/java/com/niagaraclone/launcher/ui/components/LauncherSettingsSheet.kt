package com.niagaraclone.launcher.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.ColorExtractor
import com.niagaraclone.launcher.data.model.ClockStyle
import com.niagaraclone.launcher.data.model.IconShape
import com.niagaraclone.launcher.data.model.IconStyle
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.ThemePreset
import com.niagaraclone.launcher.data.model.WallpaperTheme
import com.niagaraclone.launcher.data.model.WaveVerticalPosition
import com.niagaraclone.launcher.data.model.WaveVisibility
import com.niagaraclone.launcher.ui.theme.SurfaceTranslucent
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherSettingsSheet(
    settings: LauncherSettings,
    onDismissRequest: () -> Unit,
    onUpdateSettings: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onResetDefaults: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val themeAccent = Color(settings.activeAccentHex)
    val themeAccentLight = Color(settings.activeAccentLightHex)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val palette = ColorExtractor.extractAndSavePalette(context, uri)
                if (palette != null) {
                    onUpdateSettings { s ->
                        s.copy(
                            wallpaperTheme = WallpaperTheme.CUSTOM_PHOTO,
                            customPhotoPath = palette.imagePath,
                            customAccentHex = palette.accentHex,
                            customAccentLightHex = palette.accentLightHex
                        )
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SurfaceTranslucent,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Title + Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Customize Niagara",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Configure ergonomics, theme & icons",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = onResetDefaults,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    shape = CircleShape,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = "Reset",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset", fontSize = 12.sp, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Wallpaper Themes & Palette Extraction
            SectionHeader(title = "WALLPAPER THEME & SMART PALETTE")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WallpaperTheme.values().forEach { theme ->
                    val isSelected = settings.wallpaperTheme == theme
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(theme.accentHex).copy(alpha = 0.35f) else Color(0x22FFFFFF),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(theme.accentLightHex)) else null,
                        modifier = Modifier.clickable {
                            onUpdateSettings { it.copy(wallpaperTheme = theme) }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (theme == WallpaperTheme.CUSTOM_PHOTO) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = Color(theme.accentHex),
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (theme == WallpaperTheme.SYSTEM_WALLPAPER) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(theme.accentHex), CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(theme.topColorHex), Color(theme.bottomColorHex))
                                            )
                                        )
                                        .border(1.dp, Color(theme.accentHex), CircleShape)
                                )
                            }
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Choose Custom Wallpaper & Extract Palette button
            Button(
                onClick = { photoPickerLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = themeAccent.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeAccentLight.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = themeAccentLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (settings.wallpaperTheme == WallpaperTheme.CUSTOM_PHOTO && !settings.customPhotoPath.isNullOrEmpty()) {
                        "Change Custom Photo & Extract Palette"
                    } else {
                        "Choose Custom Wallpaper & Extract Palette"
                    },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (settings.wallpaperTheme == WallpaperTheme.SYSTEM_WALLPAPER) {
                Spacer(modifier = Modifier.height(14.dp))
                SectionHeader(title = "SYSTEM WALLPAPER ACCENT COLOR")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemePreset.values().forEach { preset ->
                        val isSelected = settings.themePreset == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(preset.accentHex).copy(alpha = 0.35f) else Color(0x22FFFFFF),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(preset.accentLightHex)) else null,
                            modifier = Modifier.clickable {
                                onUpdateSettings { it.copy(themePreset = preset) }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color(preset.accentHex), CircleShape)
                                )
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Background Darkness (Scrim Opacity) Slider
            SliderSetting(
                title = "Background Darkness",
                valueDisplay = "${settings.scrimOpacityPercent}%",
                value = settings.scrimOpacityPercent.toFloat(),
                valueRange = 0f..95f,
                accentColor = themeAccent,
                onValueChange = {
                    onUpdateSettings { s -> s.copy(scrimOpacityPercent = it.roundToInt()) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Icon Style Selector
            SectionHeader(title = "ICON STYLE")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconStyle.values().forEach { style ->
                    val isSelected = settings.iconStyle == style
                    ChipOption(
                        label = style.label,
                        isSelected = isSelected,
                        accentColor = themeAccent,
                        accentLight = themeAccentLight,
                        onClick = { onUpdateSettings { it.copy(iconStyle = style) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Icon Shape Selector
            SectionHeader(title = "ICON CONTAINER SHAPE")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconShape.values().forEach { shape ->
                    val isSelected = settings.iconShape == shape
                    ChipOption(
                        label = shape.label,
                        isSelected = isSelected,
                        accentColor = themeAccent,
                        accentLight = themeAccentLight,
                        onClick = { onUpdateSettings { it.copy(iconShape = shape) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Icon Size Slider (Small default: 26 dp)
            SliderSetting(
                title = "Icon Size",
                valueDisplay = "${settings.iconSizeDp} dp",
                value = settings.iconSizeDp.toFloat(),
                valueRange = 18f..42f,
                accentColor = themeAccent,
                onValueChange = {
                    onUpdateSettings { s -> s.copy(iconSizeDp = it.roundToInt()) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Edge Inset / Margin Slider (Far from edges default: 44 dp)
            SliderSetting(
                title = "Distance from Screen Edges",
                valueDisplay = "${settings.edgePaddingDp} dp",
                value = settings.edgePaddingDp.toFloat(),
                valueRange = 20f..76f,
                accentColor = themeAccent,
                onValueChange = {
                    onUpdateSettings { s -> s.copy(edgePaddingDp = it.roundToInt()) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Row Height / Spacing Slider
            SliderSetting(
                title = "Row Spacing / Height",
                valueDisplay = "${settings.rowHeightDp} dp",
                value = settings.rowHeightDp.toFloat(),
                valueRange = 36f..60f,
                accentColor = themeAccent,
                onValueChange = {
                    onUpdateSettings { s -> s.copy(rowHeightDp = it.roundToInt()) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 8. Text Size Slider
            SliderSetting(
                title = "App Label Font Size",
                valueDisplay = "${settings.textSizeSp} sp",
                value = settings.textSizeSp.toFloat(),
                valueRange = 12f..20f,
                accentColor = themeAccent,
                onValueChange = {
                    onUpdateSettings { s -> s.copy(textSizeSp = it.roundToInt()) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 9. Favorites Top Offset Slider
            SliderSetting(
                title = "Favorites Vertical Position (Top Margin)",
                valueDisplay = "${settings.favoritesTopOffsetDp} dp",
                value = settings.favoritesTopOffsetDp.toFloat(),
                valueRange = 0f..180f,
                accentColor = themeAccent,
                onValueChange = {
                    onUpdateSettings { s -> s.copy(favoritesTopOffsetDp = it.roundToInt()) }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 10. NIAGARA CLOCK & HEADER
            SectionHeader(title = "NIAGARA CLOCK & HEADER")

            ToggleRow(
                title = "Show Clock",
                checked = settings.showClock,
                accentColor = themeAccent,
                onCheckedChange = { checked -> onUpdateSettings { it.copy(showClock = checked) } }
            )

            if (settings.showClock) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Clock Typography Style",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClockStyle.values().forEach { style ->
                        val isSelected = settings.clockStyle == style
                        ChipOption(
                            label = style.label,
                            isSelected = isSelected,
                            accentColor = themeAccent,
                            accentLight = themeAccentLight,
                            onClick = { onUpdateSettings { it.copy(clockStyle = style) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SliderSetting(
                    title = "Clock Font Size",
                    valueDisplay = "${settings.clockFontSizeSp} sp",
                    value = settings.clockFontSizeSp.toFloat(),
                    valueRange = 32f..72f,
                    accentColor = themeAccent,
                    onValueChange = {
                        onUpdateSettings { s -> s.copy(clockFontSizeSp = it.roundToInt()) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ToggleRow(
                title = "Show Date",
                checked = settings.showDate,
                accentColor = themeAccent,
                onCheckedChange = { checked -> onUpdateSettings { it.copy(showDate = checked) } }
            )

            ToggleRow(
                title = "Show Battery & Alarm",
                checked = settings.showBatteryAndAlarm,
                accentColor = themeAccent,
                onCheckedChange = { checked -> onUpdateSettings { it.copy(showBatteryAndAlarm = checked) } }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 11. ALPHABET WAVE & ERGONOMICS
            SectionHeader(title = "ALPHABET WAVE & ERGONOMICS")

            Text(
                text = "Wave Visibility",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WaveVisibility.values().forEach { visibility ->
                    val isSelected = settings.waveVisibility == visibility
                    ChipOption(
                        label = visibility.label,
                        isSelected = isSelected,
                        accentColor = themeAccent,
                        accentLight = themeAccentLight,
                        onClick = { onUpdateSettings { it.copy(waveVisibility = visibility) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Wave Vertical Alignment",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WaveVerticalPosition.values().forEach { pos ->
                    val isSelected = settings.waveVerticalPosition == pos
                    ChipOption(
                        label = pos.label,
                        isSelected = isSelected,
                        accentColor = themeAccent,
                        accentLight = themeAccentLight,
                        onClick = { onUpdateSettings { it.copy(waveVerticalPosition = pos) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            SliderSetting(
                title = "Wave Vertical Height",
                valueDisplay = "${settings.waveHeightPercent}%",
                value = settings.waveHeightPercent.toFloat(),
                valueRange = 40f..90f,
                accentColor = themeAccent,
                onValueChange = {
                    onUpdateSettings { s -> s.copy(waveHeightPercent = it.roundToInt()) }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            SliderSetting(
                title = "Wave Letter Font Size",
                valueDisplay = "${settings.waveLetterSizeSp} sp",
                value = settings.waveLetterSizeSp.toFloat(),
                valueRange = 8f..14f,
                accentColor = themeAccent,
                onValueChange = {
                    onUpdateSettings { s -> s.copy(waveLetterSizeSp = it.roundToInt()) }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ToggleRow(
                title = "Left-Handed Wave Mode",
                checked = settings.waveOnLeftSide,
                accentColor = themeAccent,
                onCheckedChange = { checked -> onUpdateSettings { it.copy(waveOnLeftSide = checked) } }
            )

            ToggleRow(
                title = "Swipe Up on Home to Search",
                checked = settings.swipeUpToSearch,
                accentColor = themeAccent,
                onCheckedChange = { checked -> onUpdateSettings { it.copy(swipeUpToSearch = checked) } }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = TextMuted,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ChipOption(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    accentLight: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.35f) else Color(0x22FFFFFF),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, accentLight) else null,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) TextPrimary else TextSecondary
            )
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    valueDisplay: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary
            )
            Text(
                text = valueDisplay,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
