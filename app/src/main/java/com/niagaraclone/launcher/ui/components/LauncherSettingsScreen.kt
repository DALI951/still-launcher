package com.niagaraclone.launcher.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.AssetWallpaperLoader
import com.niagaraclone.launcher.data.ColorExtractor
import com.niagaraclone.launcher.data.model.ClockStyle
import com.niagaraclone.launcher.data.model.IconShape
import com.niagaraclone.launcher.data.model.IconStyle
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.ThemePreset
import com.niagaraclone.launcher.data.model.WallpaperTheme
import com.niagaraclone.launcher.data.model.WaveVerticalPosition
import com.niagaraclone.launcher.data.model.WaveVisibility
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class AccentSwatch(val label: String, val hex: Long, val lightHex: Long) {
    OCEAN_BLUE("Ocean Blue", 0xFF4E8CFF, 0xFF99C2FF),
    AMBER_GOLD("Amber Gold", 0xFFF39C12, 0xFFFAD7A0),
    CYBER_MINT("Cyber Mint", 0xFF2ECC71, 0xFF82E0AA),
    ELECTRIC_VIOLET("Electric Violet", 0xFF9B59B6, 0xFFD2B4DE),
    CRIMSON_ROSE("Crimson Rose", 0xFFE74C3C, 0xFFF5B7B1),
    PURE_SILVER("Pure Silver", 0xFFB0B3BA, 0xFFECEFF1)
}

/**
 * Dedicated Full-Screen Settings App & Theme Studio.
 * Includes a Live Interactive Sandbox mini-phone preview that updates in real time.
 */
