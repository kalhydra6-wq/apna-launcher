package com.apnalauncher

import android.app.Application
import com.apnalauncher.data.AppRepository

/** Process-wide repository so receivers, services and UI share one app list. */
class ApnaLauncherApp : Application() {
    lateinit var repository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(this)
    }

    val repo: AppRepository get() = repository
}
