package com.niagaraclone.launcher.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.niagaraclone.launcher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkName: String,
    val apkSize: Long,
    val remoteVersionCode: Int,
    val isNewer: Boolean
)

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progressPercent: Int, val downloadedMb: Float, val totalMb: Float) : UpdateDownloadState()
    data class Complete(val apkFile: File, val releaseInfo: ReleaseInfo) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

sealed class InstallResult {
    object Success : InstallResult()
    object NeedsUnknownSourcesPermission : InstallResult()
    data class Error(val message: String) : InstallResult()
}

class AppUpdateRepository(private val context: Context) {

    private val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
    val cachedApkFile = File(updatesDir, "Still-update.apk")

    /**
     * Checks GitHub API for the latest published release.
     */
    suspend fun checkLatestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "StillLauncher-Android")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("GitHub API returned HTTP $responseCode")
                )
            }

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)

            val tagName = json.optString("tag_name", "")
            val releaseName = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "")

            val assetsArray = json.optJSONArray("assets") ?: return@withContext Result.failure(
                Exception("No assets found in latest release")
            )

            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize: Long = 0

            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    apkName = name
                    apkSize = asset.optLong("size", 0)
                    break
                }
            }

            if (apkUrl.isNullOrEmpty() || apkName == null) {
                return@withContext Result.failure(
                    Exception("No APK asset found in release $tagName")
                )
            }

            val remoteVersionCode = parseVersionCodeFromTag(tagName)
            val isNewer = isRemoteNewer(
                remoteTag = tagName,
                currentVersionName = BuildConfig.VERSION_NAME,
                currentVersionCode = BuildConfig.VERSION_CODE
            )

            Result.success(
                ReleaseInfo(
                    tagName = tagName,
                    releaseName = releaseName,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkUrl,
                    apkName = apkName,
                    apkSize = apkSize,
                    remoteVersionCode = remoteVersionCode,
                    isNewer = isNewer
                )
            )
        } catch (e: Exception) {
            Log.e("AppUpdateRepository", "Failed to check latest release", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads the APK asset with streaming progress reporting.
     * Follows HTTP 301/302/307/308 redirects across domains.
     * Verifies APK magic bytes before returning Complete.
     */
    fun downloadApk(releaseInfo: ReleaseInfo): Flow<UpdateDownloadState> = flow {
        emit(UpdateDownloadState.Downloading(0, 0f, (releaseInfo.apkSize / (1024f * 1024f))))

        val tempFile = File(updatesDir, "download_${System.currentTimeMillis()}.apk")
        try {
            var currentUrl = releaseInfo.downloadUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0

            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "StillLauncher-Android")
                    setRequestProperty("Accept", "application/octet-stream")
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val newLocation = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!newLocation.isNullOrEmpty() && redirectCount < 6) {
                        currentUrl = newLocation
                        redirectCount++
                        continue
                    }
                }
                break
            }

            val conn = connection ?: throw Exception("Failed to establish download connection")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Download server returned HTTP ${conn.responseCode}")
            }

            val totalBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else releaseInfo.apkSize
            val totalMb = (totalBytes / (1024f * 1024f))

            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead: Long = 0
                    var lastReportPercent = -1

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val percent = if (totalBytes > 0) {
                            ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }

                        if (percent != lastReportPercent) {
                            lastReportPercent = percent
                            val downloadedMb = totalRead / (1024f * 1024f)
                            emit(UpdateDownloadState.Downloading(percent, downloadedMb, totalMb))
                        }
                    }
                }
            }

            // Verify file validity: size > 50 KB and starts with zip/apk magic bytes ('P', 'K')
            if (tempFile.length() < 50_000L || !verifyApkMagicBytes(tempFile)) {
                tempFile.delete()
                emit(UpdateDownloadState.Error("Downloaded file is invalid or corrupt (failed APK verification)"))
                return@flow
            }

            // Rename to standard cached APK file path
            if (cachedApkFile.exists()) {
                cachedApkFile.delete()
            }
            tempFile.renameTo(cachedApkFile)

            emit(UpdateDownloadState.Complete(cachedApkFile, releaseInfo))
        } catch (e: Exception) {
            tempFile.delete()
            emit(UpdateDownloadState.Error("Download failed: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Attempts silent package installation via PackageInstaller (Android 12+),
     * or seamlessly falls back to standard installer view intent.
     */
    fun installApkSilentlyOrPrompt(apkFile: File): InstallResult {
        if (!apkFile.exists() || apkFile.length() < 50_000L) {
            return InstallResult.Error("APK file not found or corrupted.")
        }

        // Check Unknown Sources Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            if (!canInstall) {
                try {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return InstallResult.NeedsUnknownSourcesPermission
                } catch (e: Exception) {
                    Log.e("AppUpdateRepository", "Failed to open unknown sources settings", e)
                }
            }
        }

        // On Android 12+ (API 31+), attempt unattended silent install via PackageInstaller
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val packageInstaller = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)

                val sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)

                apkFile.inputStream().use { input ->
                    session.openWrite("still_update", 0, apkFile.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }

                val intent = Intent(context, UpdateInstallReceiver::class.java).apply {
                    action = UpdateInstallReceiver.ACTION_INSTALL_STATUS
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                session.commit(pendingIntent.intentSender)
                session.close()
                return InstallResult.Success
            } catch (e: Exception) {
                Log.w("AppUpdateRepository", "Silent install attempt failed, falling back to prompt", e)
            }
        }

        // Fallback: Launch standard package installer intent
        return installApk(apkFile)
    }

    /**
     * Checks for updates, downloads the APK if newer, and triggers installation automatically.
     */
    suspend fun autoCheckAndInstall(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val releaseResult = checkLatestRelease()
            val releaseInfo = releaseResult.getOrNull() ?: return@withContext Result.failure(
                releaseResult.exceptionOrNull() ?: Exception("Unknown release check error")
            )

            if (!releaseInfo.isNewer) {
                return@withContext Result.success(false)
            }

            Log.i("AppUpdateRepository", "New update detected (${releaseInfo.tagName}), auto-downloading...")

            var completedApk: File? = null
            downloadApk(releaseInfo).collect { state ->
                if (state is UpdateDownloadState.Complete) {
                    completedApk = state.apkFile
                } else if (state is UpdateDownloadState.Error) {
                    throw Exception(state.message)
                }
            }

            val apk = completedApk ?: throw Exception("APK download did not complete")
            Log.i("AppUpdateRepository", "Auto-download completed, triggering installation...")
            installApkSilentlyOrPrompt(apk)
            Result.success(true)
        } catch (e: Exception) {
            Log.e("AppUpdateRepository", "Auto update failed", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if the app can install unknown packages, prompts if missing,
     * or invokes the system package installer via FileProvider.
     */
    fun installApk(apkFile: File): InstallResult {
        if (!apkFile.exists() || apkFile.length() < 50_000L) {
            return InstallResult.Error("APK file not found or corrupted.")
        }

        // Check Unknown Sources Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            if (!canInstall) {
                try {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return InstallResult.NeedsUnknownSourcesPermission
                } catch (e: Exception) {
                    Log.e("AppUpdateRepository", "Failed to open unknown sources settings", e)
                }
            }
        }

        // Launch Package Installer via FileProvider
        return try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            InstallResult.Success
        } catch (e: Exception) {
            Log.e("AppUpdateRepository", "Failed to launch package installer", e)
            InstallResult.Error("Installer launch failed: ${e.localizedMessage}")
        }
    }

    private fun verifyApkMagicBytes(file: File): Boolean {
        return try {
            FileInputStream(file).use { input ->
                val b1 = input.read()
                val b2 = input.read()
                // Zip/APK files start with PK (0x50, 0x4B)
                b1 == 0x50 && b2 == 0x4B
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun parseVersionCodeFromTag(tagName: String): Int {
        val clean = tagName.removePrefix("v").trim()
        val parts = clean.split(".").mapNotNull { it.toIntOrNull() }
        val patch = parts.lastOrNull() ?: 0
        return 100 + patch
    }

    private fun isRemoteNewer(
        remoteTag: String,
        currentVersionName: String,
        currentVersionCode: Int
    ): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").trim()
        val cleanCurrent = currentVersionName.removePrefix("v").trim()

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        // Compare semantic version numbers (e.g. 1.0.5 vs 1.0.0)
        for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        // If version strings are equal, fallback to inferred remote code comparison
        val remoteCode = parseVersionCodeFromTag(remoteTag)
        return remoteCode > currentVersionCode
    }
}
