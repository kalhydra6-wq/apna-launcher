package com.apnalauncher.icons

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Xml
import com.apnalauncher.data.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Represents a Nova/ADW style icon pack installed on the device. */
data class IconPackInfo(val packageName: String, val name: String)

/**
 * Reads `appfilter.xml` (Nova / ADW format) from third party icon packs and
 * resolves per-app drawables. Falls back to the system icon when missing.
 */
class IconPackManager(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private val mappingCache = mutableMapOf<String, Map<String, String>>()
    private val packCache = mutableMapOf<String, IconPackInfo>()

    /** Finds installed icon packs by looking for the appfilter.xml resource. */
    suspend fun availablePacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        pm.getInstalledApplications(0).mapNotNull { app ->
            try {
                val res = pm.getResourcesForApplication(app.packageName)
                val id = res.getIdentifier("appfilter", "xml", app.packageName)
                if (id != 0) IconPackInfo(app.packageName, app.loadLabel(pm).toString()).also {
                    packCache[app.packageName] = it
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun mapping(packPackage: String): Map<String, String> {
        mappingCache[packPackage]?.let { return it }
        val result = mutableMapOf<String, String>()
        try {
            val res = pm.getResourcesForApplication(packPackage)
            val id = res.getIdentifier("appfilter", "xml", packPackage)
            if (id != 0) {
                val parser = res.getXml(id)
                val event = Xml.newPullParser().let { it }
                // Use the parser returned by resources (already positioned at START_DOCUMENT).
                var type = parser.eventType
                while (type != android.util.Xml.END_DOCUMENT) {
                    if (type == android.util.Xml.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component").orEmpty()
                        val drawable = parser.getAttributeValue(null, "drawable").orEmpty()
                        if (component.isNotBlank() && drawable.isNotBlank()) {
                            result[normalize(component)] = drawable
                        }
                    }
                    type = parser.next()
                }
            }
        } catch (_: Exception) {
        }
        mappingCache[packPackage] = result
        return result
    }

    /** ComponentInfo{com.pkg/com.pkg.Act} -> "com.pkg/com.pkg.Act". */
    private fun normalize(component: String): String {
        val inner = component.substringAfter("{").substringBefore("}")
        return inner.ifBlank { component }
    }

    /** Returns the icon pack drawable for an app, or null when not themed. */
    fun iconFor(packPackage: String, app: AppInfo): Drawable? {
        if (packPackage.isBlank()) return null
        val key = app.key
        val drawableName = mapping(packPackage)[key] ?: return null
        return try {
            val res = pm.getResourcesForApplication(packPackage)
            val id = res.getIdentifier(drawableName, "drawable", packPackage)
            if (id != 0) res.getDrawable(id, null) else null
        } catch (_: Exception) {
            null
        }
    }
}
