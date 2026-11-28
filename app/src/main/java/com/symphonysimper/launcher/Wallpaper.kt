package com.symphonysimper.launcher

import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_LOCK
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.util.Size
import android.view.Display
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class Wallpaper(private val context: Context, private val prefs: Prefs) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val manager = context.getSystemService(WallpaperManager::class.java)
    private val worker = Dispatchers.Default.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + worker)

    init {
        prefs.sp.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        if (key in SOURCE_KEYS) follow()
    }

    val isOurs get() = manager.getWallpaperId(FLAG_SYSTEM) == prefs.wallpaperId

    fun photo(dark: Boolean) = File(context.filesDir, if (dark) "wallpaper_dark" else "wallpaper_light")

    fun follow() {
        scope.launch { runCatching { show(Takeover.OnlyIfOurs) } }
    }

    suspend fun set() = withContext(worker) { runCatching { show(Takeover.Always) }.isSuccess }

    private fun show(takeover: Takeover) {
        val dark = context.isDarkMode()
        val photo = photo(dark && !prefs.sameImage).takeIf { prefs.wallpaperImages && it.exists() }
        val shown = if (photo != null) "${photo.name}:${prefs.wallpaperVersion}" else "pebbles:$dark"
        if (takeover == Takeover.OnlyIfOurs && (shown == prefs.wallpaperShown || !isOurs)) return
        val which = if (manager.getWallpaperId(FLAG_LOCK) < 0) FLAG_SYSTEM or FLAG_LOCK else FLAG_SYSTEM
        val image = photo ?: pebbles(dark)
        val crop = photo?.let(::centreCrop)
        // The system forgets the old wallpaper's colours as soon as a set starts, and re-reads them
        // from the old image if anyone asks before the new one is in, so hand it a finished file.
        val id = try {
            image.inputStream().use { manager.setStream(it, crop, true, which) }
        } finally {
            if (photo == null) image.delete()
        }
        check(id > 0)
        prefs.wallpaperId = id
        prefs.wallpaperShown = shown
    }

    private fun pebbles(dark: Boolean): File {
        val screen = screenSize()
        val bitmap = Bitmap.createBitmap(screen.width, screen.height, Bitmap.Config.ARGB_8888)
        Pebbles.draw(Canvas(bitmap), screen.width.toFloat(), screen.height.toFloat(), dark)
        val file = File(context.cacheDir, "pebbles.png")
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    private fun centreCrop(file: File): Rect {
        val image = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, image)
        val screen = screenSize()
        val scale = min(image.outWidth / screen.width.toFloat(), image.outHeight / screen.height.toFloat())
        val width = (screen.width * scale).toInt()
        val height = (screen.height * scale).toInt()
        val left = (image.outWidth - width) / 2
        val top = (image.outHeight - height) / 2
        return Rect(left, top, left + width, top + height)
    }

    private fun screenSize(): Size {
        val mode = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY).mode
        return Size(min(mode.physicalWidth, mode.physicalHeight), max(mode.physicalWidth, mode.physicalHeight))
    }

    private enum class Takeover { Always, OnlyIfOurs }

    private companion object {
        val SOURCE_KEYS = setOf("wallpaper_images", "same_image", "wallpaper_version")
    }
}

fun Context.isDarkMode() = resources.configuration.isNightModeActive

fun decodeSampled(file: File, w: Int, h: Int): Bitmap? {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2
    val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val scale = max(w / decoded.width.toFloat(), h / decoded.height.toFloat())
    if (scale >= 1f) return decoded
    val scaled = Bitmap.createScaledBitmap(decoded, ceil(decoded.width * scale).toInt(), ceil(decoded.height * scale).toInt(), true)
    if (scaled !== decoded) decoded.recycle()
    return scaled
}