@Composable
fun LauncherSettingsScreen(
    settings: LauncherSettings,
    onDismissRequest: () -> Unit,
    onApplySettings: (LauncherSettings) -> Unit,
    onResetDefaults: () -> Unit,
    onRequestDefaultLauncher: () -> Unit = {},
    onOpenUpdates: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var previewSettings by remember(settings) { mutableStateOf(settings) }
    val hasUnsavedChanges = previewSettings != settings

    val previewAccent = Color(previewSettings.activeAccentHex)
    val previewAccentLight = Color(previewSettings.activeAccentLightHex)

    var previewBitmap by remember(previewSettings.customPhotoPath) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(previewSettings.customPhotoPath) {
        val path = previewSettings.customPhotoPath
        previewBitmap = if (!path.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(path)
                } catch (e: Exception) {
                    null
                }
            }
        } else {
            null
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val palette = ColorExtractor.extractAndSavePalette(context, uri)
                if (palette != null) {
                    previewSettings = previewSettings.copy(
                        wallpaperTheme = WallpaperTheme.CUSTOM_PHOTO,
                        customPhotoPath = palette.imagePath,
                        customAccentHex = palette.accentHex,
                        customAccentLightHex = palette.accentLightHex
                    )
                }
            }
        }
    }

    BackHandler {
        onDismissRequest()
    }

    val tabs = listOf("Themes", "Icons", "Clock", "Wave", "About")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D11))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Column {
                        Text(
                            text = "Niagara Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Theme Studio & Ergonomics",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Apply Button in header
                Button(
                    onClick = {
                        onApplySettings(previewSettings)
                    },
                    enabled = hasUnsavedChanges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = previewAccent,
                        disabledContainerColor = Color(0x22FFFFFF)
                    ),
                    shape = CircleShape,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        tint = if (hasUnsavedChanges) Color.White else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apply",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasUnsavedChanges) Color.White else TextMuted
                    )
                }
            }

            HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 0.5.dp)

            // Pinned Top Live Sandbox Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LiveSandboxPreview(
                    settings = previewSettings,
                    customBitmap = previewBitmap
                )

                if (hasUnsavedChanges) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = { previewSettings = settings }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Undo,
                                contentDescription = null,
                                tint = previewAccentLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Discard Preview Tweaks",
                                fontSize = 12.sp,
                                color = previewAccentLight
                            )
                        }
                    }
                }
            }

            // Category Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) previewAccent.copy(alpha = 0.35f) else Color(0x22FFFFFF),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, previewAccentLight) else null,
                        modifier = Modifier.clickable { selectedTabIndex = index }
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (isSelected) TextPrimary else TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Scrollable Category Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // ==========================================
                        // TAB 1: THEME STUDIO
                        // ==========================================
                        SectionHeader("CURATED CINEMA WALLPAPERS")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WallpaperTheme.values().forEach { theme ->
                                val isSelected = previewSettings.wallpaperTheme == theme
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) Color(theme.accentHex).copy(alpha = 0.35f) else Color(0x22FFFFFF),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(theme.accentLightHex)) else null,
                                    modifier = Modifier.clickable {
                                        previewSettings = previewSettings.copy(wallpaperTheme = theme)
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (theme == WallpaperTheme.CUSTOM_PHOTO) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 54.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0x33FFFFFF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Image,
                                                    contentDescription = null,
                                                    tint = Color(theme.accentHex),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else if (theme == WallpaperTheme.SYSTEM_WALLPAPER) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 54.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(theme.accentHex)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Home,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else if (theme.assetFileName != null) {
                                            val thumb = remember(theme.assetFileName) {
                                                AssetWallpaperLoader.loadThumbnail(context, theme.assetFileName, 88, 108)
                                            }
                                            if (thumb != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 44.dp, height = 54.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(1.dp, Color(theme.accentHex).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                ) {
                                                    Image(
                                                        bitmap = thumb.asImageBitmap(),
                                                        contentDescription = theme.label,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .padding(3.dp)
                                                            .size(8.dp)
                                                            .background(Color(theme.accentHex), CircleShape)
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 44.dp, height = 54.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(Color(theme.topColorHex), Color(theme.bottomColorHex))
                                                            )
                                                        )
                                                        .border(1.dp, Color(theme.accentHex), RoundedCornerShape(8.dp))
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 54.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color(theme.topColorHex), Color(theme.bottomColorHex))
                                                        )
                                                    )
                                                    .border(1.dp, Color(theme.accentHex), RoundedCornerShape(8.dp))
                                            )
                                        }
                                        Text(
                                            text = theme.label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.sp
                                            ),
                                            color = if (isSelected) TextPrimary else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Photo Wallpaper Picker Button
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = previewAccent.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, previewAccentLight.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = previewAccentLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (previewSettings.wallpaperTheme == WallpaperTheme.CUSTOM_PHOTO && !previewSettings.customPhotoPath.isNullOrEmpty()) {
                                    "Change Photo & Re-Extract Palette"
                                } else {
                                    "Choose Custom Photo & Extract Palette"
                                },
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Accent Color Swatches
                        SectionHeader("ACCENT PALETTE OVERRIDE")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AccentSwatch.values().forEach { swatch ->
                                val isSelected = previewSettings.customAccentHex == swatch.hex
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(swatch.hex).copy(alpha = 0.35f) else Color(0x22FFFFFF),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(swatch.lightHex)) else null,
                                    modifier = Modifier.clickable {
                                        previewSettings = previewSettings.copy(
                                            customAccentHex = swatch.hex,
                                            customAccentLightHex = swatch.lightHex
                                        )
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(Color(swatch.hex), CircleShape)
                                        )
                                        Text(
                                            text = swatch.label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isSelected) TextPrimary else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Wallpaper Dimming Scrim
                        SliderSetting(
                            title = "Wallpaper Dimming (Dark Scrim)",
                            valueDisplay = "${previewSettings.scrimOpacityPercent}%",
                            value = previewSettings.scrimOpacityPercent.toFloat(),
                            valueRange = 0f..90f,
                            accentColor = previewAccent,
                            onValueChange = {
                                previewSettings = previewSettings.copy(scrimOpacityPercent = it.roundToInt())
                            }
                        )
                    }

                    1 -> {
                        // ==========================================
                        // TAB 2: ICONS & STYLE
                        // ==========================================
                        SectionHeader("ICON THEME STYLE")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconStyle.values().forEach { style ->
                                val isSelected = previewSettings.iconStyle == style
                                ChipOption(
                                    label = style.label,
                                    isSelected = isSelected,
                                    accentColor = previewAccent,
                                    accentLight = previewAccentLight,
                                    onClick = { previewSettings = previewSettings.copy(iconStyle = style) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SectionHeader("ICON CONTAINER SHAPE")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconShape.values().forEach { shape ->
                                val isSelected = previewSettings.iconShape == shape
                                ChipOption(
                                    label = shape.label,
                                    isSelected = isSelected,
                                    accentColor = previewAccent,
                                    accentLight = previewAccentLight,
                                    onClick = { previewSettings = previewSettings.copy(iconShape = shape) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        SliderSetting(
                            title = "App Icon Size",
                            valueDisplay = "${previewSettings.iconSizeDp} dp",
                            value = previewSettings.iconSizeDp.toFloat(),
                            valueRange = 18f..38f,
                            accentColor = previewAccent,
                            onValueChange = {
                                previewSettings = previewSettings.copy(iconSizeDp = it.roundToInt())
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SliderSetting(
                            title = "Screen Edge Margin",
                            valueDisplay = "${previewSettings.edgePaddingDp} dp",
                            value = previewSettings.edgePaddingDp.toFloat(),
                            valueRange = 24f..64f,
                            accentColor = previewAccent,
                            onValueChange = {
                                previewSettings = previewSettings.copy(edgePaddingDp = it.roundToInt())
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SliderSetting(
                            title = "Row Spacing / Height",
                            valueDisplay = "${previewSettings.rowHeightDp} dp",
                            value = previewSettings.rowHeightDp.toFloat(),
                            valueRange = 36f..60f,
                            accentColor = previewAccent,
                            onValueChange = {
                                previewSettings = previewSettings.copy(rowHeightDp = it.roundToInt())
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SliderSetting(
                            title = "Label Font Size",
                            valueDisplay = "${previewSettings.textSizeSp} sp",
                            value = previewSettings.textSizeSp.toFloat(),
                            valueRange = 12f..20f,
                            accentColor = previewAccent,
                            onValueChange = {
                                previewSettings = previewSettings.copy(textSizeSp = it.roundToInt())
                            }
                        )
                    }

                    2 -> {
                        // ==========================================
                        // TAB 3: CLOCK & TYPOGRAPHY
                        // ==========================================
                        ToggleRow(
                            title = "Show Minimalist Clock",
                            checked = previewSettings.showClock,
                            accentColor = previewAccent,
                            onCheckedChange = { previewSettings = previewSettings.copy(showClock = it) }
                        )

                        if (previewSettings.showClock) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionHeader("CLOCK TYPOGRAPHY STYLE")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ClockStyle.values().forEach { style ->
                                    val isSelected = previewSettings.clockStyle == style
                                    ChipOption(
                                        label = style.label,
                                        isSelected = isSelected,
                                        accentColor = previewAccent,
                                        accentLight = previewAccentLight,
                                        onClick = { previewSettings = previewSettings.copy(clockStyle = style) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SliderSetting(
                                title = "Clock Font Size",
                                valueDisplay = "${previewSettings.clockFontSizeSp} sp",
                                value = previewSettings.clockFontSizeSp.toFloat(),
                                valueRange = 32f..72f,
                                accentColor = previewAccent,
                                onValueChange = {
                                    previewSettings = previewSettings.copy(clockFontSizeSp = it.roundToInt())
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ToggleRow(
                            title = "Show Date & Weather Line",
                            checked = previewSettings.showDate,
                            accentColor = previewAccent,
                            onCheckedChange = { previewSettings = previewSettings.copy(showDate = it) }
                        )

                        ToggleRow(
                            title = "Show Battery & Alarm Status",
                            checked = previewSettings.showBatteryAndAlarm,
                            accentColor = previewAccent,
                            onCheckedChange = { previewSettings = previewSettings.copy(showBatteryAndAlarm = it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SliderSetting(
                            title = "Vertical Position Offset",
                            valueDisplay = "${previewSettings.favoritesTopOffsetDp} dp",
                            value = previewSettings.favoritesTopOffsetDp.toFloat(),
                            valueRange = 0f..180f,
                            accentColor = previewAccent,
                            onValueChange = {
                                previewSettings = previewSettings.copy(favoritesTopOffsetDp = it.roundToInt())
                            }
                        )
                    }

                    3 -> {
                        // ==========================================
                        // TAB 4: WAVE & ERGONOMICS
                        // ==========================================
                        SectionHeader("VERTICAL WAVE ALIGNMENT")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WaveVerticalPosition.values().forEach { pos ->
                                val isSelected = previewSettings.waveVerticalPosition == pos
                                ChipOption(
                                    label = pos.label,
                                    isSelected = isSelected,
                                    accentColor = previewAccent,
                                    accentLight = previewAccentLight,
                                    onClick = { previewSettings = previewSettings.copy(waveVerticalPosition = pos) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SectionHeader("WAVE VISIBILITY MODE")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WaveVisibility.values().forEach { vis ->
                                val isSelected = previewSettings.waveVisibility == vis
                                ChipOption(
                                    label = vis.label,
                                    isSelected = isSelected,
                                    accentColor = previewAccent,
                                    accentLight = previewAccentLight,
                                    onClick = { previewSettings = previewSettings.copy(waveVisibility = vis) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SliderSetting(
                            title = "Wave Vertical Height",
                            valueDisplay = "${previewSettings.waveHeightPercent}%",
                            value = previewSettings.waveHeightPercent.toFloat(),
                            valueRange = 40f..90f,
                            accentColor = previewAccent,
                            onValueChange = {
                                previewSettings = previewSettings.copy(waveHeightPercent = it.roundToInt())
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SliderSetting(
                            title = "Wave Letter Font Size",
                            valueDisplay = "${previewSettings.waveLetterSizeSp} sp",
                            value = previewSettings.waveLetterSizeSp.toFloat(),
                            valueRange = 8f..14f,
                            accentColor = previewAccent,
                            onValueChange = {
                                previewSettings = previewSettings.copy(waveLetterSizeSp = it.roundToInt())
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ToggleRow(
                            title = "Left-Handed Wave Mode",
                            checked = previewSettings.waveOnLeftSide,
                            accentColor = previewAccent,
                            onCheckedChange = { previewSettings = previewSettings.copy(waveOnLeftSide = it) }
                        )

                        ToggleRow(
                            title = "Swipe Up on Home to Search",
                            checked = previewSettings.swipeUpToSearch,
                            accentColor = previewAccent,
                            onCheckedChange = { previewSettings = previewSettings.copy(swipeUpToSearch = it) }
                        )
                    }

                    4 -> {
                        // ==========================================
                        // TAB 5: ABOUT & UPDATES
                        // ==========================================
                        SectionHeader("SYSTEM & DEFAULT LAUNCHER")

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x22FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRequestDefaultLauncher() }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(previewAccent.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Home,
                                        contentDescription = null,
                                        tint = previewAccentLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Set as Default Launcher",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Ensure Niagara handles the home button and gesture navigation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        SectionHeader("IN-APP UPDATER & BUILD")
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x22FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenUpdates() }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(previewAccent.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SystemUpdate,
                                        contentDescription = null,
                                        tint = previewAccentLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Check for Updates",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Niagara Launcher v1.0.6 • Seamless OTA updates",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Reset All Defaults Button
                        Button(
                            onClick = onResetDefaults,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset All Settings to Factory Default",
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Bottom Bar when there are unsaved changes
        AnimatedVisibility(
            visible = hasUnsavedChanges,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = previewAccent,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onApplySettings(previewSettings)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply Changes to Home Screen",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                }
            }
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
