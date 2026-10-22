package com.niagaraclone.launcher.data

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import com.niagaraclone.launcher.data.model.SystemStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemStatusProvider(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    val systemStatusFlow: Flow<SystemStatus> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                trySend(queryCurrentStatus())
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }

        context.registerReceiver(receiver, filter)
        // Initial query
        trySend(queryCurrentStatus())

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore receiver not registered if closed already
            }
        }
    }.conflate()

    fun queryCurrentStatus(): SystemStatus {
        val now = Date()

        // System time & date
        val is24Hour = DateFormat.is24HourFormat(context)
        val timePattern = if (is24Hour) "HH:mm" else "h:mm"
        val timeFormatter = SimpleDateFormat(timePattern, Locale.getDefault())
        val timeText = timeFormatter.format(now)

        val dateFormatter = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val dateText = dateFormatter.format(now)

        // Battery
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevel = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            -1
        }

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        // Next Alarm
        val nextAlarmText = alarmManager?.nextAlarmClock?.triggerTime?.let { triggerTime ->
            if (triggerTime > System.currentTimeMillis()) {
                val alarmDate = Date(triggerTime)
                val alarmFormat = SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
                alarmFormat.format(alarmDate)
            } else null
        }

        return SystemStatus(
            timeText = timeText,
            dateText = dateText,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            nextAlarm = nextAlarmText,
            weatherSummary = null
        )
    }
}
