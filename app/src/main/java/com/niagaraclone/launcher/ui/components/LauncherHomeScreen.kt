package com.niagaraclone.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.R
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.ui.LauncherUiState
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.UpdateUiState
import com.niagaraclone.launcher.ui.theme.NiagaraBlue
import com.niagaraclone.launcher.ui.theme.NiagaraBlueLight
import com.niagaraclone.launcher.ui.theme.ScrimDark
import com.niagaraclone.launcher.ui.theme.ScrimLight
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Main Home Screen combining wallpaper scrim, AtAGlanceHeader, Updates/Setup Banner,
 * Favorites, Alphabetical App List, and the signature Niagara Wave Scroller.
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

    // Back button handling
    BackHandler(
        enabled = uiState.isSearchActive ||
                uiState.isManageAppsActive ||
                uiState.isUpdateSheetActive ||
                uiState.selectedAppForMenu != null ||
                listState.firstVisibleItemIndex > 0
    ) {
        when {
            uiState.isSearchActive -> viewModel.setSearchActive(false)
            uiState.isManageAppsActive -> viewModel.setManageAppsActive(false)
            uiState.isUpdateSheetActive -> viewModel.setUpdateSheetActive(false)
            uiState.selectedAppForMenu != null -> viewModel.closeAppContextMenu()
            listState.firstVisibleItemIndex > 0 -> {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    // Build unique list items and section index map for the LazyColumn
    val (listItems, sectionIndexMap) = remember(uiState.favoriteApps, uiState.allApps) {
        buildListItemsAndIndices(uiState.favoriteApps, uiState.allApps)
    }

    // React to wave scrubbing symbol changes with nearest-letter snapping
    LaunchedEffect(uiState.selectedSectionSymbol) {
        val symbol = uiState.selectedSectionSymbol ?: return@LaunchedEffect
        if (symbol == '★') {
            listState.scrollToItem(0)
        } else {
            val targetIndex = sectionIndexMap[symbol] ?: run {
                val availableLetters = sectionIndexMap.keys.sorted()
                val nearestChar = availableLetters.minByOrNull { abs(it.code - symbol.code) }
                nearestChar?.let { sectionIndexMap[it] }
            }
            if (targetIndex != null) {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Gradient scrim for contrast over system wallpapers
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ScrimLight,
                        Color(0x33000000),
                        ScrimDark
                    )
                )
            )
    ) {
        // Main App List & Header
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = 28.dp,
                bottom = 120.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header: Minimalist Clock, Date & Status
            item(key = "header_at_a_glance") {
                AtAGlanceHeader(
                    systemStatus = uiState.systemStatus,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Optional Interactive Banner: Update Available or Setup Needed
            item(key = "header_update_banner") {
                HomeStatusBanner(
                    updateState = uiState.updateState,
                    hasEssentialPermissions = uiState.permissionsState.allEssentialGranted,
                    onOpenUpdateSheet = { viewModel.setUpdateSheetActive(true) },
                    onDownloadUpdate = { viewModel.downloadUpdate(it) },
                    onInstallUpdate = { viewModel.installDownloadedApk(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .padding(end = 48.dp)
                )
            }

            // Flattened list items with section-prefixed unique keys
            items(
                count = listItems.size,
                key = { index -> listItems[index].key }
            ) { index ->
                when (val item = listItems[index]) {
                    is HomeScreenListItem.FavoriteAppItem -> {
                        AppRowItem(
                            app = item.app,
                            onClick = { viewModel.launchApp(item.app) },
                            onLongClick = { viewModel.openAppContextMenu(item.app) },
                            showFavoriteStar = false,
                            modifier = Modifier.padding(end = 48.dp)
                        )
                    }
                    is HomeScreenListItem.AllAppItem -> {
                        AppRowItem(
                            app = item.app,
                            onClick = { viewModel.launchApp(item.app) },
                            onLongClick = { viewModel.openAppContextMenu(item.app) },
                            showFavoriteStar = false,
                            modifier = Modifier.padding(end = 48.dp)
                        )
                    }
                    is HomeScreenListItem.SectionHeader -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp, top = 20.dp, bottom = 6.dp, end = 48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = NiagaraBlue
                            )
                        }
                    }
                    is HomeScreenListItem.DividerItem -> {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Right Edge: Niagara Wave Alphabet Scroller
        NiagaraWaveScroller(
            symbols = uiState.alphabetSymbols,
            availableSymbols = uiState.availableSymbols,
            onSymbolSelected = { symbol ->
                viewModel.onWaveScrubSymbolChanged(symbol)
            },
            onScrubbingStateChanged = { isScrubbing ->
                if (isScrubbing) {
                    viewModel.onWaveScrubStarted()
                } else {
                    viewModel.onWaveScrubEnded()
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 40.dp)
        )

        // Bottom Ergonomic Quick Actions Pill (Search, Manage, Updates)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Pill
            Surface(
                shape = CircleShape,
                color = Color(0x55000000),
                contentColor = TextPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.setSearchActive(true) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = TextSecondary
                    )
                }
            }

            // Manage Apps Button
            Surface(
                shape = CircleShape,
                color = Color(0x55000000),
                contentColor = TextSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.setManageAppsActive(true) }
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Manage Apps",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Updates & Permissions Setup Pill
            val hasUpdateNotice = uiState.updateState is UpdateUiState.UpdateAvailable ||
                    uiState.updateState is UpdateUiState.ReadyToInstall ||
                    !uiState.permissionsState.allEssentialGranted

            Surface(
                shape = CircleShape,
                color = if (hasUpdateNotice) Color(0x771E5CCB) else Color(0x55000000),
                contentColor = if (hasUpdateNotice) Color.White else TextSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.setUpdateSheetActive(true) }
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.updateState is UpdateUiState.UpdateAvailable || uiState.updateState is UpdateUiState.ReadyToInstall) {
                            Icons.Outlined.SystemUpdate
                        } else {
                            Icons.Outlined.CloudDownload
                        },
                        contentDescription = "Updates & Setup",
                        tint = if (hasUpdateNotice) NiagaraBlueLight else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

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
            }
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

        // Manage Apps Sheet (Favorites & Hidden Apps)
        if (uiState.isManageAppsActive) {
            ManageAppsSheet(
                favoriteApps = uiState.favoriteApps,
                hiddenApps = uiState.hiddenApps,
                onDismissRequest = { viewModel.setManageAppsActive(false) },
                onRemoveFavorite = { viewModel.toggleFavorite(it) },
                onMoveFavoriteUp = { viewModel.moveFavorite(it, -1) },
                onMoveFavoriteDown = { viewModel.moveFavorite(it, 1) },
                onUnhideApp = { viewModel.toggleHideApp(it) }
            )
        }

        // Updates & Permissions Setup Sheet
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
    }
}

@Composable
private fun HomeStatusBanner(
    updateState: UpdateUiState,
    hasEssentialPermissions: Boolean,
    onOpenUpdateSheet: () -> Unit,
    onDownloadUpdate: (com.niagaraclone.launcher.data.ReleaseInfo) -> Unit,
    onInstallUpdate: (java.io.File) -> Unit,
    modifier: Modifier = Modifier
) {
    when (updateState) {
        is UpdateUiState.ReadyToInstall -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x444CAF50),
                modifier = modifier.clickable { onInstallUpdate(updateState.apkFile) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update downloaded: ${updateState.releaseInfo.tagName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap to install now",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF81C784)
                        )
                    }
                }
            }
        }
        is UpdateUiState.Downloading -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x33FFFFFF),
                modifier = modifier.clickable(onClick = onOpenUpdateSheet)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloading ${updateState.releaseInfo.tagName}…",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                        Text(
                            text = "${updateState.progressPercent}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = NiagaraBlueLight
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { updateState.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = NiagaraBlue,
                        trackColor = Color(0x22FFFFFF)
                    )
                }
            }
        }
        is UpdateUiState.UpdateAvailable -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x441E5CCB),
                modifier = modifier.clickable(onClick = onOpenUpdateSheet)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        tint = NiagaraBlueLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New version available: ${updateState.releaseInfo.tagName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap to view release notes & download",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        else -> {
            if (!hasEssentialPermissions) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x33FFFFFF),
                    modifier = modifier.clickable(onClick = onOpenUpdateSheet)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = NiagaraBlueLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup Niagara Launcher",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Set default home app & allow in-app updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * List item model for the Home Screen LazyColumn with section-prefixed keys.
 */
