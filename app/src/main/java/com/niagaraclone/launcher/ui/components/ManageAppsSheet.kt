package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.R
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.ui.theme.DangerRed
import com.niagaraclone.launcher.ui.theme.NiagaraBlue
import com.niagaraclone.launcher.ui.theme.SurfaceTranslucent
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAppsSheet(
    favoriteApps: List<AppInfo>,
    hiddenApps: List<AppInfo>,
    onDismissRequest: () -> Unit,
    onRemoveFavorite: (AppInfo) -> Unit,
    onMoveFavoriteUp: (AppInfo) -> Unit,
    onMoveFavoriteDown: (AppInfo) -> Unit,
    onUnhideApp: (AppInfo) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.manage_favorites_title),
        stringResource(R.string.hidden_apps_title)
    )

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
                .height(520.dp)
                .padding(top = 16.dp)
        ) {
            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = NiagaraBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = NiagaraBlue
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                ),
                                color = if (selectedTabIndex == index) TextPrimary else TextSecondary
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // Manage Favorites Tab
                    if (favoriteApps.isEmpty()) {
                        EmptyStateMessage(text = "No favorites pinned")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp)
                        ) {
                            itemsIndexed(
                                items = favoriteApps,
                                key = { _, app -> app.uniqueKey }
                            ) { index, app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(
                                        drawable = app.icon,
                                        modifier = Modifier.size(36.dp)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Move Up Button
                                    IconButton(
                                        onClick = { onMoveFavoriteUp(app) },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ArrowUpward,
                                            contentDescription = "Move Up",
                                            tint = if (index > 0) TextSecondary else TextMuted.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Move Down Button
                                    IconButton(
                                        onClick = { onMoveFavoriteDown(app) },
                                        enabled = index < favoriteApps.lastIndex
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ArrowDownward,
                                            contentDescription = "Move Down",
                                            tint = if (index < favoriteApps.lastIndex) TextSecondary else TextMuted.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Remove Favorite Button
                                    IconButton(onClick = { onRemoveFavorite(app) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Remove Favorite",
                                            tint = DangerRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Hidden Apps Tab
                    if (hiddenApps.isEmpty()) {
                        EmptyStateMessage(text = stringResource(R.string.no_hidden_apps))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp)
                        ) {
                            itemsIndexed(
                                items = hiddenApps,
                                key = { _, app -> app.uniqueKey }
                            ) { _, app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(
                                        drawable = app.icon,
                                        modifier = Modifier.size(36.dp)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Unhide Button
                                    IconButton(onClick = { onUnhideApp(app) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Visibility,
                                            contentDescription = "Unhide",
                                            tint = NiagaraBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}
