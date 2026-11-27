package com.symphonysimper.launcher

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.Settings

/**
 * Keeps the screen on by setting the system screen timeout to "never", and puts the old timeout
 * back when turned off. While it's on, a quiet notification shows its icon in the status bar.
 * Turned on from home's long-press menu; a chip under the date and the notification turn it off.
 */
object StayAwake {
    private const val NEVER = Int.MAX_VALUE
    private const val DEFAULT_TIMEOUT = 60_000
    private const val CHANNEL = "stay_awake_status"
    private const val NOTIFICATION_ID = 1

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
        updateNotification(context)
    }

    /** Shows the notification while Stay awake is on and removes it otherwise. */
    fun updateNotification(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!isOn(context) || !canNotify(context)) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        // Default importance, since Android hides silent notifications' status bar icons; but no sound or buzz.
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.stay_awake_label), NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
        val turnOff = PendingIntent.getBroadcast(
            context, 0, Intent(context, StayAwakeOffReceiver::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val icon = Icon.createWithResource(context, R.drawable.ic_stay_awake)
        manager.notify(
            NOTIFICATION_ID,
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(icon)
                .setContentTitle("Stay awake is on")
                .setContentText("The screen won't turn off by itself")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(Notification.Action.Builder(icon, "Turn off", turnOff).build())
                .build(),
        )
    }

    fun canNotify(context: Context) =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun timeout(context: Context) =
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_TIMEOUT)

    private fun putTimeout(context: Context, ms: Int) {
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms)
    }
}

/** The notification's "Turn off" button. */
class StayAwakeOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = StayAwake.set(context, false)
}
