package com.niagaraclone.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niagaraclone.launcher.ui.LauncherViewModel

/**
 * Main Home Screen for Still — Minimal Launcher.
 *
 * Restrained minimalist hierarchy:
 * - Top: Digital clock, date with day of week, optional small battery indicator.
 * - Middle: Short user-selected favorites list (plain text by default, optional monochrome icons).
 * - Bottom: One clearly discoverable search pill to access installed apps.
 * - Zero feeds, zero recommendations, zero Niagara-style alphabet waves, zero widgets.
 */
@Composable
fun LauncherHomeScreen(
    viewModel: LauncherViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val bgColor = Color(settings.themeMode.bgHex)
    val textSecondaryColor = Color(settings.themeMode.textSecondaryHex)

    // Handle back button on home screen
    BackHandler(
        enabled = uiState.isSearchActive ||
                uiState.isSettingsSheetActive ||
                uiState.isYouTubeGateActive ||
                uiState.pendingFrictionApp != null ||
                uiState.selectedAppForMenu != null
    ) {
        when {
            uiState.isYouTubeGateActive -> viewModel.setYouTubeGateActive(false)
            uiState.pendingFrictionApp != null -> viewModel.dismissFriction()
            uiState.selectedAppForMenu != null -> viewModel.closeAppContextMenu()
            uiState.isSettingsSheetActive -> viewModel.setSettingsSheetActive(false)
            uiState.isSearchActive -> viewModel.setSearchActive(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Section: Header + Discreet Settings icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AtAGlanceHeader(
                    systemStatus = uiState.systemStatus,
                    settings = settings,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { viewModel.setSettingsSheetActive(true) },
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Still Settings",
                        tint = textSecondaryColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Middle Section: User-Selected Favorite Applications
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (uiState.favoriteApps.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No favorite applications yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondaryColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the search bar below to choose applications.",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondaryColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.favoriteApps,
                        key = { it.uniqueKey }
                    ) { app ->
                        AppRowItem(
                            app = app,
                            settings = settings,
                            onClick = { viewModel.requestLaunchApp(app) },
                            onLongClick = { viewModel.openAppContextMenu(app) }
                        )
                    }
                }
            }

            // 3. Bottom Section: Clearly discoverable search trigger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(settings.themeMode.surfaceHex),
                    border = BorderStroke(1.dp, Color(0x18FFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { viewModel.setSearchActive(true) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = textSecondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search apps...",
                            color = textSecondaryColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // ==========================================
        // OVERLAYS
        // ==========================================

        // 1. Application Search Screen
        AppSearchOverlay(
            visible = uiState.isSearchActive,
            query = uiState.searchQuery,
            filteredApps = uiState.filteredApps,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onDismiss = { viewModel.setSearchActive(false) },
            onAppClick = { viewModel.requestLaunchApp(it) },
            onAppLongClick = { viewModel.openAppContextMenu(it) },
            settings = settings
        )

        // 2. Settings Screen
        if (uiState.isSettingsSheetActive) {
            LauncherSettingsScreen(
                viewModel = viewModel,
                onDismissRequest = { viewModel.setSettingsSheetActive(false) }
            )
        }

        // 3. Built-in Intentional YouTube Link Gate
        if (uiState.isYouTubeGateActive) {
            YouTubeGateScreen(
                settings = settings,
                historyList = uiState.youtubeHistory,
                onSaveHistory = { viewModel.saveYouTubeHistory(it) },
                onRemoveHistoryItem = { viewModel.removeYouTubeHistoryItem(it) },
                onClearHistory = { viewModel.clearYouTubeHistory() },
                onDismissRequest = { viewModel.setYouTubeGateActive(false) }
            )
        }

        // 4. Mindful App Launch Friction / Focus Barrier
        if (uiState.pendingFrictionApp != null && uiState.pendingFrictionType != null) {
            AppLaunchFrictionScreen(
                app = uiState.pendingFrictionApp!!,
                settings = settings,
                frictionType = uiState.pendingFrictionType!!,
                onProceedLaunch = { viewModel.confirmFrictionLaunch() },
                onCancel = { viewModel.dismissFriction() }
            )
        }

        // 5. App Context Options Sheet
        if (uiState.selectedAppForMenu != null) {
            AppContextMenuSheet(
                app = uiState.selectedAppForMenu!!,
                favoritesCount = uiState.favoriteApps.size,
                onDismissRequest = { viewModel.closeAppContextMenu() },
                onToggleFavorite = { viewModel.addFavorite(it) },
                onMoveFavoriteUp = { viewModel.moveFavorite(it, -1) },
                onMoveFavoriteDown = { viewModel.moveFavorite(it, 1) },
                onOpenAppInfo = { viewModel.openAppDetails(it) },
                onUninstall = { viewModel.requestUninstall(it) },
                onSetLaunchRule = { targetApp, rule -> viewModel.setAppLaunchRule(targetApp, rule) },
                onOpenYouTubeGate = { viewModel.setYouTubeGateActive(true) }
            )
        }
    }
}
