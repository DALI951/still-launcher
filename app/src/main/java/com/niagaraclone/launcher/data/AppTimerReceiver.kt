package com.niagaraclone.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AppTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("AppTimerReceiver", "Received broadcast: $action")

        val timerManager = AppTimerManager.getInstance(context)

        when (action) {
            ACTION_TIMER_EXPIRED -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
                val appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: "Application"
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 300)
                timerManager.onAlarmExpired(packageName, appLabel, totalSeconds)
            }
            ACTION_FINISH_TIMER -> {
                timerManager.stopTimer()
            }
        }
    }

    companion object {
        const val ACTION_TIMER_EXPIRED = "com.niagaraclone.launcher.ACTION_TIMER_EXPIRED"
        const val ACTION_FINISH_TIMER = "com.niagaraclone.launcher.ACTION_FINISH_TIMER"

        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_TOTAL_SECONDS = "extra_total_seconds"
    }
}
