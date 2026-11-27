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
        // Android drops our notifications when the app is updated or the phone restarts, but Stay
        // awake stays on; put its status bar icon back each time the launcher starts.
        StayAwake.updateNotification(this)
    }

    companion object {
        lateinit var instance: LauncherApp
            private set
    }
}
