package com.niagaraclone.launcher.data

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class LauncherPermissionsState(
    val canInstallPackages: Boolean = false,
    val hasNotifications: Boolean = false,
    val isDefaultLauncher: Boolean = false
) {
    val allEssentialGranted: Boolean
        get() = canInstallPackages && isDefaultLauncher
}

class PermissionsManager(private val context: Context) {

    private val roleManager: RoleManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.getSystemService(RoleManager::class.java)
    } else null

    fun checkPermissionsState(): LauncherPermissionsState {
        val canInstall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val isDefault = isCurrentDefaultLauncher()

        return LauncherPermissionsState(
            canInstallPackages = canInstall,
            hasNotifications = hasNotifications,
            isDefaultLauncher = isDefault
        )
    }

    fun isCurrentDefaultLauncher(): Boolean {
        // Method 1: Check RoleManager on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                return true
            }
        }

        // Method 2: Check current resolved activity for HOME category
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val currentPackage = resolveInfo?.activityInfo?.packageName
        return currentPackage == context.packageName
    }

    /**
     * Creates the Android 10+ RoleManager Intent to request the ROLE_HOME.
     * MUST be launched with startActivityForResult / ActivityResultLauncher to avoid RequestRoleActivity aborting!
     */
    fun createHomeRoleIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                return try {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }

    /**
     * Fallback to direct Android System Home Settings or launcher chooser.
     */
    fun openHomeSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val chooser = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (ignored: Exception) {
                openAppSettings(context)
            }
        }
    }

    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                openAppSettings(context)
            }
        }
    }

    fun openNotificationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                openAppSettings(context)
            }
        } catch (e: Exception) {
            openAppSettings(context)
        }
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (ignored: Exception) {}
    }
}
