package com.niagaraclone.launcher.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.niagaraclone.launcher.MainActivity
import com.niagaraclone.launcher.R
import com.niagaraclone.launcher.ui.MindfulTimerOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class ActiveTimerSession(
    val packageName: String,
    val appLabel: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val endTimeRealtime: Long,
    val targetWallClockMs: Long,
    val startTimeMs: Long = System.currentTimeMillis()
)

class AppTimerManager private constructor(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var countdownJob: Job? = null

    private val _activeSession = MutableStateFlow<ActiveTimerSession?>(null)
    val activeSession: StateFlow<ActiveTimerSession?> = _activeSession.asStateFlow()

    // Cooldown map: packageName -> timestamp in ms when cooldown expires
    private val cooldownMap = ConcurrentHashMap<String, Long>()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mindful App Usage Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows active countdown and alerts when planned app usage time has expired"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun openOverlaySettings(ctx: Context = context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
            }
        }
    }

    fun isAppInCooldown(packageName: String): Boolean {
        val expiry = cooldownMap[packageName] ?: return false
        val now = System.currentTimeMillis()
        return if (now < expiry) {
            true
        } else {
            cooldownMap.remove(packageName)
            false
        }
    }

    fun getRemainingCooldownSeconds(packageName: String): Int {
        val expiry = cooldownMap[packageName] ?: return 0
        val remainingMs = expiry - System.currentTimeMillis()
        return if (remainingMs > 0) (remainingMs / 1000).toInt() else 0
    }

    fun setAppCooldown(packageName: String, cooldownMinutes: Int = 10) {
        cooldownMap[packageName] = System.currentTimeMillis() + (cooldownMinutes * 60 * 1000L)
    }

    fun clearAppCooldown(packageName: String) {
        cooldownMap.remove(packageName)
    }

    fun startTimer(packageName: String, appLabel: String, minutes: Int) {
        stopTimer()
        val totalSeconds = (minutes * 60).coerceAtLeast(30)
        val nowRealtime = SystemClock.elapsedRealtime()
        val endTimeRealtime = nowRealtime + (totalSeconds * 1000L)
        val targetWallClockMs = System.currentTimeMillis() + (totalSeconds * 1000L)

        val session = ActiveTimerSession(
            packageName = packageName,
            appLabel = appLabel,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            endTimeRealtime = endTimeRealtime,
            targetWallClockMs = targetWallClockMs
        )
        _activeSession.value = session
        postProgressNotification(session)
        scheduleAlarm(packageName, appLabel, totalSeconds, endTimeRealtime)

        countdownJob = scope.launch {
            while (true) {
                delay(1000L)
                val remaining = ((endTimeRealtime - SystemClock.elapsedRealtime()) / 1000L).toInt().coerceAtLeast(0)
                val updated = session.copy(remainingSeconds = remaining)
                _activeSession.value = updated

                if (remaining <= 0) {
                    onTimerExpired(updated)
                    break
                }
            }
        }
    }

    fun extendTimer(additionalSeconds: Int = 120) {
        val current = _activeSession.value ?: return
        val additionalMs = additionalSeconds * 1000L
        val newEndTimeRealtime = current.endTimeRealtime + additionalMs
        val newTargetWallClockMs = current.targetWallClockMs + additionalMs
        val newTotalSeconds = current.totalSeconds + additionalSeconds
        val newRemaining = ((newEndTimeRealtime - SystemClock.elapsedRealtime()) / 1000L).toInt().coerceAtLeast(0)

        val updated = current.copy(
            totalSeconds = newTotalSeconds,
            remainingSeconds = newRemaining,
            endTimeRealtime = newEndTimeRealtime,
            targetWallClockMs = newTargetWallClockMs
        )
        _activeSession.value = updated
        postProgressNotification(updated)
        scheduleAlarm(updated.packageName, updated.appLabel, updated.totalSeconds, newEndTimeRealtime)

        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (true) {
                delay(1000L)
                val remaining = ((newEndTimeRealtime - SystemClock.elapsedRealtime()) / 1000L).toInt().coerceAtLeast(0)
                val s = updated.copy(remainingSeconds = remaining)
                _activeSession.value = s

                if (remaining <= 0) {
                    onTimerExpired(s)
                    break
                }
            }
        }
    }

    fun stopTimer() {
        countdownJob?.cancel()
        countdownJob = null
        cancelAlarm()
        _activeSession.value = null
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(EXPIRED_NOTIFICATION_ID)
    }

    fun onAlarmExpired(packageName: String, appLabel: String, totalSeconds: Int) {
        val current = _activeSession.value
        val session = if (current != null && current.packageName == packageName) {
            current.copy(remainingSeconds = 0)
        } else {
            ActiveTimerSession(
                packageName = packageName,
                appLabel = appLabel,
                totalSeconds = totalSeconds,
                remainingSeconds = 0,
                endTimeRealtime = SystemClock.elapsedRealtime(),
                targetWallClockMs = System.currentTimeMillis()
            )
        }
        onTimerExpired(session)
    }

    private fun onTimerExpired(session: ActiveTimerSession) {
        countdownJob?.cancel()
        countdownJob = null
        cancelAlarm()
        _activeSession.value = null
        notificationManager.cancel(NOTIFICATION_ID)

        // Set 10-minute cooldown
        setAppCooldown(session.packageName, 10)

        // Trigger gentle mindful vibration
        triggerVibration()

        // Post high-priority alert notification
        postExpiredNotification(session)

        // Launch full-screen overlay over the distracting app
        try {
            val intent = Intent(context, MindfulTimerOverlayActivity::class.java).apply {
                putExtra(MindfulTimerOverlayActivity.EXTRA_PACKAGE_NAME, session.packageName)
                putExtra(MindfulTimerOverlayActivity.EXTRA_APP_LABEL, session.appLabel)
                putExtra(MindfulTimerOverlayActivity.EXTRA_INTENDED_MINUTES, (session.totalSeconds / 60).coerceAtLeast(1))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppTimerManager", "Failed to launch MindfulTimerOverlayActivity", e)
        }
    }

    private fun scheduleAlarm(packageName: String, appLabel: String, totalSeconds: Int, triggerAtRealtimeMs: Long) {
        try {
            val alarmIntent = Intent(context, AppTimerReceiver::class.java).apply {
                action = AppTimerReceiver.ACTION_TIMER_EXPIRED
                putExtra(AppTimerReceiver.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AppTimerReceiver.EXTRA_APP_LABEL, appLabel)
                putExtra(AppTimerReceiver.EXTRA_TOTAL_SECONDS, totalSeconds)
            }
            val pendingAlarm = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_CODE,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtRealtimeMs,
                    pendingAlarm
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtRealtimeMs,
                    pendingAlarm
                )
            }
        } catch (e: Exception) {
            Log.e("AppTimerManager", "Failed to schedule exact alarm", e)
        }
    }

    private fun cancelAlarm() {
        try {
            val alarmIntent = Intent(context, AppTimerReceiver::class.java).apply {
                action = AppTimerReceiver.ACTION_TIMER_EXPIRED
            }
            val pendingAlarm = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_CODE,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingAlarm)
        } catch (_: Exception) {}
    }

    private fun postProgressNotification(session: ActiveTimerSession) {
        if (!hasNotificationPermission()) return

        val returnIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            returnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val finishIntent = Intent(context, AppTimerReceiver::class.java).apply {
            action = AppTimerReceiver.ACTION_FINISH_TIMER
        }
        val finishPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            finishIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Native Chronometer Countdown: Android SystemUI ticks down live second-by-second!
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Still • ${session.appLabel}")
            .setContentText("Mindful timer in progress (${session.totalSeconds / 60}m intended)")
            .setWhen(session.targetWallClockMs)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Finish Now", finishPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun postExpiredNotification(session: ActiveTimerSession) {
        if (!hasNotificationPermission()) return

        val returnIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            2,
            returnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(context, MindfulTimerOverlayActivity::class.java).apply {
            putExtra(MindfulTimerOverlayActivity.EXTRA_PACKAGE_NAME, session.packageName)
            putExtra(MindfulTimerOverlayActivity.EXTRA_APP_LABEL, session.appLabel)
            putExtra(MindfulTimerOverlayActivity.EXTRA_INTENDED_MINUTES, (session.totalSeconds / 60).coerceAtLeast(1))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            3,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time is up: ${session.appLabel}")
            .setContentText("You intended to use ${session.appLabel} for ${(session.totalSeconds / 60).coerceAtLeast(1)}m. Tap to return to Still.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 600))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_launcher_foreground, "Close App", contentPendingIntent)
            .build()

        notificationManager.notify(EXPIRED_NOTIFICATION_ID, notification)
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val CHANNEL_ID = "mindful_app_timer_channel"
        const val NOTIFICATION_ID = 2001
        const val EXPIRED_NOTIFICATION_ID = 2002
        const val ALARM_REQ_CODE = 3001

        @Volatile
        private var instance: AppTimerManager? = null

        fun getInstance(context: Context): AppTimerManager {
            return instance ?: synchronized(this) {
                instance ?: AppTimerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
