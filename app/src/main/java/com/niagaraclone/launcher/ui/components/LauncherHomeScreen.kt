package com.niagaraclone.launcher.ui.components

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.R
import com.niagaraclone.launcher.data.ReleaseInfo
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.data.model.WallpaperTheme
import com.niagaraclone.launcher.ui.LauncherUiState
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.UpdateUiState
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

/**
 * Main Home Screen with authentic Niagara two-mode layout:
 * - Mode 1 (Default): Favorites-only ergonomic home screen
 * - Mode 2: Alphabetical All-Apps list activated via Wave Scroller or "All Apps" pill
 */
@Composable
fun LauncherHomeScreen(
    viewModel: LauncherViewModel,
    uiState: LauncherUiState,
    modifier: Modifier = Modifier,
    onRequestDefaultLauncher: () -> Unit = {},
    onRequestUnknownSources: () -> Unit = {},
    onRequestNotifications: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val settings = uiState.settings
    val themeAccent = Color(settings.activeAccentHex)
    val themeAccentLight = Color(settings.activeAccentLightHex)

    var isWaveScrubbing by remember { mutableStateOf(false) }
    val dragThresholdPx = with(density) { 40.dp.toPx() }

    // Back button handling
    BackHandler(
        enabled = uiState.isSearchActive ||
                uiState.isManageAppsActive ||
                uiState.isSettingsSheetActive ||
                uiState.isUpdateSheetActive ||
                uiState.isDefaultPromptDialogVisible ||
                uiState.selectedAppForMenu != null ||
                uiState.isAllAppsVisible ||
                listState.firstVisibleItemIndex > 0
    ) {
        when {
            uiState.isSearchActive -> viewModel.setSearchActive(false)
            uiState.isManageAppsActive -> viewModel.setManageAppsActive(false)
            uiState.isSettingsSheetActive -> viewModel.setSettingsSheetActive(false)
            uiState.isUpdateSheetActive -> viewModel.setUpdateSheetActive(false)
            uiState.isDefaultPromptDialogVisible -> viewModel.setDefaultPromptDialogVisible(false)
            uiState.selectedAppForMenu != null -> viewModel.closeAppContextMenu()
            uiState.isAllAppsVisible -> viewModel.setAllAppsVisible(false)
            listState.firstVisibleItemIndex > 0 -> {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    // Build unique alphabetical list items and section index map for All Apps mode
    val (allAppItems, sectionIndexMap) = remember(uiState.allApps) {
        buildAllAppItemsAndIndices(uiState.allApps)
    }

    // React to wave scrubbing symbol changes
    LaunchedEffect(uiState.selectedSectionSymbol) {
        val symbol = uiState.selectedSectionSymbol ?: return@LaunchedEffect
        if (symbol == '★') {
            viewModel.setAllAppsVisible(false)
        } else if (symbol == '⚙') {
            viewModel.setAllAppsVisible(true)
            val settingsIndex = sectionIndexMap['⚙'] ?: allAppItems.lastIndex
            listState.scrollToItem(settingsIndex)
        } else {
            val targetIndex = sectionIndexMap[symbol] ?: run {
                val availableLetters = sectionIndexMap.keys.filter { it != '⚙' }.sorted()
                val nearestChar = availableLetters.minByOrNull { abs(it.code - symbol.code) }
                nearestChar?.let { sectionIndexMap[it] }
            }
            if (targetIndex != null) {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    // Custom photo bitmap loader
    val customBitmap = remember(settings.wallpaperTheme, settings.customPhotoPath) {
        if (settings.wallpaperTheme == WallpaperTheme.CUSTOM_PHOTO && !settings.customPhotoPath.isNullOrEmpty()) {
            try {
                BitmapFactory.decodeFile(settings.customPhotoPath)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Wallpaper Theme & Background Rendering
    val backgroundModifier = when (settings.wallpaperTheme) {
        WallpaperTheme.SYSTEM_WALLPAPER -> {
            Modifier.background(
                Color(settings.themePreset.scrimBaseHex).copy(
                    alpha = settings.scrimOpacityPercent / 100f
                )
            )
        }
        WallpaperTheme.CUSTOM_PHOTO -> {
            Modifier.background(Color.Black)
        }
        else -> {
            Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(settings.wallpaperTheme.topColorHex),
                        Color(settings.wallpaperTheme.bottomColorHex)
                    )
                )
            )
        }
    }

    // Swipe-Up to Search Gesture Detector
    val swipeModifier = if (settings.swipeUpToSearch) {
        Modifier.pointerInput(uiState.isSearchActive, uiState.isAllAppsVisible, isWaveScrubbing) {
            if (uiState.isSearchActive || uiState.isAllAppsVisible || isWaveScrubbing) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var totalDrag = 0f
                var triggered = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    val deltaY = change.position.y - change.previousPosition.y
                    totalDrag += deltaY
                    if (totalDrag < -dragThresholdPx && !triggered) {
                        triggered = true
                        change.consume()
                        viewModel.setSearchActive(true)
                    }
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(backgroundModifier)
            .then(swipeModifier)
    ) {
        // Custom photo background layer
        if (settings.wallpaperTheme == WallpaperTheme.CUSTOM_PHOTO && customBitmap != null) {
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Scrim over custom photo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.scrimOpacityPercent / 100f))
            )
        } else if (settings.wallpaperTheme != WallpaperTheme.SYSTEM_WALLPAPER && settings.scrimOpacityPercent > 0) {
            // Subtle scrim overlay for curated themes
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (settings.scrimOpacityPercent / 100f) * 0.4f))
            )
        }
        if (!uiState.isAllAppsVisible) {
            // ==========================================
            // MODE 1: PURE NIAGARA FAVORITES HOME SCREEN
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Configurable top spacer to position favorites in ergonomic thumb reach
                item(key = "fav_top_spacer") {
                    Spacer(modifier = Modifier.height(settings.favoritesTopOffsetDp.dp))
                }

                // Header: Minimalist Clock, Date & Status
                item(key = "header_at_a_glance") {
                    AtAGlanceHeader(
                        systemStatus = uiState.systemStatus,
                        settings = settings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = settings.edgePaddingDp.dp,
                                end = (settings.edgePaddingDp + 16).dp
                            )
                    )
                }

                // Optional Interactive Banner: Updates or Setup
                item(key = "header_status_banner") {
                    HomeStatusBanner(
                        updateState = uiState.updateState,
                        isDefaultLauncher = uiState.permissionsState.isDefaultLauncher,
                        hasEssentialPermissions = uiState.permissionsState.allEssentialGranted,
                        onRequestDefaultLauncher = onRequestDefaultLauncher,
                        onOpenUpdateSheet = { viewModel.setUpdateSheetActive(true) },
                        onDownloadUpdate = { viewModel.downloadUpdate(it) },
                        onInstallUpdate = { viewModel.installDownloadedApk(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = settings.edgePaddingDp.dp,
                                end = (settings.edgePaddingDp + 20).dp,
                                top = 4.dp,
                                bottom = 8.dp
                            )
                    )
                }

                // ONLY Favorites apps on the Home Screen
                items(
                    count = uiState.favoriteApps.size,
                    key = { index -> "fav_${uiState.favoriteApps[index].uniqueKey}" }
                ) { index ->
                    val app = uiState.favoriteApps[index]
                    AppRowItem(
                        app = app,
                        settings = settings,
                        onClick = { viewModel.launchApp(app) },
                        onLongClick = { viewModel.openAppContextMenu(app) },
                        showFavoriteStar = false
                    )
                }

                // Quick Chip to open All Apps
                item(key = "open_all_apps_chip") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = settings.edgePaddingDp.dp,
                                top = 16.dp,
                                bottom = 24.dp
                            )
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x33FFFFFF),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { viewModel.setAllAppsVisible(true) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "All Apps",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = themeAccentLight
                                )
                                Text(
                                    text = "• ${uiState.allApps.size}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // MODE 2: ALPHABETICAL ALL-APPS VIEW
            // ==========================================
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
            ) {
                // Return to Favorites chip at top
                item(key = "back_to_favorites_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = settings.edgePaddingDp.dp,
                                end = (settings.edgePaddingDp + 16).dp,
                                top = 8.dp,
                                bottom = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = themeAccent.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeAccentLight.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { viewModel.setAllAppsVisible(false) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = themeAccentLight,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Favorites",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Alphabetical items (Section headers + Apps)
                items(
                    count = allAppItems.size,
                    key = { index -> allAppItems[index].key }
                ) { index ->
                    when (val item = allAppItems[index]) {
                        is AllAppListItem.AppItem -> {
                            AppRowItem(
                                app = item.app,
                                settings = settings,
                                onClick = { viewModel.launchApp(item.app) },
                                onLongClick = { viewModel.openAppContextMenu(item.app) },
                                showFavoriteStar = item.app.isFavorite
                            )
                        }
                        is AllAppListItem.SectionHeader -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = settings.edgePaddingDp.dp,
                                        top = 18.dp,
                                        bottom = 6.dp,
                                        end = (settings.edgePaddingDp + 16).dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = themeAccentLight
                                )
                            }
                        }
                        is AllAppListItem.SettingsItem -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = settings.edgePaddingDp.dp,
                                        end = (settings.edgePaddingDp + 16).dp,
                                        top = 16.dp,
                                        bottom = 36.dp
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x22FFFFFF))
                                    .clickable { viewModel.setSettingsSheetActive(true) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(themeAccent.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = "Niagara Settings",
                                        tint = themeAccentLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Niagara Settings",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Theme Studio & Customization",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Niagara Wave Alphabet Scroller (Positioned according to waveOnLeftSide & waveVerticalPosition settings)
        NiagaraWaveScroller(
            symbols = uiState.alphabetSymbols,
            availableSymbols = uiState.availableSymbols,
            settings = settings,
            themePreset = settings.themePreset,
            isLeftSide = settings.waveOnLeftSide,
            onSymbolSelected = { symbol ->
                viewModel.onWaveScrubSymbolChanged(symbol)
            },
            onScrubbingStateChanged = { isScrubbing ->
                isWaveScrubbing = isScrubbing
                if (isScrubbing) {
                    viewModel.onWaveScrubStarted()
                } else {
                    viewModel.onWaveScrubEnded()
                }
            },
            onOpenSettings = {
                viewModel.setSettingsSheetActive(true)
            },
            modifier = Modifier
                .align(if (settings.waveOnLeftSide) Alignment.CenterStart else Alignment.CenterEnd)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 16.dp)
        )

        // App Search Fullscreen Overlay
        AppSearchOverlay(
            visible = uiState.isSearchActive,
            query = uiState.searchQuery,
            filteredApps = uiState.filteredApps,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onDismiss = { viewModel.setSearchActive(false) },
            onAppClick = { app ->
                viewModel.launchApp(app)
                viewModel.setSearchActive(false)
            },
            onAppLongClick = { app ->
                viewModel.openAppContextMenu(app)
            },
            settings = settings
        )

        // Context Menu Modal Sheet
        if (uiState.selectedAppForMenu != null) {
            AppContextMenuSheet(
                app = uiState.selectedAppForMenu,
                favoritesCount = uiState.favoriteApps.size,
                onDismissRequest = { viewModel.closeAppContextMenu() },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onMoveFavoriteUp = { viewModel.moveFavorite(it, -1) },
                onMoveFavoriteDown = { viewModel.moveFavorite(it, 1) },
                onToggleHide = { viewModel.toggleHideApp(it) },
                onOpenAppInfo = { viewModel.openAppDetails(it) },
                onUninstall = { viewModel.requestUninstall(it) }
            )
        }

        // Manage Apps Sheet
        if (uiState.isManageAppsActive) {
            ManageAppsSheet(
                favoriteApps = uiState.favoriteApps,
                allApps = uiState.allApps,
                hiddenApps = uiState.hiddenApps,
                onDismissRequest = { viewModel.setManageAppsActive(false) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onMoveFavoriteUp = { viewModel.moveFavorite(it, -1) },
                onMoveFavoriteDown = { viewModel.moveFavorite(it, 1) },
                onUnhideApp = { viewModel.toggleHideApp(it) },
                settings = settings
            )
        }

        // Updates & Setup Sheet
        if (uiState.isUpdateSheetActive) {
            UpdateAndPermissionsSheet(
                updateState = uiState.updateState,
                permissionsState = uiState.permissionsState,
                onDismissRequest = { viewModel.setUpdateSheetActive(false) },
                onCheckForUpdates = { viewModel.checkForUpdates(silent = false) },
                onDownloadUpdate = { viewModel.downloadUpdate(it) },
                onInstallUpdate = { viewModel.installDownloadedApk(it) },
                onRequestDefaultLauncher = onRequestDefaultLauncher,
                onRequestUnknownSources = onRequestUnknownSources,
                onRequestNotifications = onRequestNotifications
            )
        }

        // Full-Screen Dedicated Niagara Settings App & Theme Studio
        AnimatedVisibility(
            visible = uiState.isSettingsSheetActive,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            LauncherSettingsScreen(
                settings = settings,
                onDismissRequest = { viewModel.setSettingsSheetActive(false) },
                onApplySettings = { updated ->
                    viewModel.updateSettings { updated }
                    viewModel.setSettingsSheetActive(false)
                },
                onResetDefaults = { viewModel.resetSettingsToDefault() },
                onRequestDefaultLauncher = onRequestDefaultLauncher,
                onOpenUpdates = {
                    viewModel.setSettingsSheetActive(false)
                    viewModel.setUpdateSheetActive(true)
                }
            )
        }

        // Default Launcher Prompt Dialog (if Niagara is not currently default home app)
        if (uiState.isDefaultPromptDialogVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.setDefaultPromptDialogVisible(false) },
                containerColor = Color(0xFA1E222A),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = themeAccentLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Set as Default Home App",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                },
                text = {
                    Text(
                        text = "To use Niagara Launcher whenever you press the Home button or swipe up, set it as your default home app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setDefaultPromptDialogVisible(false)
                            onRequestDefaultLauncher()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent)
                    ) {
                        Text("Set as Default Home App", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setDefaultPromptDialogVisible(false) }) {
                        Text("Later", color = TextSecondary)
                    }
                }
            )
        }
    }
}

