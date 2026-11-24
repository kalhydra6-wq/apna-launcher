package com.apnalauncher.icons

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.apnalauncher.data.AppInfo
import com.apnalauncher.data.LauncherSettings

/** Loads and caches app icons as compose ImageBitmaps so scrolling stays smooth. */
class IconCache(private val context: Context, private val packs: IconPackManager) {
    private val pm: PackageManager = context.packageManager
    private val cache = object : LruCache<String, ImageBitmap>(320) {}

    fun get(app: AppInfo, settings: LauncherSettings): ImageBitmap {
        cache.get(app.key)?.let { return it }
        val size = (192 * settings.iconSize).toInt().coerceIn(72, 512)
        val drawable: Drawable? = packs.iconFor(settings.iconPack, app)
            ?: try {
                pm.getApplicationIcon(app.packageName)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        val bitmap = drawable?.toBitmap(size) ?: Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val image = bitmap.asImageBitmap()
        cache.put(app.key, image)
        return image
    }

    fun clear() = cache.evictAll()

    private fun Drawable.toBitmap(size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        // Adaptive icons need their foreground/background layers drawn explicitly.
        val d: Drawable = if (this is AdaptiveIconDrawable) {
            android.graphics.drawable.LayerDrawable(arrayOf(background, foreground)).also {
                it.setBounds(0, 0, size, size)
            }
        } else this
        d.setBounds(0, 0, size, size)
        d.draw(canvas)
        return output
    }
}
