package com.apnalauncher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One cell on the home screen: app shortcut, folder or widget. */
@Entity(tableName = "home_items")
data class HomeItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val page: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    val type: String = TYPE_APP,       // app | folder | widget
    val value: String = "",            // component key / folder id / widget id
    val label: String? = null,
    val spanX: Int = 1,
    val spanY: Int = 1,
) {
    companion object {
        const val TYPE_APP = "app"
        const val TYPE_FOLDER = "folder"
        const val TYPE_WIDGET = "widget"
    }
}

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String,
    val name: String,
)

@Entity(tableName = "folder_apps", primaryKeys = ["folderId", "key"])
data class FolderApp(val folderId: String, val key: String)

@Entity(tableName = "hidden_apps")
data class HiddenApp(@PrimaryKey val key: String)

@Entity(tableName = "locked_apps")
data class LockedApp(@PrimaryKey val key: String)

/** Per-app icon and label overrides. */
@Entity(tableName = "app_custom")
data class AppCustom(
    @PrimaryKey val key: String,
    val label: String? = null,
    val iconPackage: String? = null,
    val iconRes: String? = null,
)
