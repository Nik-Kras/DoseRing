package com.nikkras.dosering.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap

/**
 * Draws the progress ring for a widget tile. Colours are derived from the medicine colour
 * only, so the bitmap reads well on both light and dark home screens.
 */
object RingRenderer {

    private const val STROKE = 0.095f
    private const val TRACK_ALPHA = 0x40
    private const val FILL_READY_ALPHA = 0x33
    private const val FILL_FILLING_ALPHA = 0x14

    fun ring(sizePx: Int, color: Int, progress: Float, label: String, ready: Boolean): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val stroke = sizePx * STROKE
        val bounds = ringBounds(sizePx, stroke)

        canvas.drawOval(
            RectF(bounds).apply { inset(stroke / 2f, stroke / 2f) },
            paint(ColorUtils.setAlphaComponent(color, if (ready) FILL_READY_ALPHA else FILL_FILLING_ALPHA)),
        )
        canvas.drawOval(bounds, strokePaint(ColorUtils.setAlphaComponent(color, TRACK_ALPHA), stroke))

        val arc = strokePaint(color, stroke).apply { strokeCap = Paint.Cap.ROUND }
        when {
            ready || progress >= 1f -> canvas.drawOval(bounds, arc)
            progress > 0f -> canvas.drawArc(bounds, -90f, 360f * progress, false, arc)
        }

        val text = paint(color).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textSize = sizePx * if (label.length > 1) 0.27f else 0.34f
        }
        canvas.drawText(label, sizePx / 2f, sizePx / 2f - (text.descent() + text.ascent()) / 2f, text)
        return bitmap
    }

    /** The nameless "+" tile. */
    fun plus(sizePx: Int, color: Int): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val stroke = sizePx * 0.045f
        val bounds = ringBounds(sizePx, sizePx * STROKE)
        val dash = sizePx * 0.07f
        canvas.drawOval(bounds, strokePaint(color, stroke).apply { pathEffect = DashPathEffect(floatArrayOf(dash, dash * 0.8f), 0f) })

        val arm = sizePx * 0.15f
        val center = sizePx / 2f
        val plus = strokePaint(color, sizePx * 0.065f).apply { strokeCap = Paint.Cap.ROUND }
        canvas.drawLine(center - arm, center, center + arm, center, plus)
        canvas.drawLine(center, center - arm, center, center + arm, plus)
        return bitmap
    }

    private fun ringBounds(sizePx: Int, stroke: Float): RectF {
        val inset = stroke / 2f + sizePx * 0.02f
        return RectF(inset, inset, sizePx - inset, sizePx - inset)
    }

    private fun paint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun strokePaint(color: Int, width: Float) = paint(color).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
    }
}
