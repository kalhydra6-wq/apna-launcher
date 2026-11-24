package com.apnalauncher.ui.common

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutQuery
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.apnalauncher.data.AppInfo

/** Launcher-wide helpers for app actions. */
object AppActions {

    fun launch(activity: FragmentActivity, app: AppInfo, locked: Boolean) {
        if (!locked) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(android.content.ComponentName(app.packageName, app.className))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { activity.startActivity(intent) }
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(android.content.ComponentName(app.packageName, app.className))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { activity.startActivity(intent) }
                }
            })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock ${app.label}")
                .setSubtitle("Biometric required for locked apps")
                .setNegativeButtonText("Cancel")
                .build()
        )
    }

    fun appInfo(context: Context, app: AppInfo) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun uninstallIntent(app: AppInfo): Intent =
        Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:${app.packageName}"))
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)

    /** Dynamic / static / pinned shortcuts exposed by LauncherApps. */
    fun shortcuts(context: Context, app: AppInfo): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val query = ShortcutQuery.Builder()
                .setQueryFlags(ShortcutQuery.FLAG_MATCH_DYNAMIC or ShortcutQuery.FLAG_MATCH_STATIC or
                    ShortcutQuery.FLAG_MATCH_PINNED)
                .setActivity(android.content.ComponentName(app.packageName, app.className))
                .build()
            launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun startShortcut(context: Context, shortcut: ShortcutInfo) {
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.startShortcut(shortcut.`package`, shortcut.id, Rect(), null, Process.myUserHandle())
        } catch (_: Exception) {
        }
    }
}
