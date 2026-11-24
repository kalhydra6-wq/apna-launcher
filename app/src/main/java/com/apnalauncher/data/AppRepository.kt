package com.apnalauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.apnalauncher.icons.IconCache
import com.apnalauncher.icons.IconPackManager

/** Loads installed apps and merges them with Room/DataStore user state. */
class AppRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pm: PackageManager = context.packageManager
    val dao = LauncherDatabase.get(context).dao()
    val settings = SettingsDataStore(context)
    val packs = IconPackManager(context)
    val icons = IconCache(context, packs)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    val hiddenKeys: StateFlow<Set<String>> = dao.hiddenApps()
        .map { list -> list.map { it.key }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val lockedKeys: StateFlow<Set<String>> = dao.lockedApps()
        .map { list -> list.map { it.key }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val customs: StateFlow<Map<String, AppCustom>> = dao.customs()
        .map { list -> list.associateBy { it.key } }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    val visibleApps: StateFlow<List<AppInfo>> = combine(apps, hiddenKeys) { all, hidden ->
        all.filterNot { it.key in hidden }.sortedBy { it.label.lowercase() }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        refreshApps()
    }

    /** Re-queries the package manager after install / uninstall / update. */
    fun refreshApps() = scope.launch {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val items = resolved.mapNotNull { info ->
            val label = info.activityInfo.loadLabel(pm)?.toString()?.trim().orEmpty()
            if (label.isBlank()) null else AppInfo(
                packageName = info.activityInfo.packageName,
                className = info.activityInfo.name,
                label = label,
                isSystem = (info.activityInfo.applicationInfo.flags and
                    android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
            )
        }.distinctBy { it.key }.sortedBy { it.label.lowercase() }
        _apps.value = items
        icons.clear()
    }

    fun appByKey(key: String): AppInfo? = _apps.value.firstOrNull { it.key == key }

    fun launch(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(android.content.ComponentName(app.packageName, app.className))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    suspend fun toggleHidden(app: AppInfo) {
        if (app.key in hiddenKeys.value) dao.unhideApp(app.key) else dao.hideApp(HiddenApp(app.key))
    }

    suspend fun toggleLocked(app: AppInfo) {
        if (app.key in lockedKeys.value) dao.unlockApp(app.key) else dao.lockApp(LockedApp(app.key))
    }

    suspend fun renameApp(app: AppInfo, label: String) {
        val old = customs.value[app.key]
        dao.saveCustom(old?.copy(label = label) ?: AppCustom(app.key, label = label))
        refreshApps()
    }

    suspend fun changeIcon(app: AppInfo, iconPackage: String, iconRes: String) {
        val old = customs.value[app.key]
        dao.saveCustom(old?.copy(iconPackage = iconPackage, iconRes = iconRes)
            ?: AppCustom(app.key, iconPackage = iconPackage, iconRes = iconRes))
        icons.clear()
    }

    suspend fun resetApp(app: AppInfo) = dao.deleteCustom(app.key)
}
