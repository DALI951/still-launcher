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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.niagaraclone.launcher.ui.LauncherViewModel
import com.niagaraclone.launcher.ui.components.LauncherHomeScreen
import com.niagaraclone.launcher.ui.components.parseYouTubeUrl
import com.niagaraclone.launcher.ui.theme.StillLauncherTheme

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

        // Handle incoming YouTube view or share links
        handleIncomingIntent(intent)

        // Check default launcher status
        if (!viewModel.permissionsManager.isCurrentDefaultLauncher()) {
            promptSetDefaultLauncher()
        } else {
            requestNotificationPermissionIfNeeded()
        }

        setContent {
            StillLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    LauncherHomeScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        val action = intent.action
        if (action == Intent.ACTION_VIEW) {
            val dataStr = intent.dataString
            if (!dataStr.isNullOrBlank()) {
                val parsed = parseYouTubeUrl(dataStr)
                if (parsed != null) {
                    viewModel.openYouTubeVideoDirectly(parsed.first, parsed.second, dataStr)
                    return true
                }
            }
        } else if (action == Intent.ACTION_SEND && (intent.type == "text/plain" || intent.type?.startsWith("text/") == true)) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val parsed = parseYouTubeUrl(sharedText)
                if (parsed != null) {
                    viewModel.openYouTubeVideoDirectly(parsed.first, parsed.second, sharedText)
                    return true
                }
            }
        }
        return false
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
            } catch (_: Exception) {}
        }
        // Fallback to system home settings
        viewModel.permissionsManager.openHomeSettings(this)
    }

    /**
     * Triggered whenever physical or gesture Home is pressed while Still is already running,
     * or when an external intent is delivered to singleTask MainActivity.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!handleIncomingIntent(intent)) {
            viewModel.onHomeIntentReceived()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permissions and resume pending APK installation if permission was granted
        viewModel.refreshPermissionsAndPendingInstall()
    }
}