private sealed class HomeScreenListItem(val key: String) {
    data class FavoriteAppItem(val app: AppInfo) : HomeScreenListItem("fav_${app.uniqueKey}")
    data class AllAppItem(val app: AppInfo) : HomeScreenListItem("all_${app.uniqueKey}")
    data class SectionHeader(val title: String) : HomeScreenListItem("header_$title")
    data class DividerItem(val id: String) : HomeScreenListItem("divider_$id")
}

private fun buildListItemsAndIndices(
    favoriteApps: List<AppInfo>,
    allApps: List<AppInfo>
): Pair<List<HomeScreenListItem>, Map<Char, Int>> {
    val items = mutableListOf<HomeScreenListItem>()
    val sectionIndices = mutableMapOf<Char, Int>()

    val headerOffset = 2 // item 0 is AtAGlanceHeader, item 1 is StatusBanner

    // Favorites section
    if (favoriteApps.isNotEmpty()) {
        favoriteApps.forEach { app ->
            items.add(HomeScreenListItem.FavoriteAppItem(app))
        }
        items.add(HomeScreenListItem.DividerItem("favorites_end"))
    }

    // Alphabetical sections
    var currentLetter: Char? = null
    allApps.forEach { app ->
        val letter = app.sectionLetter
        if (letter != currentLetter) {
            currentLetter = letter
            val headerIndex = items.size + headerOffset
            items.add(HomeScreenListItem.SectionHeader(letter.toString()))
            if (!sectionIndices.containsKey(letter)) {
                sectionIndices[letter] = headerIndex
            }
        }
        items.add(HomeScreenListItem.AllAppItem(app))
    }

    return Pair(items, sectionIndices)
}
