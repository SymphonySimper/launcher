package com.symphonysimper.launcher

import android.app.WallpaperColors
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
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
        private var shownKey: String? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            engines += this
            if (!isPreview) liveEngines++
            setOffsetNotificationsEnabled(false)
            prefs.sp.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            engines -= this
            if (!isPreview) liveEngines--
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

        /** Material You builds the system colours from these: the photo's own, or the Pebbles palette. */
        override fun onComputeColors(): WallpaperColors {
            val dark = isDark(this@PebblesWallpaper)
            val image = if (prefs.wallpaperImages) photoFor(dark) else null
            return image?.let(WallpaperColors::fromBitmap) ?: Pebbles.colors(dark)
        }

        fun draw() {
            if (width == 0 || height == 0) return
            val dark = isDark(this@PebblesWallpaper)
            val holder = surfaceHolder
            val canvas = try { holder.lockHardwareCanvas() } catch (_: Exception) { null } ?: return
            val image = if (prefs.wallpaperImages) photoFor(dark) else null
            try {
                if (image != null) drawCentreCrop(canvas, image) else Pebbles.draw(canvas, width.toFloat(), height.toFloat(), dark)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
            // Something else is showing now, so the system colours should follow it.
            val shown = "$dark:${if (image != null) photoKey else "pebbles"}"
            if (shown != shownKey) {
                shownKey = shown
                notifyColorsChanged()
            }
        }

        private fun photoFor(dark: Boolean): Bitmap? {
            if (width == 0 || height == 0) return null
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
        /**
         * Engines drawing on the real home or lock screen, not a preview. The service shares the app's
         * process, so this is how settings knows Pebbles is set: Android won't tell other apps.
         */
        private var liveEngines by mutableIntStateOf(0)

        val isActive get() = liveEngines > 0

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

    /**
     * Like the Phone and Messages icons: shapes fade from lavender ([top]) to bright blue ([bottom]),
     * with a deeper blue copy ([back]) peeking out behind them at the top left.
     */
    private class Palette(val background: Long, val soft: Long, val softTop: Long, val top: Long, val bottom: Long, val back: Long)

    private val LIGHT = Palette(
        background = 0xFFFFFFFF, soft = 0xFFDCE5FB, softTop = 0xFFECEAFF,
        top = 0xFFA6A7FF, bottom = 0xFF3186FF, back = 0xFF336EF3,
    )
    private val DARK = Palette(
        background = 0xFF000000, soft = 0xFF12203F, softTop = 0xFF1F1D47,
        top = 0xFF8B8CF0, bottom = 0xFF2D78F0, back = 0xFF2457D2,
    )

    private fun palette(dark: Boolean) = if (dark) DARK else LIGHT

    /** The palette as wallpaper colours, so Material You themes the phone in the same blues. */
    fun colors(dark: Boolean): WallpaperColors {
        val p = palette(dark)
        return WallpaperColors(
            Color.valueOf(p.bottom.toInt()),
            Color.valueOf(p.top.toInt()),
            Color.valueOf(p.soft.toInt()),
            if (dark) WallpaperColors.HINT_SUPPORTS_DARK_THEME else WallpaperColors.HINT_SUPPORTS_DARK_TEXT,
        )
    }

    fun draw(canvas: Canvas, width: Float, height: Float, dark: Boolean) {
        val p = palette(dark)
        canvas.drawColor(p.background.toInt())
        val scale = max(width / W, height / H)
        canvas.save()
        canvas.translate((width - W * scale) / 2f, (height - H * scale) / 2f)
        canvas.scale(scale, scale)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Pebbles fall from the top right to the bottom left, growing as they go, between a soft
        // circle behind the clock and another bleeding off behind the dock.
        soft(canvas, paint, 58f, 150f, 104f, p.softTop, p.soft)
        soft(canvas, paint, 335f, 650f, 170f, p.soft, p.softTop)

        pebble(canvas, paint, p, roundRect(RectF(276f, 112f, 342f, 178f), 24f, 18f)) // clear of the clock
        pebble(canvas, paint, p, roundRect(RectF(170f, 248f, 360f, 352f), 52f, -30f))
        pebble(canvas, paint, p, roundRect(RectF(-52f, 405f, 218f, 565f), 80f, 20f))

        paint.style = Paint.Style.STROKE
        ring(canvas, paint, p, 118f, 285f, 28f, 3f)
        ring(canvas, paint, p, 300f, 455f, 20f, 2.5f)
        canvas.restore()
    }

    private fun soft(canvas: Canvas, paint: Paint, cx: Float, cy: Float, r: Float, from: Long, to: Long) {
        paint.shader = fade(cy - r, cy + r, from, to)
        canvas.drawCircle(cx, cy, r, paint)
    }

    private fun ring(canvas: Canvas, paint: Paint, p: Palette, cx: Float, cy: Float, r: Float, width: Float) {
        paint.strokeWidth = width
        paint.shader = fade(cy - r, cy + r, p.top, p.bottom)
        canvas.drawCircle(cx, cy, r, paint)
    }

    /** Draws [shape] twice: a deeper blue copy nudged up and left, then the faded shape over it. */
    private fun pebble(canvas: Canvas, paint: Paint, p: Palette, shape: Path) {
        paint.shader = null
        paint.color = p.back.toInt()
        canvas.save()
        canvas.translate(-7f, -6f)
        canvas.drawPath(shape, paint)
        canvas.restore()
        // The fade runs straight down the screen however the shape is turned, as on the icons, and
        // reaches full blue a little past halfway.
        val bounds = RectF().also { shape.computeBounds(it, true) }
        paint.shader = fade(bounds.top, bounds.top + bounds.height() * 0.65f, p.top, p.bottom)
        canvas.drawPath(shape, paint)
    }

    /** A rounded rectangle turned [degrees] about its centre. */
    private fun roundRect(rect: RectF, radius: Float, degrees: Float) = Path().apply {
        addRoundRect(rect, radius, radius, Path.Direction.CW)
        transform(Matrix().apply { setRotate(degrees, rect.centerX(), rect.centerY()) })
    }

    private fun fade(top: Float, bottom: Float, from: Long, to: Long) =
        LinearGradient(0f, top, 0f, bottom, from.toInt(), to.toInt(), Shader.TileMode.CLAMP)
}
