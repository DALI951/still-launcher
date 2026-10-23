package com.niagaraclone.launcher.data

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyUsageSummary(
    val totalTimeMinutes: Long = 0,
    val unlockCount: Int = 0,
    val topApps: List<AppUsageDetail> = emptyList(),
    val hasUsageAccess: Boolean = false
)

data class AppUsageDetail(
    val packageName: String,
    val usageMinutes: Long
)

class ScreenTimeTracker private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("still_screen_time", Context.MODE_PRIVATE)

    init {
        registerUnlockReceiver()
    }

    private fun registerUnlockReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    incrementUnlockCount()
                }
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) {}
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun incrementUnlockCount() {
        val today = getTodayDateString()
        val savedDate = prefs.getString("unlock_date", "")
        val count = if (savedDate == today) {
            prefs.getInt("unlock_count", 0) + 1
        } else {
            1
        }
        prefs.edit()
            .putString("unlock_date", today)
            .putInt("unlock_count", count)
            .apply()
    }

    fun getTodayUnlockCount(): Int {
        val today = getTodayDateString()
        val savedDate = prefs.getString("unlock_date", "")
        return if (savedDate == today) prefs.getInt("unlock_count", 0) else 0
    }

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun openUsageAccessSettings(ctx: Context = context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun getDailyUsageSummary(): DailyUsageSummary {
        val unlocks = getTodayUnlockCount()
        val hasPermission = hasUsageStatsPermission()
        if (!hasPermission) {
            return DailyUsageSummary(
                totalTimeMinutes = 0,
                unlockCount = unlocks,
                topApps = emptyList(),
                hasUsageAccess = false
            )
        }

        return try {
            val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val stats: List<UsageStats> = usageManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: emptyList()

            var totalMs = 0L
            val appUsageList = mutableListOf<AppUsageDetail>()

            for (stat in stats) {
                val totalTimeInForeground = stat.totalTimeInForeground
                if (totalTimeInForeground > 60_000L) { // at least 1 min
                    totalMs += totalTimeInForeground
                    appUsageList.add(
                        AppUsageDetail(
                            packageName = stat.packageName,
                            usageMinutes = totalTimeInForeground / (60 * 1000L)
                        )
                    )
                }
            }

            val top3 = appUsageList.sortedByDescending { it.usageMinutes }.take(3)

            DailyUsageSummary(
                totalTimeMinutes = totalMs / (60 * 1000L),
                unlockCount = unlocks,
                topApps = top3,
                hasUsageAccess = true
            )
        } catch (_: Exception) {
            DailyUsageSummary(
                totalTimeMinutes = 0,
                unlockCount = unlocks,
                topApps = emptyList(),
                hasUsageAccess = false
            )
        }
    }

    /**
     * Retrieves today's total foreground usage minutes for a specific package name since local midnight.
     * Returns -1 if usage access permission is not granted or if an error occurs.
     */
    fun getTodayUsageMinutes(packageName: String): Int {
        if (!hasUsageStatsPermission()) {
            return -1
        }
        return try {
            val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val stats: List<UsageStats> = usageManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: emptyList()

            var totalMs = 0L
            for (stat in stats) {
                if (stat.packageName == packageName) {
                    totalMs += stat.totalTimeInForeground
                }
            }
            (totalMs / (60 * 1000L)).toInt()
        } catch (_: Exception) {
            -1
        }
    }

    companion object {
        @Volatile
        private var instance: ScreenTimeTracker? = null

        fun getInstance(context: Context): ScreenTimeTracker {
            return instance ?: synchronized(this) {
                instance ?: ScreenTimeTracker(context.applicationContext).also { instance = it }
            }
        }
    }
}
