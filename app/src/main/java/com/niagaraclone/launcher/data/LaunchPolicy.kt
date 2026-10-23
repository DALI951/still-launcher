package com.niagaraclone.launcher.data

/**
 * Pure decision logic for app launch policies and daily usage restrictions.
 */
object LaunchPolicy {
    /**
     * Determines whether an application has reached or exceeded its daily usage limit.
     *
     * @param usedMinutes Daily usage time in minutes today (local time). Negative values (< 0) indicate
     *                    unknown stats or missing usage access permissions, which fail open (returns false).
     * @param limitMinutes Configured daily usage limit in minutes. If <= 0, no limit is enforced (returns false).
     * @return true if usedMinutes >= limitMinutes (when usedMinutes >= 0 and limitMinutes > 0), false otherwise.
     */
    fun isOverDailyLimit(usedMinutes: Int, limitMinutes: Int): Boolean {
        if (usedMinutes < 0) return false
        if (limitMinutes <= 0) return false
        return usedMinutes >= limitMinutes
    }
}
