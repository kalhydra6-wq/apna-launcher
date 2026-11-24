package com.apnalauncher.data

import android.content.Context

/** Lightweight launch counter used for "Suggested" and "Recent" apps. */
object AppUsageStore {
    private const val PREFS = "apna_usage"

    fun record(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS, 0)
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun top(context: Context, limit: Int = 8): List<String> {
        val prefs = context.getSharedPreferences(PREFS, 0)
        return prefs.all
            .filterValues { it is Int }
            .toList()
            .sortedByDescending { (it.second as? Int) ?: 0 }
            .take(limit)
            .map { it.first }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, 0).edit().clear().apply()
    }
}
