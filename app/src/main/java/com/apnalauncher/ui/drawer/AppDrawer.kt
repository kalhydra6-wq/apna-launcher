package com.apnalauncher.ui.drawer

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apnalauncher.ApnaLauncherApp
import com.apnalauncher.data.AppInfo
import com.apnalauncher.ui.common.AppActions
import com.apnalauncher.ui.common.AppIconView
import com.apnalauncher.ui.common.AppMenu
import com.apnalauncher.ui.theme.LocalLauncherSettings

/** Full app drawer: search, tabs, alphabet fast-scroll and long-press actions. */
@Composable
fun AppDrawer(
    vm: DrawerViewModel,
    onDismiss: () -> Unit,
    onAddToHome: (AppInfo) -> Unit,
    onAddToDock: (AppInfo) -> Unit,
    onEditApp: (AppInfo) -> Unit,
) {
    val context = LocalContext.current
    val repo = (context.applicationContext as ApnaLauncherApp).repo
    val apps by vm.apps.collectAsState()
    val settings by vm.settings.collectAsState()
    val query by vm.query.collectAsState()
    val tab by vm.tab.collectAsState()
    val locked by repo.lockedKeys.collectAsState()
    val listState = rememberLazyGridState()
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    val uninstall = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        repo.refreshApps()
    }
    val tabs = listOf("All", "Social", "Games", "Tools")

    // Build offsets for the alphabet strip.
    val indexOffsets = remember(apps) {
        val map = mutableMapOf<String, Int>()
        apps.forEachIndexed { index, app ->
            val letter = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            if (!map.containsKey(letter)) map[letter] = index
        }
        map
    }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (settings.blurDrawer)
                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
            else MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text("Search apps...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) { Text("✕") }
            }
            if (settings.tabsEnabled) {
                ScrollableTabRow(selectedTabIndex = tabs.indexOf(tab).coerceAtLeast(0)) {
                    tabs.forEach { name ->
                        Tab(selected = tab == name, onClick = { vm.setTab(name) }, text = { Text(name) })
                    }
                }
            }
            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = listState,
                    columns = GridCells.Fixed(settings.drawerCols),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    var lastLetter = ""
                    apps.forEachIndexed { index, app ->
                        val letter = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                        if (letter != lastLetter) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    letter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            lastLetter = letter
                        }
                        item(key = app.key) {
                            AppIconView(
                                app = app,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { AppActions.launch(context as androidx.fragment.app.FragmentActivity, app,
                                    app.key in locked); onDismiss() },
                                onLongClick = { menuApp = app },
                            )
                        }
                    }
                }
                // Alphabet fast-scroll strip
                if (indexOffsets.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 2.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        indexOffsets.keys.sorted().forEach { letter ->
                            Text(
                                letter,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                                    .clickable {
                                        indexOffsets[letter]?.let { index ->
                                            scope.launch { listState.scrollToItem(index) }
                                        }
                                    },
                            )
                        }
                    }
                }
            }
        }

        menuApp?.let { app ->
            AppMenu(
                app = app,
                isLocked = app.key in locked,
                onDismiss = { menuApp = null },
                onAddToHome = { onAddToHome(app); Toast.makeText(context, "Added to home", Toast.LENGTH_SHORT).show() },
                onAddToDock = { onAddToDock(app) },
                onHide = { vm.toggleHide(app); Toast.makeText(context, "App hidden", Toast.LENGTH_SHORT).show() },
                onToggleLock = { vm.toggleLock(app) },
                onEdit = { onEditApp(app) },
                onUninstall = { uninstall.launch(AppActions.uninstallIntent(app)) },
            )
        }
    }
}