private sealed class AllAppListItem(val key: String) {
    data class AppItem(val app: AppInfo) : AllAppListItem("all_${app.uniqueKey}")
    data class SectionHeader(val title: String) : AllAppListItem("header_$title")
    data object SettingsItem : AllAppListItem("bottom_niagara_settings")
}

private fun buildAllAppItemsAndIndices(
    allApps: List<AppInfo>
): Pair<List<AllAppListItem>, Map<Char, Int>> {
    val items = mutableListOf<AllAppListItem>()
    val sectionIndices = mutableMapOf<Char, Int>()

    val headerOffset = 1 // Item 0 is the "Back to Favorites" header

    var currentLetter: Char? = null
    allApps.forEach { app ->
        val letter = app.sectionLetter
        if (letter != currentLetter) {
            currentLetter = letter
            val headerIndex = items.size + headerOffset
            items.add(AllAppListItem.SectionHeader(letter.toString()))
            if (!sectionIndices.containsKey(letter)) {
                sectionIndices[letter] = headerIndex
            }
        }
        items.add(AllAppListItem.AppItem(app))
    }

    // Append dedicated Niagara Settings item at the bottom of the list
    val settingsItemIndex = items.size + headerOffset
    items.add(AllAppListItem.SettingsItem)
    sectionIndices['⚙'] = settingsItemIndex

    return Pair(items, sectionIndices)
}

