package com.symphonysimper.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings

/**
 * Keeps the screen on by setting the system screen timeout to "never", and puts the old timeout
 * back when turned off. Turned on from home's long-press menu; the chip under the date turns it off,
 * and so does locking the phone, so it can't be left on by mistake.
 */
object StayAwake {
    private const val NEVER = Int.MAX_VALUE
    private const val DEFAULT_TIMEOUT = 60_000

    private val prefs get() = LauncherApp.instance.prefs

    /** The screen timeout setting, to watch for changes made anywhere. */
    val timeoutUri: Uri = Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT)

    /** On only if we turned it on and nobody changed the timeout since. */
    fun isOn(context: Context) = prefs.stayAwake && timeout(context) == NEVER

    /** Changing the screen timeout needs the "Modify system settings" permission. */
    fun canTurnOn(context: Context) = Settings.System.canWrite(context)

    fun set(context: Context, on: Boolean) {
        if (on) {
            // Never keep "never" as the timeout to go back to, or turning it off would leave the screen on.
            timeout(context).takeIf { it != NEVER }?.let { prefs.savedTimeout = it }
            putTimeout(context, NEVER)
        } else {
            putTimeout(context, prefs.savedTimeout)
        }
        prefs.stayAwake = on
        watchScreenOff(context, on)
    }

    /** When the launcher starts with Stay awake still on, it listens for the screen going off again. */
    fun resume(context: Context) {
        if (isOn(context)) watchScreenOff(context, true)
    }

    // Locking the phone turns Stay awake off. Screen off is only sent to receivers registered at run
    // time, so this one lives in the app, not on screen, but only while Stay awake is on: it does
    // nothing until the screen goes off, once.
    private var screenOff: BroadcastReceiver? = null

    private fun watchScreenOff(context: Context, on: Boolean) {
        val app = context.applicationContext
        screenOff?.let { app.unregisterReceiver(it) }
        screenOff = null
        if (!on) return
        screenOff = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                // If the timeout was changed elsewhere meanwhile, leave it be and just stop listening.
                if (isOn(app)) set(app, false) else watchScreenOff(app, false)
            }
        }.also { app.registerReceiver(it, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED) }
    }

    private fun timeout(context: Context) =
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_TIMEOUT)

    private fun putTimeout(context: Context, ms: Int) {
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms)
    }
}
