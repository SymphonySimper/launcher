package com.symphonysimper.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.symphonysimper.launcher.calendarIntents
import com.symphonysimper.launcher.clockIntent
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
import androidx.lifecycle.compose.LifecycleStartEffect
import com.symphonysimper.launcher.App
import com.symphonysimper.launcher.LauncherApp
import com.symphonysimper.launcher.expandNotifications
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
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    val context = LocalContext.current
    var canPullShade by remember { mutableStateOf(false) }
    val stayAwake = rememberStayAwake()
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { menuAt = it }) }
            .draggable(
                rememberDraggableState { dy ->
                    drawer.drag(dy)
                    if (drawer.progress > 0f) canPullShade = false
                    // Android only lets the system drag the shade with a finger, so the next best thing is
                    // to start it the moment the swipe is known to be downward (past the touch slop).
                    if (canPullShade && dy > 0f) {
                        canPullShade = false
                        context.expandNotifications()
                    }
                },
                Orientation.Vertical,
                onDragStarted = { canPullShade = drawer.progress <= 0f },
                onDragStopped = { drawer.release(it) },
            ),
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Clock(Modifier.padding(start = ClockStart - ClockTapPadding, top = 36.dp))
            AnimatedVisibility(
                stayAwake.on,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(MaterialTheme.motionScheme.fastSpatialSpec(), expandFrom = Alignment.Top),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                    shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec(), shrinkTowards = Alignment.Top),
            ) {
                StayAwakeChip(onTurnOff = { stayAwake.set(false) }, Modifier.padding(start = ClockStart + DateIndent))
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                dock.forEach { app ->
                    val source = rememberLaunchSource()
                    AppIcon(
                        app, Sizes.AppIcon, source.modifier,
                        iconModifier = Modifier.clip(SquircleShape).combinedClickable(onClick = { source.launch(app) }),
                        badge = true,
                        themed = LauncherApp.instance.prefs.themedDock,
                    )
                }
            }
            Spacer(Modifier.height(18.dp + searchBarSpace()))
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
                    pill = MenuEntry(if (stayAwake.on) Ic.coffee else Ic.coffeeOutline, "Stay awake") {
                        stayAwake.set(!stayAwake.on)
                    },
                    pillOn = stayAwake.on,
                )
            }
        }
    }
}

private val ClockStart = 26.dp

private val DateIndent = 3.dp

private val ClockTapPadding = 8.dp

@Composable
private fun Clock(modifier: Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LifecycleStartEffect(Unit) {
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
        onStopOrDispose { context.unregisterReceiver(receiver) }
    }
    val locale = Locale.getDefault()
    val is24 = DateFormat.is24HourFormat(context)
    val time = remember(locale, is24) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm", locale) }
    val date = remember(locale) { SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale) }
    val color = MaterialTheme.colorScheme.onSurface
    val clockSource = rememberLaunchSource()
    val calendarSource = rememberLaunchSource()
    Column(modifier) {
        Text(
            time.format(now),
            color = color,
            style = MaterialTheme.typography.displayLarge,
            fontSize = 84.sp,
            lineHeight = 88.sp,
            letterSpacing = (-3).sp,
            modifier = Modifier
                .then(clockSource.modifier)
                .clip(ClockTapShape)
                .clickable { clockSource.open(clockIntent()) }
                .padding(horizontal = ClockTapPadding),
        )
        Text(
            date.format(now),
            color = color.copy(alpha = 0.8f),
            fontSize = 19.sp,
            modifier = Modifier
                .padding(start = DateIndent)
                .then(calendarSource.modifier)
                .clip(ClockTapShape)
                .clickable {
                    val (today, app) = calendarIntents(System.currentTimeMillis())
                    calendarSource.open(today, app)
                }
                // Compose widens the touch area of small targets to 48dp, so this only sizes the highlight.
                .padding(horizontal = ClockTapPadding, vertical = 4.dp),
        )
    }
}

private val ClockTapShape = RoundedCornerShape(16.dp)
