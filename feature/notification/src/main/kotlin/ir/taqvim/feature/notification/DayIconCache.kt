/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap

/** How a day icon is drawn: a bare white number (a status bar icon) or a white number on a colored disc. */
enum class DayIconStyle { GLYPH, BADGE }

/**
 * Day icons (T-1213, T-1215): today's number in the app's digits drawn into a square bitmap with the system's bold
 * font, fitted to the width. Icons are kept per text, size, style and color in a small least-recently-used cache, so
 * each day is drawn once however often the notification or tile is refreshed.
 */
class DayIconCache(
    private val capacity: Int = DEFAULT_CAPACITY,
) {
    private val bitmaps =
        object : LinkedHashMap<Key, Bitmap>(capacity, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Bitmap>): Boolean = size > capacity
        }
    private val text =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    /** How many icons were drawn rather than taken from the cache. */
    @get:Synchronized
    var drawn: Int = 0
        private set

    init {
        require(capacity > 0) { "Capacity must be positive: $capacity" }
    }

    /** The icon of [number] at [sizePx] × [sizePx]; [color] fills the disc of a [DayIconStyle.BADGE]. */
    @Synchronized
    fun icon(
        number: String,
        sizePx: Int,
        style: DayIconStyle,
        color: Int = Color.TRANSPARENT,
    ): Bitmap {
        require(sizePx > 0) { "Icon size must be positive: $sizePx" }
        return bitmaps.getOrPut(Key(number, sizePx, style, color)) {
            drawn++
            draw(number, sizePx, style, color)
        }
    }

    private fun draw(
        number: String,
        sizePx: Int,
        style: DayIconStyle,
        color: Int,
    ): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val half = sizePx / 2f
        if (style == DayIconStyle.BADGE) {
            fill.color = color
            canvas.drawCircle(half, half, half, fill)
        }
        val fraction = if (style == DayIconStyle.BADGE) BADGE_TEXT else GLYPH_TEXT
        text.textSize = sizePx * fraction
        val widest = sizePx * fraction
        val width = text.measureText(number)
        if (width > widest) text.textSize *= widest / width
        canvas.drawText(number, half, half - (text.descent() + text.ascent()) / 2f, text)
        return bitmap
    }

    private data class Key(
        val number: String,
        val sizePx: Int,
        val style: DayIconStyle,
        val color: Int,
    )

    private companion object {
        /** Icons of a few days in the notification's two sizes and the tile's. */
        const val DEFAULT_CAPACITY = 8
        const val LOAD_FACTOR = 0.75f
        const val GLYPH_TEXT = 0.9f
        const val BADGE_TEXT = 0.6f
    }
}
