package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.data.model.AppLaunchRule
import com.niagaraclone.launcher.ui.theme.DangerRed
import com.niagaraclone.launcher.ui.theme.TextMuted
import com.niagaraclone.launcher.ui.theme.TextPrimary
import com.niagaraclone.launcher.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContextMenuSheet(
    app: AppInfo,
    favoritesCount: Int,
    onDismissRequest: () -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onMoveFavoriteUp: (AppInfo) -> Unit,
    onMoveFavoriteDown: (AppInfo) -> Unit,
    onOpenAppInfo: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,
    onSetLaunchRule: (AppInfo, AppLaunchRule) -> Unit = { _, _ -> },
    onOpenYouTubeGate: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRuleSelector by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF141416),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp)
        ) {
            // App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleMonochromeAppIcon(
                    drawable = app.icon,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (app.isFavorite) "Favorite • ${app.launchRule.label}" else app.launchRule.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(10.dp))

            // Special YouTube Option
            if (app.isYouTube) {
                ActionItem(
                    icon = Icons.Outlined.PlayArrow,
                    title = "Open Intentional YouTube Gate",
                    onClick = {
                        onDismissRequest()
                        onOpenYouTubeGate()
                    }
                )
            }

            // Launch Rule Selector
            ActionItem(
                icon = Icons.Outlined.SelfImprovement,
                title = "Launch Restriction: ${app.launchRule.label}",
                onClick = { showRuleSelector = !showRuleSelector }
            )

            if (showRuleSelector) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(8.dp)
                ) {
                    AppLaunchRule.values().forEach { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (app.launchRule == rule) Color(0x334E8CFF) else Color.Transparent)
                                .clickable {
                                    onSetLaunchRule(app, rule)
                                    showRuleSelector = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = rule.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = rule.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Action: Favorite Toggle
            ActionItem(
                icon = if (app.isFavorite) Icons.Outlined.StarOutline else Icons.Outlined.Star,
                title = if (app.isFavorite) "Remove from Favorites" else "Add to Favorites",
                onClick = { onToggleFavorite(app) }
            )

            // Reorder actions: Move Up & Move Down
            if (app.isFavorite) {
                if (app.favoriteRank > 0) {
                    ActionItem(
                        icon = Icons.Outlined.ArrowUpward,
                        title = "Move Up in Favorites",
                        onClick = { onMoveFavoriteUp(app) }
                    )
                }
                if (app.favoriteRank in 0 until (favoritesCount - 1)) {
                    ActionItem(
                        icon = Icons.Outlined.ArrowDownward,
                        title = "Move Down in Favorites",
                        onClick = { onMoveFavoriteDown(app) }
                    )
                }
            }

            // Action: System App Info
            ActionItem(
                icon = Icons.Outlined.Info,
                title = "System App Info",
                onClick = { onOpenAppInfo(app) }
            )

            // Action: Uninstall
            ActionItem(
                icon = Icons.Outlined.Delete,
                iconTint = DangerRed,
                title = "Uninstall",
                titleColor = DangerRed,
                onClick = { onUninstall(app) }
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = TextSecondary,
    titleColor: Color = TextPrimary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = titleColor
        )
    }
}
