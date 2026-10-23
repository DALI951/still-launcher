package com.niagaraclone.launcher.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.data.model.AppLaunchRule
import com.niagaraclone.launcher.data.model.FocusProfile
import com.niagaraclone.launcher.data.model.StillAccent
import com.niagaraclone.launcher.data.model.StillThemeMode
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.UpdateUiState
import com.niagaraclone.launcher.ui.theme.DangerRed
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary

private enum class SettingsSection(val label: String, val icon: ImageVector) {
    APPEARANCE("Appearance", Icons.Outlined.Palette),
    FAVORITES("Favorites & Apps", Icons.Outlined.Apps),
    FOCUS("Focus", Icons.Outlined.SelfImprovement),
    YOUTUBE("YouTube", Icons.Outlined.PlayArrow),
    UPDATES("Updates", Icons.Outlined.SystemUpdate),
    PRIVACY("Privacy", Icons.Outlined.Security),
    ABOUT("About", Icons.Outlined.Info)
}

/**
 * Consolidated settings interface for Still — Minimal Launcher.
 * Contains only the 7 necessary sections without unneeded bloat.
 */
@Composable
fun LauncherSettingsScreen(
    viewModel: LauncherViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val bgColor = Color(settings.themeMode.bgHex)
    val textColor = Color(settings.themeMode.textHex)
    val textSecondaryColor = Color(settings.themeMode.textSecondaryHex)
    val accent = Color(settings.activeAccentHex)
    val accentLight = Color(settings.activeAccentLightHex)

    var currentSection by remember { mutableStateOf(SettingsSection.APPEARANCE) }
    var showDisableGateDialog by remember { mutableStateOf(false) }

    Surface(
        color = bgColor,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close Settings",
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Still Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = textColor
                    )
                    Text(
                        text = currentSection.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondaryColor
                    )
                }
            }

            // Section Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsSection.values().forEach { section ->
                    val isSelected = currentSection == section
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) accent.copy(alpha = 0.25f) else Color(0x10FFFFFF),
                        border = if (isSelected) BorderStroke(1.dp, accentLight) else BorderStroke(1.dp, Color(0x14FFFFFF)),
                        modifier = Modifier.clickable { currentSection = section }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = null,
                                tint = if (isSelected) accentLight else textSecondaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = section.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                ),
                                color = if (isSelected) textColor else textSecondaryColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0x14FFFFFF))

            // Section Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentSection) {
                    SettingsSection.APPEARANCE -> AppearanceSection(viewModel, settings, textColor, textSecondaryColor, accent, accentLight)
                    SettingsSection.FAVORITES -> FavoritesSection(viewModel, uiState, textColor, textSecondaryColor, accent, accentLight)
                    SettingsSection.FOCUS -> FocusSection(viewModel, settings, textColor, textSecondaryColor, accent, accentLight)
                    SettingsSection.YOUTUBE -> YouTubeSection(
                        viewModel = viewModel,
                        settings = settings,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor,
                        accent = accent,
                        accentLight = accentLight,
                        onRequestDisableGate = { showDisableGateDialog = true },
                        onOpenGateDirectly = {
                            onDismissRequest()
                            viewModel.setYouTubeGateActive(true)
                        }
                    )
                    SettingsSection.UPDATES -> UpdatesSection(viewModel, uiState.updateState, textColor, textSecondaryColor, accent, accentLight)
                    SettingsSection.PRIVACY -> PrivacySection(context, textColor, textSecondaryColor, accent, accentLight)
                    SettingsSection.ABOUT -> AboutSection(textColor, textSecondaryColor, accent, accentLight)
                }
            }
        }
    }

    // Explicit confirmation dialog for disabling YouTube gate
    if (showDisableGateDialog) {
        AlertDialog(
            onDismissRequest = { showDisableGateDialog = false },
            title = {
                Text(
                    text = "Disable Intentional YouTube Gate?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = textColor
                )
            },
            text = {
                Text(
                    text = "Disabling the gate will open the standard YouTube application with recommendations, trending feeds, and algorithmic rabbit holes. Are you sure you want to disable intentional viewing?",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = textSecondaryColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSettings { it.copy(interceptYouTube = false) }
                        showDisableGateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Disable Gate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableGateDialog = false }) {
                    Text("Keep Gate Enabled", color = textColor)
                }
            },
            containerColor = Color(0xFF1C1C1E)
        )
    }
}

