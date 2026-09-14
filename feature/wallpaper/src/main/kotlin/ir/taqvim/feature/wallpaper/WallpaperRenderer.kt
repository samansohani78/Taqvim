/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.wallpaper

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.graphics.withTranslation
import ir.taqvim.core.ui.painter.MonthBitmapPainter
import ir.taqvim.core.ui.painter.MoonBitmapPainter
import ir.taqvim.core.ui.painter.PainterEnvironment
import ir.taqvim.core.ui.painter.PainterPalette

/** An axis-aligned box in pixels. */
data class Box(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
}

/**
 * Where the wallpaper's parts go on a [width] × [height] surface. Portrait: the date on top, the Moon beside it and
 * the month below, clear of the status bar and of the launcher's dock. Landscape: the date and Moon on the start half
 * and the month on the other.
 */
data class WallpaperLayout(
    val title: Box,
    val moon: Box,
    val month: Box,
) {
    companion object {
        private const val MARGIN = 0.06f
        private const val TOP = 0.12f
        private const val TITLE_HEIGHT = 0.12f
        private const val MOON = 0.22f
        private const val MONTH_TOP = 0.36f
        private const val MONTH_BOTTOM = 0.78f
        private const val HALF = 0.5f

        fun of(
            width: Int,
            height: Int,
        ): WallpaperLayout {
            require(width > 0 && height > 0) { "Surface size must be positive: $width × $height" }
            val w = width.toFloat()
            val h = height.toFloat()
            val margin = minOf(w, h) * MARGIN
            return if (h >= w) {
                val moonSize = w * MOON
                val top = h * TOP
                WallpaperLayout(
                    title = Box(margin, top + moonSize, w - margin, top + moonSize + h * TITLE_HEIGHT),
                    moon = Box((w - moonSize) / 2f, top, (w + moonSize) / 2f, top + moonSize),
                    month = Box(margin, h * MONTH_TOP + moonSize, w - margin, h * MONTH_BOTTOM + moonSize / 2f),
                )
            } else {
                val half = w * HALF
                val moonSize = h * MOON
                WallpaperLayout(
                    title = Box(margin, h * HALF, half - margin, h * HALF + h * TITLE_HEIGHT * 2f),
                    moon = Box((half - moonSize) / 2f, h * HALF - moonSize - margin, (half + moonSize) / 2f, h * HALF),
                    month = Box(half, h * TOP, w - margin, h - margin),
                )
            }
        }
    }
}

/**
 * Draws [WallpaperContent] (T-1215): a day or night sky gradient with a few fixed stars at night, the Moon, today's
 * date and the month grid of the T-702 painters. Only draws when asked; the engine decides when (see
 * [WallpaperRedrawPolicy]). Not thread-safe.
 */
class WallpaperRenderer(
    private val environment: PainterEnvironment,
) {
    private val palette =
        PainterPalette(
            background = 0,
            surfaceHighest = SHADOW,
            onSurface = TEXT,
            onSurfaceVariant = TEXT_VARIANT,
            outline = TEXT_VARIANT,
            outlineVariant = OUTLINE_VARIANT,
            primary = ACCENT,
            onPrimary = ON_ACCENT,
            error = HOLIDAY,
            tertiary = ACCENT,
            land = SHADOW,
        )
    private val month = MonthBitmapPainter(palette, environment)
    private val moon = MoonBitmapPainter(palette, environment)
    private val sky = Paint()
    private val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TEXT }
    private val text =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = environment.typeface
        }

    /** Draws [content] filling [width] × [height] pixels of [canvas]. */
    fun draw(
        canvas: Canvas,
        content: WallpaperContent,
        width: Int,
        height: Int,
    ) {
        val layout = WallpaperLayout.of(width, height)
        val (top, bottom) = if (content.daylight) DAY_TOP to DAY_BOTTOM else NIGHT_TOP to NIGHT_BOTTOM
        sky.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), sky)
        if (!content.daylight) drawStars(canvas, width, height)
        canvas.withTranslation(layout.moon.left, layout.moon.top) {
            moon.draw(this, content.moon, layout.moon.width.toInt(), layout.moon.height.toInt())
        }
        drawTitle(canvas, content, layout.title)
        canvas.withTranslation(layout.month.left, layout.month.top) {
            month.draw(this, content.month, layout.month.width.toInt(), layout.month.height.toInt())
        }
    }

    private fun drawTitle(
        canvas: Canvas,
        content: WallpaperContent,
        box: Box,
    ) {
        text.color = TEXT
        text.textSize = environment.textPx(TITLE_SP)
        fit(content.title, box.width)
        val titleBaseline = box.top + text.textSize
        canvas.drawText(content.title, box.centerX, titleBaseline, text)
        text.color = TEXT_VARIANT
        text.textSize = environment.textPx(SUBTITLE_SP)
        fit(content.subtitle, box.width)
        canvas.drawText(content.subtitle, box.centerX, titleBaseline + text.textSize * LINE, text)
    }

    private fun fit(
        value: String,
        width: Float,
    ) {
        val measured = text.measureText(value)
        if (measured > width) text.textSize *= width / measured
    }

    private fun drawStars(
        canvas: Canvas,
        width: Int,
        height: Int,
    ) {
        val radius = environment.px(STAR_DP)
        WallpaperStars.positions(STARS).forEach { (x, y) ->
            canvas.drawCircle(x * width, y * height * STAR_BAND, radius, star)
        }
    }

    private companion object {
        const val TITLE_SP = 28f
        const val SUBTITLE_SP = 16f
        const val LINE = 1.4f
        const val STAR_DP = 1f
        const val STARS = 40
        const val STAR_BAND = 0.4f
        const val DAY_TOP = 0xFF3F7FBF.toInt()
        const val DAY_BOTTOM = 0xFF9CC7E8.toInt()
        const val NIGHT_TOP = 0xFF050A1F.toInt()
        const val NIGHT_BOTTOM = 0xFF1B2A4E.toInt()
        const val TEXT = 0xFFF5F7FA.toInt()
        const val TEXT_VARIANT = 0xFFD0D8E4.toInt()
        const val OUTLINE_VARIANT = 0x66FFFFFF
        const val SHADOW = 0xFF3A4660.toInt()
        const val ACCENT = 0xFFFFD27A.toInt()
        const val ON_ACCENT = 0xFF1B1300.toInt()
        const val HOLIDAY = 0xFFFFB4AB.toInt()
    }
}

/** Fixed star positions as fractions of the sky: a low-discrepancy (golden ratio) sequence, the same every night. */
object WallpaperStars {
    private const val GOLDEN = 0.618034f
    private const val PLASTIC = 0.754878f

    fun positions(count: Int): List<Pair<Float, Float>> =
        (1..count).map { index -> (index * GOLDEN) % 1f to (index * PLASTIC) % 1f }
}
