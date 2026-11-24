package com.apnalauncher.ui.notifications

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.apnalauncher.ApnaLauncherApp
import com.apnalauncher.data.LauncherSettings

/** In-launcher notification shade with quick settings and a dismissible list. */
@Composable
fun NotificationPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val notifications by NotificationStore.notifications.collectAsState()
    val repo = (context.applicationContext as ApnaLauncherApp).repo
    val settings by repo.settings.settings.collectAsState(LauncherSettings())

    @Suppress("DEPRECATION")
    var wifiOn by remember {
        mutableStateOf(runCatching {
            (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
        }.getOrDefault(false))
    }
    @Suppress("DEPRECATION")
    var bluetoothOn by remember { mutableStateOf(runCatching {
        BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
    }.getOrDefault(false)) }
    var torchOn by remember { mutableStateOf(false) }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            runCatching {
                val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cm.cameraIdList.firstOrNull()?.let { cm.setTorchMode(it, !torchOn) }
                torchOn = !torchOn
            }.onFailure { Toast.makeText(context, "Torch unavailable", Toast.LENGTH_SHORT).show() }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Notifications", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { NotificationStore.clearAll() }) { Text("Clear all") }
            IconButton(onClick = onClose) { Text("✕") }
        }

        // Quick settings tiles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            QuickTile("📶", "Wi-Fi", wifiOn) {
                runCatching {
                    @Suppress("DEPRECATION")
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val method = WifiManager::class.java.getDeclaredMethod(
                        "setWifiEnabled", java.lang.Boolean.TYPE,
                    )
                    method.invoke(wm, !wifiOn)
                    wifiOn = !wifiOn
                }.onFailure {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            QuickTile("🔵", "Bluetooth", bluetoothOn) {
                runCatching {
                    @Suppress("DEPRECATION")
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                    if (adapter == null) throw IllegalStateException()
                    if (bluetoothOn) adapter.disable() else adapter.enable()
                    bluetoothOn = !bluetoothOn
                }.onFailure {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            QuickTile("🔦", "Torch", torchOn) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    runCatching {
                        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        cm.cameraIdList.firstOrNull()?.let { cm.setTorchMode(it, !torchOn) }
                        torchOn = !torchOn
                    }.onFailure { Toast.makeText(context, "Torch unavailable", Toast.LENGTH_SHORT).show() }
                } else {
                    cameraPermission.launch(Manifest.permission.CAMERA)
                }
            }
            QuickTile("✈️", "Airplane", false) {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }

        HorizontalDivider()

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notifications", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notifications, key = { it.key }) { sbn ->
                    val extras = sbn.notification.extras
                    val title = extras?.getCharSequence("android.title")?.toString().orEmpty()
                    val text = extras?.getCharSequence("android.text")?.toString().orEmpty()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(sbn.key) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (dragAmount < -18) {
                                        NotificationStore.onRemoved(sbn)
                                        ApnaNotificationListener.dismissNotification(sbn.key)
                                    }
                                }
                            }
                            .clickable {
                                sbn.notification.contentIntent?.send()
                                onClose()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
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

@Composable
private fun QuickTile(emoji: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 18.sp) }
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
