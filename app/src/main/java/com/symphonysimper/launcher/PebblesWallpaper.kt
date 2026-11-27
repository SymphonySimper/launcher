package com.symphonysimper.launcher

import android.app.WallpaperColors
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File
import kotlin.math.max

/**
 * The launcher's wallpaper: generated Pebbles, or the user's own photos, picked by dark mode.
 * It only draws when the surface changes or dark mode flips; nothing animates.
 */
class PebblesWallpaper : WallpaperService() {
    private val engines = mutableSetOf<PebblesEngine>()

    override fun onCreateEngine(): Engine = PebblesEngine()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        engines.forEach { it.draw() }
    }

    private inner class PebblesEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val prefs = LauncherApp.instance.prefs
        private var width = 0
        private var height = 0
        private var photo: Bitmap? = null
        private var photoKey: String? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            engines += this
            setOffsetNotificationsEnabled(false)
            prefs.sp.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            engines -= this
            prefs.sp.unregisterOnSharedPreferenceChangeListener(this)
            photo?.recycle()
            super.onDestroy()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
            draw()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) = draw()

        override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
            if (key in WALLPAPER_KEYS) draw()
        }

        // Plain grey: Android then picks its own default system colours.
        override fun onComputeColors() = WallpaperColors(android.graphics.Color.valueOf(0xFF808080.toInt()), null, null)

        fun draw() {
            if (width == 0 || height == 0) return
            val dark = isDark(this@PebblesWallpaper)
            val holder = surfaceHolder
            val canvas = try { holder.lockHardwareCanvas() } catch (_: Exception) { null } ?: return
            try {
                val image = if (prefs.wallpaperImages) photoFor(dark) else null
                if (image != null) drawCentreCrop(canvas, image) else Pebbles.draw(canvas, width.toFloat(), height.toFloat(), dark)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun photoFor(dark: Boolean): Bitmap? {
            val file = wallpaperFile(this@PebblesWallpaper, dark && !prefs.sameImage)
            val key = "${file.name}:${prefs.wallpaperVersion}:$width"
            if (key == photoKey) return photo
            photo?.recycle()
            photo = decodeSampled(file, width, height)
            photoKey = key
            return photo
        }

        private fun drawCentreCrop(canvas: Canvas, image: Bitmap) {
            val scale = max(width / image.width.toFloat(), height / image.height.toFloat())
            val matrix = Matrix().apply {
                setScale(scale, scale)
                postTranslate((width - image.width * scale) / 2f, (height - image.height * scale) / 2f)
            }
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(image, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        }
    }

    companion object {
        private val WALLPAPER_KEYS = setOf("wallpaper_images", "same_image", "wallpaper_version")

        fun isDark(context: Context) =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        fun wallpaperFile(context: Context, dark: Boolean) =
            File(context.filesDir, if (dark) "wallpaper_dark" else "wallpaper_light")

        /** Decodes [file] no larger than needed to cover [w]x[h]. */
        fun decodeSampled(file: File, w: Int, h: Int): Bitmap? {
            if (!file.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2
            return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}

/** The generated wallpaper. Drawn in a 360x800 space, scaled to cover the screen. */
object Pebbles {
    private const val W = 360f
    private const val H = 800f

    fun draw(canvas: Canvas, width: Float, height: Float, dark: Boolean) {
        val bg = if (dark) 0xFF000000.toInt() else 0xFFF1F1F1.toInt()
        val main = if (dark) 0xFF262626.toInt() else 0xFFD3D3D3.toInt()
        val soft = if (dark) 0xFF171717.toInt() else 0xFFE2E2E2.toInt()
        val line = if (dark) 0xFFE6E6E6.toInt() else 0xFF111111.toInt()

        canvas.drawColor(bg)
        val scale = max(width / W, height / H)
        canvas.save()
        canvas.translate((width - W * scale) / 2f, (height - H * scale) / 2f)
        canvas.scale(scale, scale)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = line
        }
        // Kept clear of the clock text, the dock and the search bar.
        fill.color = soft
        canvas.drawCircle(60f, 170f, 92f, fill)

        fill.color = main
        canvas.withRotation(-35f, 285f, 330f) { drawRoundRect(RectF(150f, 278f, 420f, 382f), 52f, 52f, fill) }
        canvas.withRotation(16f, 105f, 470f) { drawRoundRect(RectF(30f, 395f, 180f, 545f), 50f, 50f, fill) }

        canvas.drawCircle(296f, 500f, 36f, stroke)
        fill.color = line
        canvas.drawCircle(214f, 420f, 9f, fill)
        canvas.restore()
    }

    private inline fun Canvas.withRotation(degrees: Float, px: Float, py: Float, block: Canvas.() -> Unit) {
        save()
        rotate(degrees, px, py)
        block()
        restore()
    }
}
