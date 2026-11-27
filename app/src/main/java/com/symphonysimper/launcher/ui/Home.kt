package com.symphonysimper.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.symphonysimper.launcher.App
import com.symphonysimper.launcher.launch
import com.symphonysimper.launcher.ui.theme.Sizes
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun Home(
    dock: List<App>,
    drawer: DrawerState,
    onLauncherSettings: () -> Unit,
    onSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { menuAt = it }) }
            // Swiping up pulls the drawer open with the finger.
            .draggable(
                rememberDraggableState { drawer.drag(it) },
                Orientation.Vertical,
                onDragStopped = { drawer.release(it) },
            ),
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Clock(Modifier.padding(start = 26.dp, top = 36.dp))
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                dock.forEach { app ->
                    AppIcon(
                        app, Sizes.AppIcon,
                        Modifier.clip(SquircleShape).combinedClickable(onClick = { context.launch(app) }),
                    )
                }
            }
            Spacer(Modifier.height(18.dp + searchBarSpace())) // the shared search bar sits here
        }

        menuAt?.let { at ->
            Box(Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }) {
                LauncherMenu(
                    listOf(
                        MenuEntry(Ic.tune, "Launcher settings", onLauncherSettings),
                        MenuEntry(Ic.settings, "System settings", onSystemSettings),
                    ),
                    arrow = false,
                    onDismiss = { menuAt = null },
                )
            }
        }
    }
}

/** Big time with the date below. Updates once a minute from the system tick. */
@Composable
private fun Clock(modifier: Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                now = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        now = System.currentTimeMillis()
        onDispose { context.unregisterReceiver(receiver) }
    }
    val locale = Locale.getDefault()
    val is24 = DateFormat.is24HourFormat(context)
    val time = remember(locale, is24) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm", locale) }
    val date = remember(locale) { SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale) }
    val color = MaterialTheme.colorScheme.onSurface
    Column(modifier) {
        Text(
            time.format(now),
            color = color,
            style = MaterialTheme.typography.displayLarge,
            fontSize = 84.sp,
            lineHeight = 88.sp,
            letterSpacing = (-3).sp,
        )
        Text(
            date.format(now),
            color = color.copy(alpha = 0.8f),
            fontSize = 19.sp,
            modifier = Modifier.padding(start = 3.dp, top = 4.dp),
        )
    }
}
