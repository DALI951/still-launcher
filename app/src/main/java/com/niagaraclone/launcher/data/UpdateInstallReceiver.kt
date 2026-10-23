package com.niagaraclone.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "No message"
        Log.d("UpdateInstallReceiver", "PackageInstaller status: $status ($message)")

        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i("UpdateInstallReceiver", "Update installed successfully!")
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                }
            }
            else -> {
                Log.w("UpdateInstallReceiver", "PackageInstaller returned non-success: $status ($message)")
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.niagaraclone.launcher.ACTION_INSTALL_STATUS"
    }
}