@Composable
private fun HomeStatusBanner(
    updateState: UpdateUiState,
    isDefaultLauncher: Boolean,
    hasEssentialPermissions: Boolean,
    onRequestDefaultLauncher: () -> Unit,
    onOpenUpdateSheet: () -> Unit,
    onDownloadUpdate: (ReleaseInfo) -> Unit,
    onInstallUpdate: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        !isDefaultLauncher -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x551E5CCB),
                modifier = modifier.clickable(onClick = onRequestDefaultLauncher)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = Color(0xFF99C2FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set as Default Home Screen",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap here to make Niagara your real Home app",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        updateState is UpdateUiState.ReadyToInstall -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x444CAF50),
                modifier = modifier.clickable { onInstallUpdate(updateState.apkFile) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update downloaded: ${updateState.releaseInfo.tagName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap to install now",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF81C784)
                        )
                    }
                }
            }
        }
        updateState is UpdateUiState.Downloading -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x33FFFFFF),
                modifier = modifier.clickable(onClick = onOpenUpdateSheet)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloading ${updateState.releaseInfo.tagName}…",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                            color = TextPrimary
                        )
                        Text(
                            text = "${updateState.progressPercent}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { updateState.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = Color(0xFF4E8CFF),
                        trackColor = Color(0x22FFFFFF)
                    )
                }
            }
        }
        updateState is UpdateUiState.UpdateAvailable -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x441E5CCB),
                modifier = modifier.clickable(onClick = onOpenUpdateSheet)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        tint = Color(0xFF99C2FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New version available: ${updateState.releaseInfo.tagName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap to download & install",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        else -> {
            if (!hasEssentialPermissions) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x33FFFFFF),
                    modifier = modifier.clickable(onClick = onOpenUpdateSheet)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = Color(0xFF99C2FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup Niagara Launcher",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = TextPrimary
                            )
                            Text(
                                text = "Set default home app & allow in-app updates",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
