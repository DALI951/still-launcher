package com.niagaraclone.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.niagaraclone.launcher.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    /**
     * Live flow of installed launcher applications.
     * Updates automatically on package events.
     * Dispatches all package query and icon decoding operations on Dispatchers.IO.
     */
    val installedAppsFlow: Flow<List<AppInfo>> = callbackFlow {
        val handler = Handler(Looper.getMainLooper())

        // Initial emission on IO dispatcher
        launch(Dispatchers.IO) {
            trySend(loadInstalledApps())
        }

        fun reload() {
            launch(Dispatchers.IO) {
                trySend(loadInstalledApps())
            }
        }

        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                reload()
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                reload()
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                reload()
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean
            ) {
                reload()
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean
            ) {
                reload()
            }

            override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) {
                reload()
            }

            override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) {
                reload()
            }
        }

        launcherApps.registerCallback(callback, handler)

        awaitClose {
            launcherApps.unregisterCallback(callback)
        }
    }.flowOn(Dispatchers.IO).conflate()

    /**
     * Loads all launcher activities across all user profiles (personal, work, etc.).
     * Executed off the main thread to ensure no UI jank while decoding icons.
     */
    fun loadInstalledApps(): List<AppInfo> {
        val appList = mutableListOf<AppInfo>()
        val currentPackageName = context.packageName

        try {
            val profiles = userManager.userProfiles
            for (userHandle in profiles) {
                val activities: List<LauncherActivityInfo> =
                    launcherApps.getActivityList(null, userHandle)

                for (activity in activities) {
                    val packageName = activity.applicationInfo.packageName
                    // Exclude Niagara Launcher itself from the app list
                    if (packageName == currentPackageName) {
                        continue
                    }

                    val label = activity.label?.toString() ?: packageName
                    val componentName = activity.componentName.className
                    val icon = try {
                        activity.getBadgedIcon(0)
                    } catch (e: Exception) {
                        Log.w("AppRepository", "Failed to get icon for $packageName", e)
                        null
                    }

                    appList.add(
                        AppInfo(
                            label = label,
                            packageName = packageName,
                            componentName = componentName,
                            userHandle = userHandle,
                            icon = icon,
                            sectionLetter = AppInfo.computeSectionLetter(label)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error retrieving installed apps", e)
        }

        // Sort alphabetically case-insensitively
        return appList.sortedWith(
            compareBy<AppInfo> {
                val letter = it.sectionLetter
                if (letter == '#') 1 else 0
            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        )
    }

    /**
     * Launches the main activity for the specified app.
     */
    fun launchApp(appInfo: AppInfo): Boolean {
        return try {
            val component = ComponentName(appInfo.packageName, appInfo.componentName)
            launcherApps.startMainActivity(
                component,
                appInfo.userHandle,
                null,
                null
            )
            true
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to launch app ${appInfo.packageName}", e)
            false
        }
    }

    /**
     * Opens the system App Details / App Info screen.
     */
    fun openAppDetails(appInfo: AppInfo) {
        try {
            val component = ComponentName(appInfo.packageName, appInfo.componentName)
            launcherApps.startAppDetailsActivity(
                component,
                appInfo.userHandle,
                null,
                null
            )
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to open app details for ${appInfo.packageName}", e)
            // Fallback via Intent
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${appInfo.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Launches the system uninstallation dialog.
     */
    fun requestUninstall(appInfo: AppInfo) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${appInfo.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to request uninstall for ${appInfo.packageName}", e)
        }
    }
}
