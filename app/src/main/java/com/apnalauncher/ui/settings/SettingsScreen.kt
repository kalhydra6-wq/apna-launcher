package com.apnalauncher.ui.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.apnalauncher.ApnaLauncherApp
import com.apnalauncher.backup.BackupManager
import com.apnalauncher.data.LauncherSettings
import com.apnalauncher.icons.IconPackInfo
import com.apnalauncher.ui.notifications.ApnaNotificationListener

private val CATEGORIES = listOf("Home", "Drawer", "Icons", "Themes", "Gestures", "Notifications", "Backup", "About")

/** Full settings screen covering every customizable part of the launcher. */
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = (context.applicationContext as ApnaLauncherApp).repo
    val settings by repo.settings.settings.collectAsState()
    var category by remember { mutableStateOf("Home") }
    var packs by remember { mutableStateOf<List<IconPackInfo>>(emptyList()) }
    val backup = remember { BackupManager(context, repo) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { backup.export(uri) }
                .onSuccess { Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { backup.import(uri) }
                .onSuccess { Toast.makeText(context, "Restored", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
        }
    }

    LaunchedEffect(Unit) { packs = repo.packs.availablePacks() }

    fun update(transform: (LauncherSettings) -> LauncherSettings) =
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repo.settings.update(transform)
        }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Launcher Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Text("✕") }
        }
        ScrollableTabRow(selectedTabIndex = CATEGORIES.indexOf(category)) {
            CATEGORIES.forEach { Tab(selected = category == it, onClick = { category = it }, text = { Text(it) }) }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (category) {
                "Home" -> {
                    item {
                        SectionTitle("Home screen")
                        IntSlider("Rows", settings.homeRows, 3, 9) { update { s -> s.copy(homeRows = it) } }
                        IntSlider("Columns", settings.homeCols, 3, 8) { update { s -> s.copy(homeCols = it) } }
                        Text("Tip: pinch on home to toggle edit mode. Long-press empty space for wallpaper, widgets and themes.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                "Drawer" -> {
                    item {
                        SectionTitle("App drawer")
                        IntSlider("Drawer columns", settings.drawerCols, 3, 6) { update { s -> s.copy(drawerCols = it) } }
                        SwitchRow("Show category tabs", settings.tabsEnabled) { update { s -> s.copy(tabsEnabled = it) } }
                        Text("Default tab: ${settings.lastTab}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                "Icons" -> {
                    item {
                        SectionTitle("Icon pack")
                        Text("System (default)", modifier = Modifier.clickable { update { s -> s.copy(iconPack = "") } },
                            color = if (settings.iconPack.isEmpty()) MaterialTheme.colorScheme.primary else Color.Unspecified)
                        packs.forEach { pack ->
                            Text(pack.name, modifier = Modifier.clickable { update { s -> s.copy(iconPack = pack.packageName) } },
                                color = if (settings.iconPack == pack.packageName) MaterialTheme.colorScheme.primary else Color.Unspecified)
                        }
                    }
                    item {
                        SectionTitle("Icon shape")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("circle", "squircle", "square", "rounded", "teardrop").forEach { shape ->
                                FilterChip(selected = settings.iconShape == shape,
                                    onClick = { update { s -> s.copy(iconShape = shape) } },
                                    label = { Text(shape) })
                            }
                        }
                        FloatSlider("Icon size", settings.iconSize, 0.6f, 1.6f) { update { s -> s.copy(iconSize = it) } }
                        FloatSlider("Label size", settings.labelSize, 0.6f, 1.6f) { update { s -> s.copy(labelSize = it) } }
                        SwitchRow("Show labels", settings.showLabels) { update { s -> s.copy(showLabels = it) } }
                    }
                }
                "Themes" -> {
                    item {
                        SectionTitle("Theme")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("light", "dark", "system").forEach { mode ->
                                FilterChip(selected = settings.themeMode == mode,
                                    onClick = { update { s -> s.copy(themeMode = mode) } }, label = { Text(mode) })
                            }
                        }
                        SectionTitle("Built-in style")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("default", "amoled", "glass", "neon", "minimal").forEach { theme ->
                                FilterChip(selected = settings.builtInTheme == theme,
                                    onClick = { update { s -> s.copy(builtInTheme = theme) } }, label = { Text(theme) })
                            }
                        }
                        SwitchRow("Material You dynamic colors", settings.dynamicColor) { update { s -> s.copy(dynamicColor = it) } }
                        SectionTitle("Accent color")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0xFF3B6EF5, 0xFFE5484D, 0xFF30A46C, 0xFF9E4FE8, 0xFFFF9F0A).forEach { color ->
                                Box(Modifier.size(36.dp).background(Color(color.toInt()))
                                    .clickable { update { s -> s.copy(accentColor = color.toInt()) } })
                            }
                        }
                        SectionTitle("Font")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("sans", "serif", "monospace").forEach { font ->
                                FilterChip(selected = settings.fontChoice == font,
                                    onClick = { update { s -> s.copy(fontChoice = font) } }, label = { Text(font) })
                            }
                        }
                        FloatSlider("Animation speed", settings.animationSpeed, 0.25f, 2f) {
                            update { s -> s.copy(animationSpeed = it) }
                        }
                        SwitchRow("Blur dock background", settings.blurDock) { update { s -> s.copy(blurDock = it) } }
                        SwitchRow("Blur drawer background", settings.blurDrawer) { update { s -> s.copy(blurDrawer = it) } }
                    }
                }
                "Gestures" -> {
                    item {
                        SectionTitle("Gestures")
                        SwitchRow("Swipe up opens drawer", settings.swipeUpDrawer) { update { s -> s.copy(swipeUpDrawer = it) } }
                        SwitchRow("Swipe down opens notifications", settings.swipeDownNotifications) {
                            update { s -> s.copy(swipeDownNotifications = it) }
                        }
                        SwitchRow("Double tap locks screen", settings.doubleTapLock) { update { s -> s.copy(doubleTapLock = it) } }
                        SwitchRow("Pinch opens edit mode", settings.pinchEdit) { update { s -> s.copy(pinchEdit = it) } }
                    }
                }
                "Notifications" -> {
                    item {
                        SectionTitle("Notifications")
                        val enabled = ApnaNotificationListener.isEnabled(context)
                        Text(if (enabled) "✅ Notification access granted" else "⚠️ Notification access not granted")
                        if (!enabled) {
                            Button(onClick = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            }) { Text("Grant notification access") }
                        }
                        SwitchRow("Show notification badges", settings.badgesEnabled) {
                            update { s -> s.copy(badgesEnabled = it) }
                        }
                    }
                }
                "Backup" -> {
                    item {
                        SectionTitle("Backup & restore")
                        Text("Export your layout, settings, folders and hidden apps as JSON, or restore them later.")
                        Button(onClick = { exportLauncher.launch("apna-launcher-backup.json") }) { Text("Export backup") }
                        Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("Import backup") }
                    }
                }
                "About" -> {
                    item {
                        SectionTitle("Apna Launcher")
                        Text("Version 1.0 (debug build)")
                        Text("Built with Kotlin + Jetpack Compose. MVVM, Room, DataStore.")
                        Text("Min SDK 26 · Target SDK 34")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun IntSlider(label: String, value: Int, from: Int, to: Int, onChange: (Int) -> Unit) {
    Column {
        Text("$label: $value")
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) },
            valueRange = from.toFloat()..to.toFloat(), steps = to - from - 1)
    }
}

@Composable
private fun FloatSlider(label: String, value: Float, from: Float, to: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$label: ${"%.2f".format(value)}")
        Slider(value = value, onValueChange = onChange, valueRange = from..to)
    }
}
