package com.symphonysimper.launcher.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleStartEffect
import com.symphonysimper.launcher.LauncherApp
import com.symphonysimper.launcher.StayAwake
import com.symphonysimper.launcher.start
import com.symphonysimper.launcher.ui.theme.Sizes

/** Stay awake as home sees it: whether it's on, and turning it on or off with the permissions it needs. */
@Stable
class StayAwakeSwitch internal constructor(private val context: Context) {
    var on by mutableStateOf(StayAwake.isOn(context))
        private set

    /** Turning on went to the "Modify system settings" screen; finish the job when home comes back. */
    internal var waitingForPermission = false
    internal var askNotifications: () -> Unit = {}

    fun set(on: Boolean) {
        if (on && !StayAwake.canTurnOn(context)) {
            waitingForPermission = true
            context.start(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:${context.packageName}".toUri()))
            return
        }
        StayAwake.set(context, on)
        this.on = StayAwake.isOn(context)
        // The status bar icon needs notification access; ask once, the first time it's turned on.
        val prefs = LauncherApp.instance.prefs
        if (on && !StayAwake.canNotify(context) && !prefs.notificationsAsked) {
            prefs.notificationsAsked = true
            askNotifications()
        }
    }

    /** Catches up with changes made elsewhere: the notification's Turn off, or the timeout changed in system settings. */
    internal fun refresh() {
        on = StayAwake.isOn(context)
        if (!on) StayAwake.updateNotification(context) // drop a stale status bar icon
    }
}

/** Keeps [StayAwakeSwitch.on] current while home is on screen, from timeout changes rather than polling. */
@Composable
fun rememberStayAwake(): StayAwakeSwitch {
    val context = LocalContext.current
    val switch = remember(context) { StayAwakeSwitch(context) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        StayAwake.updateNotification(context)
    }
    switch.askNotifications = { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
    LifecycleStartEffect(switch) {
        val wanted = switch.waitingForPermission
        switch.waitingForPermission = false
        if (wanted && StayAwake.canTurnOn(context)) switch.set(true) else switch.refresh()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = switch.refresh()
        }
        context.contentResolver.registerContentObserver(StayAwake.timeoutUri, false, observer)
        onStopOrDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return switch
}

/** Shown under the date while Stay awake is on. Tapping it turns Stay awake off. */
@Composable
fun StayAwakeChip(onTurnOff: () -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier
            // 32dp to look at, with the usual 48dp to hit.
            .minimumInteractiveComponentSize()
            .height(Sizes.Chip)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClickLabel = "Turn off Stay awake", role = Role.Button, onClick = onTurnOff)
            .padding(start = 10.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Ic.coffee, null, Modifier.size(18.dp), tint = color)
        Spacer(Modifier.width(6.dp))
        Text("Stay awake on", style = MaterialTheme.typography.labelLarge, color = color)
        Spacer(Modifier.width(8.dp))
        Icon(Ic.close, null, Modifier.size(16.dp), tint = color)
    }
}
