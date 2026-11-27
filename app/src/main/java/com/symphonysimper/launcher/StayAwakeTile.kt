package com.symphonysimper.launcher

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.net.toUri

/**
 * Quick Settings tile. On: the screen never turns off. Off: back to the timeout
 * the user had before.
 */
class StayAwakeTile : TileService() {
    private val prefs get() = LauncherApp.instance.prefs

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        if (!Settings.System.canWrite(this)) {
            openAndCollapse(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:$packageName".toUri()))
            return
        }
        if (isOn()) {
            putTimeout(prefs.savedTimeout)
            prefs.stayAwake = false
        } else {
            prefs.savedTimeout = currentTimeout()
            putTimeout(NEVER)
            prefs.stayAwake = true
        }
        refresh()
    }

    /** On only if we turned it on and nobody changed the timeout since. */
    private fun isOn() = prefs.stayAwake && currentTimeout() == NEVER

    private fun currentTimeout() =
        Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_TIMEOUT)

    private fun putTimeout(ms: Int) {
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms)
    }

    private fun refresh() {
        val tile = qsTile ?: return
        tile.state = if (isOn()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openAndCollapse(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private companion object {
        const val NEVER = Int.MAX_VALUE
        const val DEFAULT_TIMEOUT = 60_000
    }
}
