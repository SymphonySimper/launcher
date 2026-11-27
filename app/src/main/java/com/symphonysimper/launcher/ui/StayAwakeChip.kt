package com.symphonysimper.launcher.ui

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
import com.symphonysimper.launcher.StayAwake
import com.symphonysimper.launcher.start
import com.symphonysimper.launcher.ui.theme.Sizes

@Stable
class StayAwakeSwitch internal constructor(private val context: Context) {
    var on by mutableStateOf(StayAwake.isOn(context))
        private set

    internal var waitingForPermission = false

    fun set(on: Boolean) {
        if (on && !StayAwake.canTurnOn(context)) {
            waitingForPermission = true
            context.start(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:${context.packageName}".toUri()))
            return
        }
        StayAwake.set(context, on)
        this.on = StayAwake.isOn(context)
    }

    internal fun refresh() {
        on = StayAwake.isOn(context)
    }
}

@Composable
fun rememberStayAwake(): StayAwakeSwitch {
    val context = LocalContext.current
    val switch = remember(context) { StayAwakeSwitch(context) }
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

@Composable
fun StayAwakeChip(onTurnOff: () -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        modifier
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
