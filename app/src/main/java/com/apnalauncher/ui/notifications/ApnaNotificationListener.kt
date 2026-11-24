package com.apnalauncher.ui.notifications

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Receives notifications and feeds the badge / notification panel state. */
class ApnaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true
        active = this
        activeNotifications?.forEach { NotificationStore.onPosted(it) }
    }

    override fun onListenerDisconnected() {
        connected = false
        active = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        NotificationStore.onPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationStore.onRemoved(sbn)
    }

    fun dismiss(key: String) = runCatching { cancelNotification(key) }

    companion object {
        @Volatile
        var connected: Boolean = false
            private set

        @Volatile
        private var active: ApnaNotificationListener? = null

        fun dismissNotification(key: String) {
            active?.dismiss(key)
        }

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?: return false
            val self = ComponentName(context, ApnaNotificationListener::class.java).flattenToString()
            return flat.split(":").any { it == self }
        }
    }
}
