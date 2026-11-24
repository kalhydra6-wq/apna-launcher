package com.apnalauncher.ui.home

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetHost
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import com.apnalauncher.ApnaLauncherApp
import com.apnalauncher.data.AppInfo
import com.apnalauncher.data.HomeItem
import com.apnalauncher.ui.common.AppActions
import com.apnalauncher.ui.common.AppIconView
import com.apnalauncher.ui.common.AppMenu
import com.apnalauncher.ui.notifications.NotificationStore
import com.apnalauncher.ui.widgets.WidgetView

private data class DragState(
    val item: HomeItem, val startX: Int, val startY: Int,
    val offsetX: Float, val offsetY: Float,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    widgetHost: AppWidgetHost,
    onEditApp: (AppInfo) -> Unit,
) {
    val context = LocalContext.current
    val repo = (context.applicationContext as ApnaLauncherApp).repo
    val settings by vm.settings.collectAsState()
    val items by vm.homeItems.collectAsState()
    val folders by vm.folders.collectAsState()
    val apps by repo.visibleApps.collectAsState()
    val badges by NotificationStore.badges.collectAsState()
    val locked by repo.lockedKeys.collectAsState()
    val edit by vm.editMode.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 5 })
    var drag by remember { mutableStateOf<DragState?>(null) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var emptyMenu by remember { mutableStateOf(false) }
    var gridBounds by remember { mutableStateOf(Rect.Zero) }
    var dockBounds by remember { mutableStateOf(Rect.Zero) }
    var folderPopup by remember { mutableStateOf<String?>(null) }
    var widgetMenu by remember { mutableStateOf<HomeItem?>(null) }
    var renameFolder by remember { mutableStateOf<String?>(null) }
    val uninstall = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        repo.refreshApps()
    }
    val widgetConfigure = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = result.data?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (id != -1 && result.resultCode == android.app.Activity.RESULT_OK) vm.addWidget(id, 0, 0)
    }
    val widgetPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = result.data?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (id != -1) {
            val manager = AppWidgetManager.getInstance(context)
            val info = manager.getAppWidgetInfo(id)
            if (info?.configure != null) {
                try {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = info.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    }
                    widgetConfigure.launch(intent)
                } catch (_: Exception) {
                    vm.addWidget(id, 0, 0)
                }
            } else vm.addWidget(id, 0, 0)
        } else {
            widgetHost.deleteAppWidgetId(id)
        }
    }

    LaunchedEffect(pagerState.currentPage) { vm.page.value = pagerState.currentPage }

    fun finishDrag() {
        val d = drag ?: return
        drag = null
        val cols = settings.homeCols.coerceAtLeast(1)
        val rows = settings.homeRows.coerceAtLeast(1)
        val cellW = if (gridBounds.width > 0) gridBounds.width / cols else 0f
        val cellH = if (gridBounds.height > 0) gridBounds.height / rows else 0f
        if (cellW <= 0f || cellH <= 0f) return

        val pointX = gridBounds.left + (d.startX + 0.5f) * cellW + d.offsetX
        val pointY = gridBounds.top + (d.startY + 0.5f) * cellH + d.offsetY

        // Drop on dock -> replace that slot.
        if (dockBounds != Rect.Zero && dockBounds.contains(Offset(pointX, pointY)) &&
            d.item.type == HomeItem.TYPE_APP
        ) {
            val slot = ((pointX - dockBounds.left) / (dockBounds.width / 5)).toInt().coerceIn(0, 4)
            val dock = settings.dockApps.toMutableList()
            while (dock.size < 5) dock.add("")
            dock[slot] = d.item.value
            vm.setDock(dock)
            return
        }

        val newX = ((pointX - gridBounds.left) / cellW).toInt().coerceIn(0, cols - 1)
        val newY = ((pointY - gridBounds.top) / cellH).toInt().coerceIn(0, rows - 1)
        val target = items.firstOrNull {
            it.page == vm.page.value && it.x == newX && it.y == newY && it.id != d.item.id
        }
        when {
            target != null && d.item.type == HomeItem.TYPE_APP && target.type == HomeItem.TYPE_APP -> {
                val app = repo.appByKey(d.item.value)
                if (app != null) vm.createFolder(app, target)
            }
            target != null -> {
                vm.moveItem(d.item, vm.page.value, target.x, target.y)
                vm.moveItem(target, target.page, d.startX, d.startY)
            }
            else -> vm.moveItem(d.item, vm.page.value, newX, newY)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(settings) {
                detectTapGestures(
                    onDoubleTap = {
                        if (settings.doubleTapLock) lockScreen(context)
                    },
                    onLongPress = { emptyMenu = true },
                )
            }
            .pointerInput(settings) {
                var fired = false
                detectVerticalDragGestures(
                    onDragStart = { fired = false },
                    onDragEnd = { fired = false },
                    onDragCancel = { fired = false },
                ) { _, amount ->
                    if (fired) return@detectVerticalDragGestures
                    if (settings.swipeUpDrawer && amount < -28) {
                        fired = true; vm.drawerOpen.value = true
                    } else if (settings.swipeDownNotifications && amount > 28) {
                        fired = true; vm.notificationsOpen.value = true
                    }
                }
            }
            .pointerInput(settings) {
                var fired = false
                detectTransformGestures { _, _, zoom, _ ->
                    if (settings.pinchEdit && zoom < 0.72f && !fired) {
                        fired = true
                        vm.toggleEdit()
                    }
                    if (zoom > 1.3f) fired = false
                }
            },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 86.dp),
        ) { pageIndex ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { gridBounds = it.boundsInRoot() },
            ) {
                repeat(settings.homeRows.coerceIn(2, 8)) { row ->
                    Row(modifier = Modifier.weight(1f)) {
                        repeat(settings.homeCols.coerceIn(2, 8)) { col ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center,
                            ) {
                                val item = items.firstOrNull {
                                    it.page == pageIndex && it.x == col && it.y == row
                                }
                                if (item != null) {
                                    val isDragging = drag?.item?.id == item.id
                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                if (isDragging) {
                                                    translationX = drag?.offsetX ?: 0f
                                                    translationY = drag?.offsetY ?: 0f
                                                    scaleX = 1.12f
                                                    scaleY = 1.12f
                                                }
                                            }
                                            .pointerInput(item.id) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        drag = DragState(item, col, row, 0f, 0f)
                                                    },
                                                    onDrag = { change, amount ->
                                                        change.consume()
                                                        drag = drag?.copy(
                                                            offsetX = (drag?.offsetX ?: 0f) + amount.x,
                                                            offsetY = (drag?.offsetY ?: 0f) + amount.y,
                                                        )
                                                    },
                                                    onDragEnd = { finishDrag() },
                                                    onDragCancel = { drag = null },
                                                )
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        when (item.type) {
                                            HomeItem.TYPE_APP -> {
                                                val app = apps.firstOrNull { it.key == item.value }
                                                if (app != null) {
                                                    AppIconView(
                                                        app = app,
                                                        badgeCount = badges[app.packageName]?.count ?: 0,
                                                        onClick = {
                                                            if (edit) vm.removeItem(item)
                                                            else AppActions.launch(context as androidx.fragment.app.FragmentActivity,
                                                                app, app.key in locked)
                                                        },
                                                        onLongClick = { menuApp = app },
                                                    )
                                                }
                                            }
                                            HomeItem.TYPE_FOLDER -> {
                                                val folder = folders.firstOrNull { it.id == item.value }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("📁", fontSize = (38 * settings.iconSize).sp)
                                                    Text(folder?.name ?: "Folder", fontSize = (11 * settings.labelSize).sp)
                                                }
                                                Box(Modifier.matchParentSize().pointerInput(item.id) {
                                                    detectTapGestures(
                                                        onTap = { folderPopup = item.value },
                                                        onLongPress = { renameFolder = item.value },
                                                    )
                                                })
                                            }
                                            HomeItem.TYPE_WIDGET -> {
                                                WidgetView(
                                                    widgetId = item.value.toIntOrNull() ?: -1,
                                                    host = widgetHost,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.95f)
                                                        .height(72.dp * item.spanY),
                                                    onLongClick = { widgetMenu = item },
                                                )
                                            }
                                        }
                                        if (edit) {
                                            Text("✕", color = Color.Red, fontSize = 14.sp,
                                                modifier = Modifier.align(Alignment.TopEnd))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Page indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 78.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == pagerState.currentPage) 8.dp else 5.dp)
                        .background(
                            if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            androidx.compose.foundation.shape.CircleShape,
                        ),
                )
            }
        }

        // Dock
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    if (settings.blurDock) MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                )
                .onGloballyPositioned { dockBounds = it.boundsInRoot() },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dock = settings.dockApps
            repeat(5) { index ->
                val key = dock.getOrNull(index).orEmpty()
                val app = apps.firstOrNull { it.key == key }
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .pointerInput(index) {
                            detectTapGestures(
                                onTap = {
                                    if (app != null) AppActions.launch(context as androidx.fragment.app.FragmentActivity,
                                        app, app.key in locked)
                                    else vm.drawerOpen.value = true
                                },
                                onLongPress = {
                                    if (app != null) menuApp = app
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (app != null) {
                        AppIconView(app = app, showLabel = false, iconSize = 42.dp,
                            onClick = { AppActions.launch(context as androidx.fragment.app.FragmentActivity,
                                app, app.key in locked) })
                    } else {
                        Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Long-press empty space menu
        if (emptyMenu) {
            DropdownMenu(expanded = true, onDismissRequest = { emptyMenu = false }) {
                DropdownMenuItem(text = { Text("🖼 Wallpaper") }, onClick = {
                    runCatching {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
                    }
                    emptyMenu = false
                })
                DropdownMenuItem(text = { Text("🧩 Widgets") }, onClick = {
                    val id = widgetHost.allocateAppWidgetId()
                    val pick = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    }
                    runCatching { widgetPicker.launch(pick) }
                    emptyMenu = false
                })
                DropdownMenuItem(text = { Text("🎨 Themes") }, onClick = {
                    vm.settingsOpen.value = true; emptyMenu = false
                })
                DropdownMenuItem(text = { Text("⚙ Launcher Settings") }, onClick = {
                    vm.settingsOpen.value = true; emptyMenu = false
                })
            }
        }

        menuApp?.let { app ->
            AppMenu(
                app = app,
                isLocked = app.key in locked,
                onDismiss = { menuApp = null },
                onAddToHome = {
                    vm.addApp(app, 0, 0)
                    Toast.makeText(context, "Added to home", Toast.LENGTH_SHORT).show()
                },
                onAddToDock = {
                    val dock = settings.dockApps.toMutableList()
                    while (dock.size < 5) dock.add("")
                    if (app.key !in dock) {
                        val slot = dock.indexOfFirst { it.isEmpty() }.takeIf { it >= 0 } ?: 0
                        dock[slot] = app.key
                        vm.setDock(dock)
                    }
                },
                onHide = {
                    viewModelScopeLaunch { repo.toggleHidden(app) }
                    Toast.makeText(context, "App hidden", Toast.LENGTH_SHORT).show()
                },
                onToggleLock = { viewModelScopeLaunch { repo.toggleLocked(app) } },
                onEdit = { onEditApp(app) },
                onUninstall = { uninstall.launch(AppActions.uninstallIntent(app)) },
            )
        }

        widgetMenu?.let { item ->
            AlertDialog(
                onDismissRequest = { widgetMenu = null },
                title = { Text("Widget") },
                text = {
                    Column {
                        TextButton(onClick = {
                            vm.resizeWidget(item, item.spanX + 1, item.spanY)
                            widgetMenu = null
                        }) { Text("Resize +") }
                        TextButton(onClick = {
                            vm.resizeWidget(item, item.spanX - 1, item.spanY - 1)
                            widgetMenu = null
                        }) { Text("Resize -") }
                        TextButton(onClick = {
                            vm.removeItem(item)
                            widgetHost.deleteAppWidgetId(item.value.toIntOrNull() ?: -1)
                            widgetMenu = null
                        }) { Text("Remove widget") }
                    }
                },
                confirmButton = { TextButton(onClick = { widgetMenu = null }) { Text("Close") } },
            )
        }

        folderPopup?.let { id ->
            val members by repo.dao.folderApps(id).collectAsState(emptyList<com.apnalauncher.data.FolderApp>())
            val contents = members.mapNotNull { m -> apps.firstOrNull { it.key == m.key } }
            Dialog(onDismissRequest = { folderPopup = null }) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(folders.firstOrNull { it.id == id }?.name ?: "Folder", fontWeight = FontWeight.Bold)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.heightIn(max = 320.dp),
                        ) {
                            items(contents, key = { it.key }) { app ->
                                AppIconView(app = app, onClick = {
                                    AppActions.launch(context as androidx.fragment.app.FragmentActivity, app, false)
                                    folderPopup = null
                                })
                            }
                        }
                        Row {
                            TextButton(onClick = { renameFolder = id; folderPopup = null }) { Text("Rename") }
                            TextButton(onClick = { vm.deleteFolder(id); folderPopup = null }) { Text("Delete folder") }
                            TextButton(onClick = { folderPopup = null }) { Text("Close") }
                        }
                    }
                }
            }
        }

        renameFolder?.let { id ->
            var name by remember(id) {
                mutableStateOf(folders.firstOrNull { it.id == id }?.name ?: "")
            }
            AlertDialog(
                onDismissRequest = { renameFolder = null },
                title = { Text("Rename folder") },
                text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = { vm.renameFolder(id, name); renameFolder = null }) { Text("Save") }
                },
            )
        }
    }
}

private fun viewModelScopeLaunch(block: suspend () -> Unit) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { block() }
}

private fun lockScreen(context: android.content.Context) {
    try {
        val sm = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java).invoke(null, "window") as android.os.IBinder
        val stub = Class.forName("android.view.IWindowManager\$Stub")
            .getMethod("asInterface", android.os.IBinder::class.java).invoke(null, sm)
        stub.javaClass.getMethod("lockNow").invoke(stub)
    } catch (_: Exception) {
        Toast.makeText(context, "Screen lock not available on this device", Toast.LENGTH_SHORT).show()
    }
}
