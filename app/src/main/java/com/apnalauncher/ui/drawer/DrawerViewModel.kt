package com.apnalauncher.ui.drawer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apnalauncher.ApnaLauncherApp
import com.apnalauncher.data.AppInfo
import com.apnalauncher.data.LauncherSettings
import com.apnalauncher.ui.notifications.NotificationStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Drawer state: search, tabs, hide / lock and badge counts. */
class DrawerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as ApnaLauncherApp).repo

    val settings: StateFlow<LauncherSettings> = repo.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())
    val allApps = repo.visibleApps
    val badges = NotificationStore.badges

    val query = MutableStateFlow("")
    val tab = MutableStateFlow("All")

    val apps: StateFlow<List<AppInfo>> = combine(allApps, query, tab) { list, q, selected ->
        val filtered = if (q.isBlank()) list else list.filter {
            it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
        }
        if (selected == "All") filtered else filtered.filter { it.category == selected }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sections: StateFlow<Map<String, List<AppInfo>>> = apps.map { list ->
        list.groupBy { it.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
            .toSortedMap()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setQuery(value: String) { query.value = value }
    fun setTab(value: String) {
        tab.value = value
        viewModelScope.launch { repo.settings.update { it.copy(lastTab = value) } }
    }

    fun toggleHide(app: AppInfo) = viewModelScope.launch { repo.toggleHidden(app) }
    fun toggleLock(app: AppInfo) = viewModelScope.launch { repo.toggleLocked(app) }
}
