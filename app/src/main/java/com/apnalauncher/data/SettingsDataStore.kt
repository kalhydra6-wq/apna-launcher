package com.apnalauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

/** All user preferences in one immutable snapshot. */
data class LauncherSettings(
    val themeMode: String = "system",          // light | dark | system
    val builtInTheme: String = "default",      // default | amoled | glass | neon | minimal
    val accentColor: Int = 0xFF3B6EF5.toInt(),
    val dynamicColor: Boolean = true,
    val homeRows: Int = 5,
    val homeCols: Int = 4,
    val drawerCols: Int = 4,
    val dockApps: List<String> = emptyList(),
    val iconShape: String = "squircle",        // circle | squircle | square | rounded | teardrop
    val iconSize: Float = 1.0f,
    val labelSize: Float = 1.0f,
    val showLabels: Boolean = true,
    val iconPack: String = "",
    val swipeUpDrawer: Boolean = true,
    val doubleTapLock: Boolean = true,
    val swipeDownNotifications: Boolean = true,
    val pinchEdit: Boolean = true,
    val badgesEnabled: Boolean = true,
    val blurDock: Boolean = false,
    val blurDrawer: Boolean = false,
    val fontChoice: String = "sans",           // sans | serif | monospace
    val animationSpeed: Float = 1.0f,
    val tabsEnabled: Boolean = true,
    val lastTab: String = "All",
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BUILT_IN = stringPreferencesKey("built_in_theme")
        val ACCENT = intPreferencesKey("accent_color")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val HOME_ROWS = intPreferencesKey("home_rows")
        val HOME_COLS = intPreferencesKey("home_cols")
        val DRAWER_COLS = intPreferencesKey("drawer_cols")
        val DOCK = stringSetPreferencesKey("dock_apps")
        val SHAPE = stringPreferencesKey("icon_shape")
        val ICON_SIZE = floatPreferencesKey("icon_size")
        val LABEL_SIZE = floatPreferencesKey("label_size")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val ICON_PACK = stringPreferencesKey("icon_pack")
        val SWIPE_UP = booleanPreferencesKey("swipe_up")
        val DOUBLE_TAP = booleanPreferencesKey("double_tap")
        val SWIPE_DOWN = booleanPreferencesKey("swipe_down")
        val PINCH = booleanPreferencesKey("pinch")
        val BADGES = booleanPreferencesKey("badges")
        val BLUR_DOCK = booleanPreferencesKey("blur_dock")
        val BLUR_DRAWER = booleanPreferencesKey("blur_drawer")
        val FONT = stringPreferencesKey("font")
        val ANIM = floatPreferencesKey("anim_speed")
        val TABS = booleanPreferencesKey("tabs")
        val LAST_TAB = stringPreferencesKey("last_tab")
    }

    val settings: Flow<LauncherSettings> = context.settingsDataStore.data.map { p ->
        LauncherSettings(
            themeMode = p[Keys.THEME_MODE] ?: "system",
            builtInTheme = p[Keys.BUILT_IN] ?: "default",
            accentColor = p[Keys.ACCENT] ?: 0xFF3B6EF5.toInt(),
            dynamicColor = p[Keys.DYNAMIC] != false,
            homeRows = p[Keys.HOME_ROWS] ?: 5,
            homeCols = p[Keys.HOME_COLS] ?: 4,
            drawerCols = p[Keys.DRAWER_COLS] ?: 4,
            dockApps = (p[Keys.DOCK] ?: emptySet()).toList(),
            iconShape = p[Keys.SHAPE] ?: "squircle",
            iconSize = p[Keys.ICON_SIZE] ?: 1f,
            labelSize = p[Keys.LABEL_SIZE] ?: 1f,
            showLabels = p[Keys.SHOW_LABELS] != false,
            iconPack = p[Keys.ICON_PACK] ?: "",
            swipeUpDrawer = p[Keys.SWIPE_UP] != false,
            doubleTapLock = p[Keys.DOUBLE_TAP] != false,
            swipeDownNotifications = p[Keys.SWIPE_DOWN] != false,
            pinchEdit = p[Keys.PINCH] != false,
            badgesEnabled = p[Keys.BADGES] != false,
            blurDock = p[Keys.BLUR_DOCK] == true,
            blurDrawer = p[Keys.BLUR_DRAWER] == true,
            fontChoice = p[Keys.FONT] ?: "sans",
            animationSpeed = p[Keys.ANIM] ?: 1f,
            tabsEnabled = p[Keys.TABS] != false,
            lastTab = p[Keys.LAST_TAB] ?: "All",
        )
    }

    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = LauncherSettings(
                themeMode = prefs[Keys.THEME_MODE] ?: "system",
                builtInTheme = prefs[Keys.BUILT_IN] ?: "default",
                accentColor = prefs[Keys.ACCENT] ?: 0xFF3B6EF5.toInt(),
                dynamicColor = prefs[Keys.DYNAMIC] != false,
                homeRows = prefs[Keys.HOME_ROWS] ?: 5,
                homeCols = prefs[Keys.HOME_COLS] ?: 4,
                drawerCols = prefs[Keys.DRAWER_COLS] ?: 4,
                dockApps = (prefs[Keys.DOCK] ?: emptySet()).toList(),
                iconShape = prefs[Keys.SHAPE] ?: "squircle",
                iconSize = prefs[Keys.ICON_SIZE] ?: 1f,
                labelSize = prefs[Keys.LABEL_SIZE] ?: 1f,
                showLabels = prefs[Keys.SHOW_LABELS] != false,
                iconPack = prefs[Keys.ICON_PACK] ?: "",
                swipeUpDrawer = prefs[Keys.SWIPE_UP] != false,
                doubleTapLock = prefs[Keys.DOUBLE_TAP] != false,
                swipeDownNotifications = prefs[Keys.SWIPE_DOWN] != false,
                pinchEdit = prefs[Keys.PINCH] != false,
                badgesEnabled = prefs[Keys.BADGES] != false,
                blurDock = prefs[Keys.BLUR_DOCK] == true,
                blurDrawer = prefs[Keys.BLUR_DRAWER] == true,
                fontChoice = prefs[Keys.FONT] ?: "sans",
                animationSpeed = prefs[Keys.ANIM] ?: 1f,
                tabsEnabled = prefs[Keys.TABS] != false,
                lastTab = prefs[Keys.LAST_TAB] ?: "All",
            )
            val next = transform(current)
            prefs[Keys.THEME_MODE] = next.themeMode
            prefs[Keys.BUILT_IN] = next.builtInTheme
            prefs[Keys.ACCENT] = next.accentColor
            prefs[Keys.DYNAMIC] = next.dynamicColor
            prefs[Keys.HOME_ROWS] = next.homeRows
            prefs[Keys.HOME_COLS] = next.homeCols
            prefs[Keys.DRAWER_COLS] = next.drawerCols
            prefs[Keys.DOCK] = next.dockApps.toSet()
            prefs[Keys.SHAPE] = next.iconShape
            prefs[Keys.ICON_SIZE] = next.iconSize
            prefs[Keys.LABEL_SIZE] = next.labelSize
            prefs[Keys.SHOW_LABELS] = next.showLabels
            prefs[Keys.ICON_PACK] = next.iconPack
            prefs[Keys.SWIPE_UP] = next.swipeUpDrawer
            prefs[Keys.DOUBLE_TAP] = next.doubleTapLock
            prefs[Keys.SWIPE_DOWN] = next.swipeDownNotifications
            prefs[Keys.PINCH] = next.pinchEdit
            prefs[Keys.BADGES] = next.badgesEnabled
            prefs[Keys.BLUR_DOCK] = next.blurDock
            prefs[Keys.BLUR_DRAWER] = next.blurDrawer
            prefs[Keys.FONT] = next.fontChoice
            prefs[Keys.ANIM] = next.animationSpeed
            prefs[Keys.TABS] = next.tabsEnabled
            prefs[Keys.LAST_TAB] = next.lastTab
        }
    }

    /** Import all values from a JSON backup. */
    suspend fun replaceAll(s: LauncherSettings) = update { s }

    companion object {
        fun default() = LauncherSettings()
    }
}
