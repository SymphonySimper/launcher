package com.symphonysimper.launcher

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max

object Pebbles {
    private const val W = 360f
    private const val H = 800f

    private const val TOP = 0xFFA6A7FF
    private const val BOTTOM = 0xFF3186FF
    private const val BACK = 0xFF336EF3

    private class Backdrop(val background: Long, val soft: Long, val softTop: Long)

    private val LIGHT = Backdrop(background = 0xFFFFFFFF, soft = 0xFFDCE5FB, softTop = 0xFFECEAFF)
    private val DARK = Backdrop(background = 0xFF000000, soft = 0xFF12203F, softTop = 0xFF1F1D47)

    fun draw(canvas: Canvas, width: Float, height: Float, dark: Boolean) {
        val b = if (dark) DARK else LIGHT
        canvas.drawColor(b.background.toInt())
        val scale = max(width / W, height / H)
        canvas.save()
        canvas.translate((width - W * scale) / 2f, (height - H * scale) / 2f)
        canvas.scale(scale, scale)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        soft(canvas, paint, 58f, 150f, 104f, b.softTop, b.soft)
        soft(canvas, paint, 335f, 650f, 170f, b.soft, b.softTop)

        pebble(canvas, paint, roundRect(RectF(276f, 112f, 342f, 178f), 24f, 18f))
        pebble(canvas, paint, roundRect(RectF(170f, 248f, 360f, 352f), 52f, -30f))
        pebble(canvas, paint, roundRect(RectF(-52f, 405f, 218f, 565f), 80f, 20f))

        paint.style = Paint.Style.STROKE
        ring(canvas, paint, 118f, 285f, 28f, 3f)
        ring(canvas, paint, 300f, 455f, 20f, 2.5f)
        canvas.restore()
    }

    private fun soft(canvas: Canvas, paint: Paint, cx: Float, cy: Float, r: Float, from: Long, to: Long) {
        paint.shader = fade(cy - r, cy + r, from, to)
        canvas.drawCircle(cx, cy, r, paint)
    }

    private fun ring(canvas: Canvas, paint: Paint, cx: Float, cy: Float, r: Float, width: Float) {
        paint.strokeWidth = width
        paint.shader = fade(cy - r, cy + r, TOP, BOTTOM)
        canvas.drawCircle(cx, cy, r, paint)
    }

    private fun pebble(canvas: Canvas, paint: Paint, shape: Path) {
        paint.shader = null
        paint.color = BACK.toInt()
        // The system takes the wallpaper's main colour from this flat blue. Edge pixels blended into
        // white or black shift it by a step between light and dark, and any change re-themes the phone.
        paint.isAntiAlias = false
        canvas.save()
        canvas.translate(-7f, -6f)
        canvas.drawPath(shape, paint)
        canvas.restore()
        paint.isAntiAlias = true
        val bounds = RectF().also { shape.computeBounds(it, true) }
        paint.shader = fade(bounds.top, bounds.top + bounds.height() * 0.65f, TOP, BOTTOM)
        canvas.drawPath(shape, paint)
    }

    private fun roundRect(rect: RectF, radius: Float, degrees: Float) = Path().apply {
        addRoundRect(rect, radius, radius, Path.Direction.CW)
        transform(Matrix().apply { setRotate(degrees, rect.centerX(), rect.centerY()) })
    }

    private fun fade(top: Float, bottom: Float, from: Long, to: Long) =
        LinearGradient(0f, top, 0f, bottom, from.toInt(), to.toInt(), Shader.TileMode.CLAMP)
}
