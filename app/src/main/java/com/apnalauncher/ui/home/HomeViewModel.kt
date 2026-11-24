package com.apnalauncher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apnalauncher.ApnaLauncherApp
import com.apnalauncher.data.AppInfo
import com.apnalauncher.data.AppCustom
import com.apnalauncher.data.Folder
import com.apnalauncher.data.HomeItem
import com.apnalauncher.data.LauncherSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** State and actions for the home screen (pages, dock, folders, drag & drop). */
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as ApnaLauncherApp).repo

    val settings: StateFlow<LauncherSettings> = repo.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())

    val homeItems: StateFlow<List<HomeItem>> = repo.dao.homeItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<List<Folder>> = repo.dao.folders()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val page = MutableStateFlow(0)
    val editMode = MutableStateFlow(false)
    val drawerOpen = MutableStateFlow(false)
    val notificationsOpen = MutableStateFlow(false)
    val settingsOpen = MutableStateFlow(false)
    val openFolder = MutableStateFlow<String?>(null)
    val widgetPickerOpen = MutableStateFlow(false)

    fun toggleEdit() { editMode.value = !editMode.value }

    fun addApp(appInfo: AppInfo, x: Int, y: Int, targetPage: Int = page.value) = viewModelScope.launch {
        repo.dao.saveHomeItem(
            HomeItem(page = targetPage, x = x, y = y, type = HomeItem.TYPE_APP, value = appInfo.key)
        )
    }

    fun addWidget(widgetId: Int, x: Int, y: Int) = viewModelScope.launch {
        repo.dao.saveHomeItem(
            HomeItem(page = page.value, x = x, y = y, type = HomeItem.TYPE_WIDGET, value = widgetId.toString())
        )
    }

    fun moveItem(item: HomeItem, newPage: Int, x: Int, y: Int) = viewModelScope.launch {
        repo.dao.saveHomeItem(item.copy(page = newPage, x = x, y = y))
    }

    fun resizeWidget(item: HomeItem, spanX: Int, spanY: Int) = viewModelScope.launch {
        repo.dao.saveHomeItem(item.copy(spanX = spanX.coerceIn(1, 4), spanY = spanY.coerceIn(1, 4)))
    }

    fun removeItem(item: HomeItem) = viewModelScope.launch {
        repo.dao.deleteHomeItem(item.id)
    }

    /** Drop an app on another app: create a folder containing both. */
    fun createFolder(dragged: AppInfo, target: HomeItem) = viewModelScope.launch {
        val id = "folder_${System.currentTimeMillis()}"
        repo.dao.saveFolder(Folder(id, "New Folder"))
        repo.dao.saveFolderApp(com.apnalauncher.data.FolderApp(id, dragged.key))
        repo.dao.saveFolderApp(com.apnalauncher.data.FolderApp(id, target.value))
        repo.dao.deleteHomeItem(target.id)
        repo.dao.saveHomeItem(
            HomeItem(page = target.page, x = target.x, y = target.y,
                type = HomeItem.TYPE_FOLDER, value = id)
        )
    }

    fun addAppToFolder(folderId: String, app: AppInfo) = viewModelScope.launch {
        repo.dao.saveFolderApp(com.apnalauncher.data.FolderApp(folderId, app.key))
        repo.dao.deleteHomeItem(repo.dao.allHomeItems().firstOrNull { it.value == app.key }?.id ?: -1)
    }

    fun renameFolder(id: String, name: String) = viewModelScope.launch {
        repo.dao.saveFolder(Folder(id, name))
    }

    fun deleteFolder(id: String) = viewModelScope.launch {
        repo.dao.deleteFolder(id)
        repo.dao.deleteHomeItem(repo.dao.allHomeItems().firstOrNull { it.value == id }?.id ?: -1)
    }

    fun setDock(keys: List<String>) = viewModelScope.launch {
        repo.settings.update { it.copy(dockApps = keys.take(5)) }
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) = viewModelScope.launch {
        repo.settings.update(transform)
    }

    fun setCustomLabel(app: AppInfo, label: String) = viewModelScope.launch {
        val old = repo.customs.value[app.key]
        repo.dao.saveCustom(old?.copy(label = label) ?: AppCustom(app.key, label = label))
    }

    fun setCustomIcon(app: AppInfo, iconPackage: String, iconRes: String) = viewModelScope.launch {
        val old = repo.customs.value[app.key]
        repo.dao.saveCustom(old?.copy(iconPackage = iconPackage, iconRes = iconRes)
            ?: AppCustom(app.key, iconPackage = iconPackage, iconRes = iconRes))
        repo.icons.clear()
    }

    fun clearCustom(app: AppInfo) = viewModelScope.launch { repo.dao.deleteCustom(app.key) }
}
