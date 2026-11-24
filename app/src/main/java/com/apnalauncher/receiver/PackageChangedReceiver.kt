package com.apnalauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apnalauncher.ApnaLauncherApp

/** Keeps the drawer in sync with installs, removals and updates. */
class PackageChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? ApnaLauncherApp ?: return
        app.repo.refreshApps()
    }
}
