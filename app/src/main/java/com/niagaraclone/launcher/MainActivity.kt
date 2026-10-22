package com.niagaraclone.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.LauncherHomeScreen
import com.niagaraclone.launcher.ui.theme.NiagaraLauncherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissionsAndPendingInstall()
        requestNotificationPermissionIfNeeded()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionsAndPendingInstall()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge transparent system bars
        enableEdgeToEdge()

        // Priority #1: If not yet the default home launcher, ask immediately (and show onboarding dialog)
        // Do NOT launch two system dialogs simultaneously, or Android drops the second one!
        if (!viewModel.permissionsManager.isCurrentDefaultLauncher()) {
            viewModel.setDefaultPromptDialogVisible(true)
            promptSetDefaultLauncher()
        } else {
            requestNotificationPermissionIfNeeded()
        }

        setContent {
            NiagaraLauncherTheme {
                val uiState by viewModel.uiState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    LauncherHomeScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onRequestDefaultLauncher = {
                            promptSetDefaultLauncher()
                        },
                        onRequestUnknownSources = {
                            viewModel.permissionsManager.openUnknownSourcesSettings(this)
                        },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.permissionsManager.openNotificationSettings(this)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun promptSetDefaultLauncher() {
        val roleIntent = viewModel.permissionsManager.createHomeRoleIntent()
        if (roleIntent != null) {
            try {
                homeRoleLauncher.launch(roleIntent)
                return
            } catch (ignored: Exception) {}
        }
        // Fallback to system home settings or show prompt dialog
        viewModel.permissionsManager.openHomeSettings(this)
    }

    /**
     * Triggered whenever the physical/gesture Home button is pressed while Niagara is already running.
     * Returns immediately to the clean Favorites view and closes all sheets and search overlays.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.onHomeIntentReceived()
    }

    override fun onResume() {
        super.onResume()
        // Refresh permissions and resume pending APK installation if permission was granted
        viewModel.refreshPermissionsAndPendingInstall()
    }
}
