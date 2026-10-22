package com.niagaraclone.launcher.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.ui.theme.DangerRed
import com.niagaraclone.launcher.ui.theme.StarYellow
import com.niagaraclone.launcher.ui.theme.SurfaceTranslucent
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAppsSheet(
    favoriteApps: List<AppInfo>,
    allApps: List<AppInfo>,
    hiddenApps: List<AppInfo>,
    onDismissRequest: () -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onMoveFavoriteUp: (AppInfo) -> Unit,
    onMoveFavoriteDown: (AppInfo) -> Unit,
    onUnhideApp: (AppInfo) -> Unit,
    settings: LauncherSettings = LauncherSettings()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val themeAccent = Color(settings.themePreset.accentHex)

    val tabs = listOf(
        "Favorites (${favoriteApps.size})",
        "Add Apps",
        "Hidden (${hiddenApps.size})"
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
                .height(560.dp)
                .padding(top = 16.dp)
        ) {
            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = themeAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = themeAccent
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
                                    fontSize = 14.sp
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
                                        settings = settings,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
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
                                    IconButton(onClick = { onToggleFavorite(app) }) {
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
                    // Add to Favorites Tab (Searchable All Apps with 1-tap toggles)
                    val query = searchQuery.trim()
                    val filtered = if (query.isEmpty()) {
                        allApps
                    } else {
                        allApps.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Input Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                                cursorBrush = SolidColor(themeAccent),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Search apps to favorite…", color = TextMuted, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp)
                        ) {
                            items(
                                items = filtered,
                                key = { "add_fav_${it.uniqueKey}" }
                            ) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleFavorite(app) }
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(
                                        drawable = app.icon,
                                        settings = settings,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = if (app.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = if (app.isFavorite) "Favorited" else "Not Favorited",
                                        tint = if (app.isFavorite) StarYellow else TextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Hidden Apps Tab
                    if (hiddenApps.isEmpty()) {
                        EmptyStateMessage(text = "No hidden apps")
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
                                        settings = settings,
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
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
                                            tint = themeAccent,
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
