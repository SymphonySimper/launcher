package com.symphonysimper.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.symphonysimper.launcher.App
import com.symphonysimper.launcher.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun Home(
    dock: List<App>,
    onOpenDrawer: () -> Unit,
    onSearch: () -> Unit,
    onLauncherSettings: () -> Unit,
    onSystemSettings: () -> Unit,
) {
    val context = LocalContext.current
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { menuAt = it }) }
            .pointerInput(Unit) {
                var travelled = 0f
                var opened = false
                val threshold = 48.dp.toPx()
                detectVerticalDragGestures(
                    onDragStart = { travelled = 0f; opened = false },
                    onVerticalDrag = { _, dy ->
                        travelled += dy
                        if (!opened && travelled < -threshold) {
                            opened = true
                            onOpenDrawer()
                        }
                    },
                )
            },
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
                        app, 56.dp,
                        Modifier.clip(SquircleShape).combinedClickable(onClick = { context.launch(app) }),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Surface(
                onClick = onSearch,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp).height(52.dp),
            ) {
                Box(contentAlignment = Alignment.CenterStart) {
                    Icon(
                        Ic.search, "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 18.dp).size(26.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        menuAt?.let { at ->
            Box(Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }) {
                ExpressiveMenu(expanded = true, onDismissRequest = { menuAt = null }) {
                    DropdownMenuItem(
                        text = { Text("Launcher settings") },
                        leadingIcon = { Icon(Ic.tune, null) },
                        onClick = { menuAt = null; onLauncherSettings() },
                    )
                    DropdownMenuItem(
                        text = { Text("System settings") },
                        leadingIcon = { Icon(Ic.settings, null) },
                        onClick = { menuAt = null; onSystemSettings() },
                    )
                }
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
        Text(time.format(now), color = color, fontSize = 80.sp, lineHeight = 84.sp, letterSpacing = (-2).sp)
        Text(
            date.format(now),
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 3.dp),
        )
    }
}
