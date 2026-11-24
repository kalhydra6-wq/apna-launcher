package com.apnalauncher.data

/**
 * Immutable description of one launchable activity.
 * `key` is stable and used by Room for folders / hidden / custom icons.
 */
data class AppInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val isSystem: Boolean = false,
) {
    val key: String get() = "$packageName/$className"

    /** Coarse categories used by the drawer tabs. */
    val category: String
        get() {
            val p = packageName.lowercase()
            return when {
                p.contains("game") || p.contains("play") || p.contains("minecraft") ||
                    p.contains("gameloft") || p.contains("supercell") -> "Games"
                p.contains("whatsapp") || p.contains("instagram") || p.contains("facebook") ||
                    p.contains("telegram") || p.contains("twitter") || p.contains("snapchat") ||
                    p.contains("messenger") || p.contains("discord") -> "Social"
                p.contains("setting") || p.contains("camera") || p.contains("file") ||
                    p.contains("browser") || p.contains("chrome") || p.contains("dialer") ||
                    p.contains("phone") || p.contains("clock") || p.contains("calculator") ||
                    p.contains("gallery") || p.contains("music") -> "Tools"
                else -> "Other"
            }
        }
}

/** Notification badge for an app: count > 0 shows a dot/count. */
data class NotificationBadge(val count: Int, val text: String = "")
