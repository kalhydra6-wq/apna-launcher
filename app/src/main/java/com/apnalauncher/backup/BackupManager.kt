package com.apnalauncher.backup

import android.content.Context
import android.net.Uri
import com.apnalauncher.data.*
import com.google.gson.Gson

/** JSON export / import of the full launcher layout and settings. */
data class LauncherBackup(
    val version: Int = 1,
    val settings: LauncherSettings = LauncherSettings(),
    val home: List<HomeItem> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val folderApps: List<FolderApp> = emptyList(),
    val hidden: List<HiddenApp> = emptyList(),
    val locked: List<LockedApp> = emptyList(),
    val customs: List<AppCustom> = emptyList(),
)

class BackupManager(private val context: Context, private val repo: AppRepository) {
    private val gson = Gson()

    suspend fun export(uri: Uri) {
        val backup = LauncherBackup(
            settings = repo.settings.settings.value,
            home = repo.dao.allHomeItems(),
            folders = repo.dao.allFolders(),
            folderApps = repo.dao.allFolderApps(),
            hidden = repo.dao.allHidden(),
            locked = repo.dao.allLocked(),
            customs = repo.dao.allCustoms(),
        )
        context.contentResolver.openOutputStream(uri)?.use {
            it.write(gson.toJson(backup).toByteArray())
        }
    }

    suspend fun import(uri: Uri) {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return
        val backup = gson.fromJson(json, LauncherBackup::class.java) ?: return
        repo.settings.replaceAll(backup.settings)
        repo.dao.clearHome()
        repo.dao.saveHomeItems(backup.home)
        backup.folders.forEach { repo.dao.saveFolder(it) }
        backup.folderApps.forEach { repo.dao.saveFolderApp(it) }
        backup.hidden.forEach { repo.dao.hideApp(it) }
        backup.locked.forEach { repo.dao.lockApp(it) }
        backup.customs.forEach { repo.dao.saveCustom(it) }
    }
}
