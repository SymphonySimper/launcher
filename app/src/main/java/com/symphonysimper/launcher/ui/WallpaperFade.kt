package com.symphonysimper.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import com.symphonysimper.launcher.LauncherApp
import com.symphonysimper.launcher.Pebbles
import com.symphonysimper.launcher.ui.theme.isDark
import kotlinx.coroutines.flow.first

@Composable
fun WallpaperFade() {
    val wallpaper = LauncherApp.instance.wallpaper
    val dark = MaterialTheme.colorScheme.isDark
    val spec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    var incoming by remember { mutableStateOf<Boolean?>(null) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(dark) {
        incoming = null
        if (!wallpaper.showsPebbles(!dark) || !wallpaper.isOurs) return@LaunchedEffect
        incoming = dark
        fade.snapTo(0f)
        fade.animateTo(1f, spec)
        snapshotFlow { wallpaper.showsPebbles(dark) }.first { it }
        incoming = null
    }
    val showing = incoming ?: return
    Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }) {
        drawIntoCanvas {
            val canvas = it.nativeCanvas
            // The system draws a still wallpaper zoomed in on home (Android's default, 110%) and
            // zooms out only while the shade is open.
            canvas.scale(SystemZoom, SystemZoom, size.width / 2f, size.height / 2f)
            Pebbles.draw(canvas, size.width, size.height, showing)
        }
    }
}

private const val SystemZoom = 1.1f