// ==========================================
// 1. APPEARANCE SECTION
// ==========================================
@Composable
private fun AppearanceSection(
    viewModel: LauncherViewModel,
    settings: com.niagaraclone.launcher.data.model.LauncherSettings,
    textColor: Color,
    textSecondaryColor: Color,
    accent: Color,
    accentLight: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Theme Mode
        item {
            SettingHeader(title = "Theme", description = "Choose visual contrast level")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StillThemeMode.values().forEach { mode ->
                    val isSelected = settings.themeMode == mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) accent.copy(alpha = 0.3f) else Color(0x10FFFFFF),
                        border = if (isSelected) BorderStroke(1.dp, accentLight) else BorderStroke(1.dp, Color(0x18FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateSettings { it.copy(themeMode = mode) } }
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            ),
                            color = if (isSelected) textColor else textSecondaryColor,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Icon Presentation
        item {
            SettingHeader(title = "Icon Presentation", description = "Minimalist Phone text-only vs simple monochrome icons")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (settings.showAppIcons) "Monochrome Icons" else "Pure Typography (Text Only)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor
                    )
                    Text(
                        text = if (settings.showAppIcons) "Displays simple desaturated icons" else "No icons; eliminates visual triggers",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                }
                Switch(
                    checked = settings.showAppIcons,
                    onCheckedChange = { viewModel.updateSettings { s -> s.copy(showAppIcons = it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = accentLight, checkedTrackColor = accent)
                )
            }
        }

        // Text Size
        item {
            SettingHeader(title = "Text Size", description = "${settings.textSizeSp} sp")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(14 to "Small", 16 to "Medium", 18 to "Large").forEach { (size, label) ->
                    val isSelected = settings.textSizeSp == size
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) accent.copy(alpha = 0.3f) else Color(0x10FFFFFF),
                        border = if (isSelected) BorderStroke(1.dp, accentLight) else BorderStroke(1.dp, Color(0x18FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateSettings { it.copy(textSizeSp = size) } }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) textColor else textSecondaryColor,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Header Elements (Clock, 24h, Date, Battery)
        item {
            SettingHeader(title = "Home Screen Elements", description = "Configure information displayed on home screen")
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingToggleRow(
                    label = "Digital Clock",
                    checked = settings.showClock,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    accent = accent,
                    accentLight = accentLight,
                    onCheckedChange = { viewModel.updateSettings { s -> s.copy(showClock = it) } }
                )
                SettingToggleRow(
                    label = "24-Hour Time Format",
                    checked = settings.clock24Hour,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    accent = accent,
                    accentLight = accentLight,
                    onCheckedChange = { viewModel.updateSettings { s -> s.copy(clock24Hour = it) } }
                )
                SettingToggleRow(
                    label = "Date & Day of Week",
                    checked = settings.showDate,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    accent = accent,
                    accentLight = accentLight,
                    onCheckedChange = { viewModel.updateSettings { s -> s.copy(showDate = it) } }
                )
                SettingToggleRow(
                    label = "Battery Percentage",
                    checked = settings.showBattery,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    accent = accent,
                    accentLight = accentLight,
                    onCheckedChange = { viewModel.updateSettings { s -> s.copy(showBattery = it) } }
                )
            }
        }

        // Subtle Accent Color
        item {
            SettingHeader(title = "Subtle Accent Color", description = "One restrained highlight color")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StillAccent.values().forEach { item ->
                    val isSelected = settings.accent == item
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(item.hex))
                            .clickable { viewModel.updateSettings { it.copy(accent = item) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==========================================
// 2. FAVORITES & APPLICATIONS SECTION
// ==========================================
@Composable
private fun FavoritesSection(
    viewModel: LauncherViewModel,
    uiState: com.niagaraclone.launcher.ui.LauncherUiState,
    textColor: Color,
    textSecondaryColor: Color,
    accent: Color,
    accentLight: Color
) {
    var searchAppRuleQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingHeader(
                title = "Favorites Ordering",
                description = "Favorites appear on your home screen. Reorder or remove them."
            )
        }

        if (uiState.favoriteApps.isEmpty()) {
            item {
                Text(
                    text = "No favorites currently selected.",
                    color = textSecondaryColor,
                    fontSize = 13.sp
                )
            }
        } else {
            items(uiState.favoriteApps.size) { index ->
                val app = uiState.favoriteApps[index]
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x0EFFFFFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )

                        if (index > 0) {
                            IconButton(onClick = { viewModel.reorderFavorite(index, index - 1) }) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = textSecondaryColor, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (index < uiState.favoriteApps.size - 1) {
                            IconButton(onClick = { viewModel.reorderFavorite(index, index + 1) }) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = textSecondaryColor, modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = { viewModel.removeFavorite(app) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = DangerRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0x14FFFFFF))
            Spacer(modifier = Modifier.height(8.dp))
            SettingHeader(
                title = "Application Launch Rules",
                description = "Set distraction rules: Normal, Delayed (breathing pause), Scheduled (focus hours), or Blocked."
            )
            OutlinedTextField(
                value = searchAppRuleQuery,
                onValueChange = { searchAppRuleQuery = it },
                placeholder = { Text("Filter apps to set rule...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentLight,
                    unfocusedBorderColor = Color(0x22FFFFFF),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )
        }

        val filteredApps = uiState.allApps.filter {
            searchAppRuleQuery.isEmpty() || it.label.contains(searchAppRuleQuery, ignoreCase = true)
        }.take(30)

        items(filteredApps) { app ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0x0AFFFFFF),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = app.label, style = MaterialTheme.typography.bodyMedium, color = textColor)
                        Text(text = app.launchRule.label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = accentLight)
                    }

                    // Cycle rule button
                    OutlinedButton(
                        onClick = {
                            val nextRule = when (app.launchRule) {
                                AppLaunchRule.NORMAL -> AppLaunchRule.DELAYED
                                AppLaunchRule.DELAYED -> AppLaunchRule.SCHEDULED
                                AppLaunchRule.SCHEDULED -> AppLaunchRule.BLOCKED
                                AppLaunchRule.BLOCKED -> AppLaunchRule.NORMAL
                                else -> AppLaunchRule.NORMAL
                            }
                            viewModel.setAppLaunchRule(app, nextRule)
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Text(text = "Change", fontSize = 11.sp, color = textColor)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==========================================
// 3. FOCUS SECTION
// ==========================================
@Composable
private fun FocusSection(
    viewModel: LauncherViewModel,
    settings: com.niagaraclone.launcher.data.model.LauncherSettings,
    textColor: Color,
    textSecondaryColor: Color,
    accent: Color,
    accentLight: Color
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Active Focus Profile
        item {
            SettingHeader(title = "Active Focus Profile", description = "Choose current intentional restriction mode")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FocusProfile.values().forEach { profile ->
                    val isSelected = settings.activeFocusProfile == profile
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) accent.copy(alpha = 0.3f) else Color(0x10FFFFFF),
                        border = if (isSelected) BorderStroke(1.dp, accentLight) else BorderStroke(1.dp, Color(0x18FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateSettings { it.copy(activeFocusProfile = profile) } }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = profile.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                ),
                                color = if (isSelected) textColor else textSecondaryColor
                            )
                        }
                    }
                }
            }
        }

        // Mindful In-App Usage Timers
        item {
            SettingHeader(
                title = "Mindful In-App Usage Timers",
                description = "Set intentional time limits when launching distracting apps to eliminate mindless scrolling"
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x0EFFFFFF),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingToggleRow(
                        label = "Ask timer before launching apps",
                        checked = settings.askTimerOnAppLaunch,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor,
                        accent = accent,
                        accentLight = accentLight,
                        onCheckedChange = { viewModel.updateSettings { s -> s.copy(askTimerOnAppLaunch = it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Default Timer Duration: ${settings.defaultTimerMinutes}m",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 3, 5, 10, 15, 30).forEach { mins ->
                            val isSel = settings.defaultTimerMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) accent.copy(alpha = 0.35f) else Color(0x10FFFFFF),
                                border = if (isSel) BorderStroke(1.dp, accentLight) else BorderStroke(1.dp, Color(0x18FFFFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateSettings { s -> s.copy(defaultTimerMinutes = mins) } }
                            ) {
                                Text(
                                    text = "${mins}m",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSel) textColor else textSecondaryColor,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val canDrawOverlays = com.niagaraclone.launcher.data.AppTimerManager.getInstance(context).canDrawOverlays()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Display Over Apps (Fullscreen Reminder)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = textColor
                            )
                            Text(
                                text = if (canDrawOverlays) "Permission granted. Mindful barrier appears when time is up." else "Required to pop up mindful barrier when timer expires.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (canDrawOverlays) accentLight else Color(0xFFFFA726)
                            )
                        }
                        if (!canDrawOverlays) {
                            Button(
                                onClick = {
                                    com.niagaraclone.launcher.data.AppTimerManager.getInstance(context).openOverlaySettings(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentLight),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Daily Screen Time & Mindfulness Awareness
        item {
            SettingHeader(
                title = "Daily Screen Time & Awareness",
                description = "Minimalist Phone metrics to keep phone use conscious"
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x0EFFFFFF),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingToggleRow(
                        label = "Show Screen Time on Home",
                        checked = settings.showScreenTimeOnHome,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor,
                        accent = accent,
                        accentLight = accentLight,
                        onCheckedChange = { viewModel.updateSettings { s -> s.copy(showScreenTimeOnHome = it) } }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        label = "Show Daily Intention on Home",
                        checked = settings.showDailyIntention,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor,
                        accent = accent,
                        accentLight = accentLight,
                        onCheckedChange = { viewModel.updateSettings { s -> s.copy(showDailyIntention = it) } }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Android Monochrome / Grayscale Settings", color = textColor, fontSize = 12.sp)
                    }
                }
            }
        }

        // Breathing Pause Duration
        item {
            SettingHeader(title = "Breathing Delay Pause", description = "Seconds to pause impulse launches: ${settings.breathingDelaySeconds}s")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3, 5, 10, 15).forEach { seconds ->
                    val isSelected = settings.breathingDelaySeconds == seconds
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) accent.copy(alpha = 0.3f) else Color(0x10FFFFFF),
                        border = if (isSelected) BorderStroke(1.dp, accentLight) else BorderStroke(1.dp, Color(0x18FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateSettings { it.copy(breathingDelaySeconds = seconds) } }
                    ) {
                        Text(
                            text = "${seconds}s",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) textColor else textSecondaryColor,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Study Profile Schedule
        item {
            SettingHeader(title = "Study Profile Schedule", description = "Configured study focus window (${settings.studyStartHour}:00 - ${settings.studyEndHour}:00)")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x0EFFFFFF),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Active between ${settings.studyStartHour}:00 and ${settings.studyEndHour}:00",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apps configured with the 'Scheduled' rule will be blocked during these hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                }
            }
        }

        // Sleep Profile Schedule
        item {
            SettingHeader(title = "Sleep Profile Schedule", description = "Bedtime hours (${settings.sleepStartHour}:00 - ${settings.sleepEndHour}:00, crosses midnight)")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x0EFFFFFF),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Bedtime from ${settings.sleepStartHour}:00 to ${settings.sleepEndHour}:00",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Restricts non-essential applications across midnight to promote healthy sleep habits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==========================================
// 4. YOUTUBE SECTION
// ==========================================
@Composable
private fun YouTubeSection(
    viewModel: LauncherViewModel,
    settings: com.niagaraclone.launcher.data.model.LauncherSettings,
    textColor: Color,
    textSecondaryColor: Color,
    accent: Color,
    accentLight: Color,
    onRequestDisableGate: () -> Unit,
    onOpenGateDirectly: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Built-in Link Gate Master Switch
        item {
            SettingHeader(
                title = "Built-in YouTube Link Gate",
                description = "Enforces single-video intentional viewing without recommendations or algorithmic rabbit holes"
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (settings.interceptYouTube) "Gate Enabled (Protected)" else "Gate Disabled (Unprotected)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (settings.interceptYouTube) accentLight else DangerRed
                    )
                    Text(
                        text = if (settings.interceptYouTube) "Intercepts YouTube launches to enter URL gate" else "Standard YouTube app will open directly",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                }
                Switch(
                    checked = settings.interceptYouTube,
                    onCheckedChange = { checked ->
                        if (checked) {
                            viewModel.updateSettings { it.copy(interceptYouTube = true) }
                        } else {
                            onRequestDisableGate()
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = accentLight, checkedTrackColor = accent)
                )
            }
        }

        // Test Link Gate Button
        item {
            Button(
                onClick = onOpenGateDirectly,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Intentional YouTube Gate", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        // Local Playback History
        item {
            SettingHeader(title = "Playback History", description = "Strictly local history for replaying videos without recommendations")
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingToggleRow(
                    label = "Save Local History",
                    checked = settings.youtubeHistoryEnabled,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    accent = accent,
                    accentLight = accentLight,
                    onCheckedChange = { viewModel.updateSettings { s -> s.copy(youtubeHistoryEnabled = it) } }
                )

                OutlinedButton(
                    onClick = { viewModel.clearYouTubeHistory() },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = textSecondaryColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Local History", color = textColor, fontSize = 13.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==========================================
// 5. UPDATES SECTION
// ==========================================
@Composable
private fun UpdatesSection(
    viewModel: LauncherViewModel,
    updateState: UpdateUiState,
    textColor: Color,
    textSecondaryColor: Color,
    accent: Color,
    accentLight: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingHeader(
                title = "Application Updates",
                description = "Checks GitHub Releases (DALI951/still-launcher) for verified APK updates"
            )
        }

        // Current Version Info
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x0EFFFFFF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Installed Version: v1.0.0",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Repository: DALI951/still-launcher",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                }
            }
        }

        // Dynamic Update State Box
        item {
            when (updateState) {
                is UpdateUiState.Idle -> {
                    Button(
                        onClick = { viewModel.checkForUpdates(force = true) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Outlined.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Updates Now", fontWeight = FontWeight.SemiBold)
                    }
                }

                is UpdateUiState.Checking -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = accentLight, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Checking for latest release...", color = textColor, fontSize = 13.sp)
                    }
                }

                is UpdateUiState.UpToDate -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x1A2ECC71),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2ECC71))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("You are using the latest version of Still.", color = textColor, fontSize = 13.sp)
                        }
                    }
                }

                is UpdateUiState.UpdateAvailable -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x1F4E8CFF),
                        border = BorderStroke(1.dp, accentLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Update Available: ${updateState.releaseInfo.tagName}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = updateState.releaseInfo.releaseNotes.ifEmpty { "Performance improvements and updates." },
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondaryColor
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.startUpdateDownload(updateState.releaseInfo) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download and Install Update", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is UpdateUiState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            text = "Downloading update: ${updateState.progressPercent}% (${String.format("%.1f", updateState.downloadedMb)} MB / ${String.format("%.1f", updateState.totalMb)} MB)",
                            color = textColor,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { updateState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = accentLight,
                            trackColor = Color(0x33FFFFFF)
                        )
                    }
                }

                is UpdateUiState.ReadyToInstall -> {
                    Button(
                        onClick = { viewModel.installPendingUpdate() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Install Downloaded APK Now", fontWeight = FontWeight.Bold)
                    }
                }

                is UpdateUiState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(text = "Update error: ${updateState.message}", color = DangerRed, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.checkForUpdates(force = true) }) {
                            Text("Retry Check", color = textColor)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==========================================
// 6. PRIVACY & PERMISSIONS SECTION
// ==========================================
@Composable
private fun PrivacySection(
    context: android.content.Context,
    textColor: Color,
    textSecondaryColor: Color,
    accent: Color,
    accentLight: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingHeader(
                title = "Permissions & System Status",
                description = "Still enforces intentional rules directly. Special permissions are only requested when needed."
            )
        }

        // Default Launcher
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x0EFFFFFF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Default Home App", fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set Still as your default launcher so pressing Home always returns to your calm screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Open Default Apps Settings", color = textColor, fontSize = 12.sp)
                    }
                }
            }
        }

        // Privacy Guarantee
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x0EFFFFFF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Privacy Guarantee", fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• 100% Offline & Private: No telemetry, no third-party trackers, no user analytics.\n" +
                                "• Zero Ads or Recommendations.\n" +
                                "• All settings, favorites, and history remain strictly on your device.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = textSecondaryColor
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==========================================
// 7. ABOUT SECTION
// ==========================================
@Composable
private fun AboutSection(
    textColor: Color,
    textSecondaryColor: Color,
    accent: Color,
    accentLight: Color
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingHeader(title = "About Still", description = "Product details and mission")
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x0EFFFFFF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Still — Minimal Launcher",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "A minimalist Android home screen designed to eliminate unnecessary stimulation and encourage intentional phone use.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Version: 1.0.0\n" +
                                "Application ID: com.niagaraclone.launcher\n" +
                                "Repository: https://github.com/DALI951/still-launcher",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/DALI951/still-launcher")
                                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View on GitHub", color = textColor, fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==========================================
// HELPER COMPOSABLES
// ==========================================
@Composable
private fun SettingHeader(title: String, description: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp), color = TextPrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary)
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    textColor: Color,
    textSecondaryColor: Color,
    accent: Color,
    accentLight: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = textColor)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = accentLight, checkedTrackColor = accent)
        )
    }
}
