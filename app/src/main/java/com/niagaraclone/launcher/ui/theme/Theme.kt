package com.niagaraclone.launcher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NiagaraDarkColorScheme = darkColorScheme(
    primary = NiagaraBlue,
    onPrimary = Color.White,
    primaryContainer = NiagaraBlueDark,
    onPrimaryContainer = NiagaraBlueLight,
    secondary = NiagaraBlueLight,
    onSecondary = Color.Black,
    background = Color.Transparent,
    onBackground = TextPrimary,
    surface = SurfaceTranslucent,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceTranslucentLight,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun NiagaraLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = NiagaraDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                // In Niagara, dark translucent look favors light icons/text in status bar
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
