package com.niagaraclone.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagaraclone.launcher.R
import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.ui.theme.DangerRed
import com.niagaraclone.launcher.ui.theme.NiagaraBlue
import com.niagaraclone.launcher.ui.theme.StarYellow
import com.niagaraclone.launcher.ui.theme.SurfaceTranslucent
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
    onToggleHide: (AppInfo) -> Unit,
    onOpenAppInfo: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .padding(vertical = 20.dp)
        ) {
            // App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    drawable = app.icon,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action: Favorite / Unfavorite
            ActionItem(
                icon = if (app.isFavorite) Icons.Outlined.StarOutline else Icons.Outlined.Star,
                iconTint = if (app.isFavorite) StarYellow else NiagaraBlue,
                title = if (app.isFavorite) {
                    stringResource(R.string.remove_from_favorites)
                } else {
                    stringResource(R.string.add_to_favorites)
                },
                onClick = { onToggleFavorite(app) }
            )

            // Reorder actions: Move Up & Move Down with strict bounds checking
            if (app.isFavorite) {
                if (app.favoriteRank > 0) {
                    ActionItem(
                        icon = Icons.Outlined.ArrowUpward,
                        title = stringResource(R.string.move_up),
                        onClick = { onMoveFavoriteUp(app) }
                    )
                }
                if (app.favoriteRank in 0 until (favoritesCount - 1)) {
                    ActionItem(
                        icon = Icons.Outlined.ArrowDownward,
                        title = stringResource(R.string.move_down),
                        onClick = { onMoveFavoriteDown(app) }
                    )
                }
            }

            // Action: Hide / Unhide
            ActionItem(
                icon = if (app.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                title = if (app.isHidden) {
                    stringResource(R.string.unhide_app)
                } else {
                    stringResource(R.string.hide_app)
                },
                onClick = { onToggleHide(app) }
            )

            // Action: System App Info
            ActionItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.app_info),
                onClick = { onOpenAppInfo(app) }
            )

            // Action: Uninstall
            ActionItem(
                icon = Icons.Outlined.Delete,
                iconTint = DangerRed,
                title = stringResource(R.string.uninstall),
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
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            color = titleColor
        )
    }
}
