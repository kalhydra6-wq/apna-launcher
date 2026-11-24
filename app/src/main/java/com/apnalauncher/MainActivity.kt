package com.apnalauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apnalauncher.data.AppInfo
import com.apnalauncher.data.LauncherSettings
import com.apnalauncher.ui.common.AppIconView
import com.apnalauncher.ui.drawer.AppDrawer
import com.apnalauncher.ui.drawer.DrawerViewModel
import com.apnalauncher.ui.home.HomeScreen
import com.apnalauncher.ui.home.HomeViewModel
import com.apnalauncher.ui.notifications.ApnaNotificationListener
import com.apnalauncher.ui.notifications.NotificationPanel
import com.apnalauncher.ui.settings.SettingsScreen
import com.apnalauncher.ui.theme.ApnaTheme

/** The single HOME activity. Back always returns to the home screen. */
class MainActivity : FragmentActivity() {
    private lateinit var widgetHost: AppWidgetHost
    private val widgetHostId = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetHost = AppWidgetHost(this, widgetHostId)
        setContent {
            val homeVm: HomeViewModel = viewModel()
            val drawerVm: DrawerViewModel = viewModel()
            val settings by homeVm.settings.collectAsState()

            ApnaTheme(settings) {
                LauncherRoot(homeVm, drawerVm, widgetHost, settings)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { widgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { widgetHost.stopListening() }
    }
}

@Composable
private fun LauncherRoot(
    homeVm: HomeViewModel,
    drawerVm: DrawerViewModel,
    widgetHost: AppWidgetHost,
    settings: LauncherSettings,
) {
    val context = LocalContext.current
    val repo = (context.applicationContext as ApnaLauncherApp).repo
    var editApp by remember { mutableStateOf<AppInfo?>(null) }
    var showPermissionInfo by remember {
        mutableStateOf(!ApnaNotificationListener.isEnabled(context) &&
            context.getSharedPreferences("apna", 0).getBoolean("asked_notifications", false).not())
    }

    BackHandler {
        when {
            homeVm.settingsOpen.value -> homeVm.settingsOpen.value = false
            editApp != null -> editApp = null
            homeVm.notificationsOpen.value -> homeVm.notificationsOpen.value = false
            homeVm.drawerOpen.value -> homeVm.drawerOpen.value = false
            // Stay on home — this is a launcher.
        }
    }

    HomeScreen(homeVm, widgetHost, onEditApp = { editApp = it })

    if (homeVm.drawerOpen.value) {
        AppDrawer(
            vm = drawerVm,
            onDismiss = { homeVm.drawerOpen.value = false },
            onAddToHome = { app -> homeVm.addApp(app, 0, 0) },
            onAddToDock = {
                val dock = settings.dockApps.toMutableList()
                while (dock.size < 5) dock.add("")
                if (it.key !in dock) {
                    val slot = dock.indexOfFirst { s -> s.isEmpty() }.takeIf { s -> s >= 0 } ?: 0
                    dock[slot] = it.key
                    homeVm.setDock(dock)
                }
            },
            onEditApp = { editApp = it },
        )
    }

    if (homeVm.notificationsOpen.value) {
        NotificationPanel(onClose = { homeVm.notificationsOpen.value = false })
    }

    if (homeVm.settingsOpen.value) {
        SettingsScreen(onClose = { homeVm.settingsOpen.value = false })
    }

    editApp?.let { app ->
        EditAppDialog(app, homeVm, onClose = { editApp = null })
    }

    if (showPermissionInfo) {
        AlertDialog(
            onDismissRequest = {
                context.getSharedPreferences("apna", 0).edit().putBoolean("asked_notifications", true).apply()
                showPermissionInfo = false
            },
            title = { Text("Notification access") },
            text = {
                Text("Apna Launcher can show notification badges and an in-launcher notification panel. " +
                    "Grant notification access in the next screen (optional).")
            },
            confirmButton = {
                TextButton(onClick = {
                    context.getSharedPreferences("apna", 0).edit().putBoolean("asked_notifications", true).apply()
                    showPermissionInfo = false
                    runCatching {
                        context.startActivity(android.content.Intent(
                            android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }) { Text("Grant") }
            },
            dismissButton = {
                TextButton(onClick = {
                    context.getSharedPreferences("apna", 0).edit().putBoolean("asked_notifications", true).apply()
                    showPermissionInfo = false
                }) { Text("Not now") }
            },
        )
    }
}

/** Rename an app and pick any installed app's icon as its replacement. */
@Composable
private fun EditAppDialog(app: AppInfo, vm: HomeViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = (context.applicationContext as ApnaLauncherApp).repo
    val apps by repo.visibleApps.collectAsState()
    val customs by repo.customs.collectAsState()
    var label by remember { mutableStateOf(customs[app.key]?.label ?: app.label) }
    var selectedIcon by remember { mutableStateOf<AppInfo?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Edit ${app.label}") },
        text = {
            Column {
                OutlinedTextField(value = label, onValueChange = { label = it }, singleLine = true,
                    label = { Text("Name") })
                Text("Choose an icon (optional):", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.heightIn(max = 260.dp),
                ) {
                    items(apps.take(40), key = { it.key }) { candidate ->
                        AppIconView(
                            app = candidate, showLabel = false, iconSize = 40.dp,
                            onClick = { selectedIcon = candidate },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.setCustomLabel(app, label)
                selectedIcon?.let { vm.setCustomIcon(app, it.packageName, it.className) }
                onClose()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { vm.clearCustom(app); onClose() }) { Text("Reset") }
        },
    )
}
