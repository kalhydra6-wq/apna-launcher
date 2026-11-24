package com.apnalauncher.ui.notifications

import android.service.notification.StatusBarNotification
import com.apnalauncher.data.NotificationBadge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Single in-memory source of truth for notifications seen by the listener. */
object NotificationStore {
    private val _notifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
    val notifications: StateFlow<List<StatusBarNotification>> = _notifications.asStateFlow()

    private val _badges = MutableStateFlow<Map<String, NotificationBadge>>(emptyMap())
    val badges: StateFlow<Map<String, NotificationBadge>> = _badges.asStateFlow()

    fun onPosted(sbn: StatusBarNotification) {
        _notifications.value = (_notifications.value.filter { it.key != sbn.key } + sbn)
            .sortedByDescending { it.postTime }
        recomputeBadges()
    }

    fun onRemoved(sbn: StatusBarNotification) {
        _notifications.value = _notifications.value.filter { it.key != sbn.key }
        recomputeBadges()
    }

    fun clearAll() {
        _notifications.value = emptyList()
        _badges.value = emptyMap()
    }

    private fun recomputeBadges() {
        val map = mutableMapOf<String, NotificationBadge>()
        _notifications.value.forEach { sbn ->
            val extra = runCatching {
                sbn.notification.extras?.getInt("android.badgeCount", 1) ?: 1
            }.getOrDefault(1)
            val previous = map[sbn.packageName]?.count ?: 0
            map[sbn.packageName] = NotificationBadge(previous + extra.coerceAtLeast(1))
        }
        _badges.value = map
    }
}
