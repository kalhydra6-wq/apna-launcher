package com.apnalauncher.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.apnalauncher.data.AppInfo

/** Long-press menu shared by home icons and drawer items. */
@Composable
fun AppMenu(
    app: AppInfo,
    isLocked: Boolean,
    onDismiss: () -> Unit,
    onAddToHome: () -> Unit,
    onAddToDock: () -> Unit,
    onHide: () -> Unit,
    onToggleLock: () -> Unit,
    onEdit: () -> Unit,
    onUninstall: () -> Unit,
) {
    val context = LocalContext.current
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        MenuItem("App Info") { AppActions.appInfo(context, app); onDismiss() }
        MenuItem("Add to Home") { onAddToHome(); onDismiss() }
        MenuItem("Add to Dock") { onAddToDock(); onDismiss() }
        MenuItem(if (isLocked) "Unlock app" else "Lock app (biometric)") { onToggleLock(); onDismiss() }
        MenuItem("Edit name / icon") { onEdit(); onDismiss() }
        MenuItem("Hide app") { onHide(); onDismiss() }
        MenuItem("Uninstall") { onUninstall(); onDismiss() }
        val shortcuts = AppActions.shortcuts(context, app)
        if (shortcuts.isNotEmpty()) {
            HorizontalDivider()
            shortcuts.take(4).forEach { shortcut ->
                MenuItem("⚡ ${shortcut.shortLabel ?: shortcut.id}") {
                    AppActions.startShortcut(context, shortcut); onDismiss()
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text) }, onClick = onClick)
}
