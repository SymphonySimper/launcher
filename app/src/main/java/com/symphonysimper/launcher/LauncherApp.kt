package com.symphonysimper.launcher

import android.app.Application

class LauncherApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var apps: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        apps = AppRepository(this, prefs)
        apps.start()
        StayAwake.resume(this)
    }

    companion object {
        lateinit var instance: LauncherApp
            private set
    }
}
