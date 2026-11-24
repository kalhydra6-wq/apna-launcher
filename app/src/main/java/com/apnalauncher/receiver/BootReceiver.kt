package com.apnalauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apnalauncher.ApnaLauncherApp

/** Refreshes the app list after reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as? ApnaLauncherApp)?.repo?.refreshApps()
    }
}
