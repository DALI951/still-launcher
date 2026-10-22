package com.niagaraclone.launcher.data.model

/**
 * Encapsulates glanceable system status information displayed on the home screen header.
 */
data class SystemStatus(
    val timeText: String = "",
    val dateText: String = "",
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val nextAlarm: String? = null,
    val weatherSummary: String? = null
)
