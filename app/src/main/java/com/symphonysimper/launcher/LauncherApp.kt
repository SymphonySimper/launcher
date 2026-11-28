package com.symphonysimper.launcher

import android.app.Application
import android.content.res.Configuration

class LauncherApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var apps: AppRepository
        private set
    lateinit var wallpaper: Wallpaper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        apps = AppRepository(this, prefs)
        apps.start()
        wallpaper = Wallpaper(this, prefs)
        wallpaper.follow()
        StayAwake.rewatchAfterRestart(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        wallpaper.follow()
    }

    companion object {
        lateinit var instance: LauncherApp
            private set
    }
}
