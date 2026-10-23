package com.niagaraclone.launcher.services

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Lightweight AccessibilityService enabling double-tap to lock screen without Device Administrator overhead.
 */
class StillAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event inspection needed; used purely for system global action lock screen
    }

    override fun onInterrupt() {
        // No interrupt action needed
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instanceRef?.get() == this) {
            instanceRef = null
        }
    }

    companion object {
        private var instanceRef: WeakReference<StillAccessibilityService>? = null

        /**
         * Returns true if the Still accessibility service is actively running and connected.
         */
        fun isEnabled(): Boolean {
            return instanceRef?.get() != null
        }

        /**
         * Locks the screen using performGlobalAction on Android 9+ (API 28+).
         * Returns true if action was successfully dispatched.
         */
        fun lockScreen(): Boolean {
            val service = instanceRef?.get() ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
        }
    }
}
