package com.apnalauncher.ui.notifications

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apnalauncher.ApnaLauncherApp
import com.apnalauncher.data.LauncherSettings

/** In-launcher notification shade: list, tap to open, swipe to dismiss. */
@Composable
fun NotificationPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val notifications by NotificationStore.notifications.collectAsState()
    val repo = (context.applicationContext as ApnaLauncherApp).repo
    val settings by repo.settings.settings.collectAsState(LauncherSettings())

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Notifications", style = Material3Typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { NotificationStore.clearAll() }) { Text("Clear all") }
            IconButton(onClick = onClose) { Text("✕") }
        }
        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notifications", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notifications, key = { it.key }) { sbn ->
                    val extras = sbn.notification.extras
                    val title = extras?.getCharSequence("android.title")?.toString().orEmpty()
                    val text = extras?.getCharSequence("android.text")?.toString().orEmpty()
                    val app = repo.appByKey(sbn.packageName + "/" + sbn.packageName)
                    var dismissed = false
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(sbn.key) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    if (dragAmount < -18 && !dismissed) {
                                        dismissed = true
                                        NotificationStore.onRemoved(sbn)
                                        ApnaNotificationListener.dismissNotification(sbn.key)
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text, fontSize = 12.sp, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    sbn.packageName + " · " + DateUtils.getRelativeTimeSpanString(sbn.postTime),
                                    fontSize = 10.sp, color = Color.Gray,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private object Material3Typography {
    val titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 22.sp)
}
