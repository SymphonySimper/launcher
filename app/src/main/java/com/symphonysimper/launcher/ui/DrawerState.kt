package com.symphonysimper.launcher.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * How far the app drawer is open, from 0 (closed) to 1 (open). A drag moves it with the finger over
 * [range] pixels; on release a fling decides where it goes, or else whichever end is nearer.
 */
@Stable
class DrawerState(
    private val scope: CoroutineScope,
    private val spec: AnimationSpec<Float>,
    private val flingVelocity: Float,
) {
    /** Can briefly overshoot 0..1 while springing into place. */
    var progress by mutableFloatStateOf(0f)
        private set

    /** Where the drawer is headed: true while it's opening or open. */
    var open by mutableStateOf(false)
        private set

    /** Finger travel, in pixels, from closed to open. */
    var range = 1f

    private var settling: Job? = null

    /** Moves with a drag of [dy] pixels (negative is up) and returns how much of it was used. */
    fun drag(dy: Float): Float {
        settling?.cancel()
        val before = progress.coerceIn(0f, 1f)
        progress = (before - dy / range).coerceIn(0f, 1f)
        return (before - progress) * range
    }

    /** Ends a drag let go at [velocity] pixels per second (negative is up). */
    fun release(velocity: Float) = animateTo(
        when {
            velocity < -flingVelocity -> true
            velocity > flingVelocity -> false
            else -> progress > 0.5f
        },
        velocity,
    )

    fun animateTo(open: Boolean, velocity: Float = 0f) {
        this.open = open
        settling?.cancel()
        settling = scope.launch {
            animate(progress, if (open) 1f else 0f, -velocity / range, spec) { value, _ -> progress = value }
        }
    }

    fun snapTo(open: Boolean) {
        settling?.cancel()
        this.open = open
        progress = if (open) 1f else 0f
    }

    /** Lets a list in the drawer pull it closed once the list is at the top. */
    val pullToClose = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            // Pushing back up mid-pull reopens the drawer before the list scrolls.
            if (source == NestedScrollSource.UserInput && available.y < 0f && progress < 1f) Offset(0f, drag(available.y))
            else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (source == NestedScrollSource.UserInput && available.y > 0f) Offset(0f, drag(available.y))
            else Offset.Zero

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (progress >= 1f) return Velocity.Zero
            release(available.y)
            return available
        }
    }
}

@Composable
fun rememberDrawerState(): DrawerState {
    val scope = rememberCoroutineScope()
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fling = with(LocalDensity.current) { 400.dp.toPx() }
    return remember { DrawerState(scope, spec, fling) }
}
