/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb

private const val TRANSPARENT = 0

/**
 * Colors of the bitmap painters (T-702) as `0xAARRGGBB` ints, taken from a Material color scheme so widget bitmaps
 * match the app theme. [background] is transparent unless the painter should fill the bitmap itself; [land] fills
 * closed map shapes and [surfaceHighest] is the Moon's shadow and the map's water.
 */
public data class PainterPalette(
    public val background: Int,
    public val surfaceHighest: Int,
    public val onSurface: Int,
    public val onSurfaceVariant: Int,
    public val outline: Int,
    public val outlineVariant: Int,
    public val primary: Int,
    public val onPrimary: Int,
    public val error: Int,
    public val tertiary: Int,
    public val land: Int,
) {
    public companion object {
        /** The palette of [scheme]; the bitmap background is filled with the scheme's surface when [opaque]. */
        public fun of(
            scheme: ColorScheme,
            opaque: Boolean = false,
        ): PainterPalette =
            PainterPalette(
                background = if (opaque) scheme.surface.toArgb() else TRANSPARENT,
                surfaceHighest = scheme.surfaceContainerHighest.toArgb(),
                onSurface = scheme.onSurface.toArgb(),
                onSurfaceVariant = scheme.onSurfaceVariant.toArgb(),
                outline = scheme.outline.toArgb(),
                outlineVariant = scheme.outlineVariant.toArgb(),
                primary = scheme.primary.toArgb(),
                onPrimary = scheme.onPrimary.toArgb(),
                error = scheme.error.toArgb(),
                tertiary = scheme.tertiary.toArgb(),
                land = scheme.secondaryContainer.toArgb(),
            )
    }
}

/**
 * Where a painter draws: [density] pixels per dp, the user's [fontScale], the writing direction of the app language
 * ([rtl]) and the [typeface] (e.g. a custom font of T-700). Texts drawn by painters are prepared by the caller in the
 * app's digits.
 */
public data class PainterEnvironment(
    public val density: Float,
    public val fontScale: Float = 1f,
    public val rtl: Boolean = false,
    public val typeface: Typeface = Typeface.DEFAULT,
) {
    init {
        require(density > 0f && fontScale > 0f) { "Density and font scale must be positive" }
    }

    /** [dp] in pixels. */
    public fun px(dp: Float): Float = dp * density

    /** A text size of [sp] in pixels, following the font scale. */
    public fun textPx(sp: Float): Float = sp * density * fontScale
}

/**
 * Draws a model of type [M] on a canvas (T-702). Widgets ask for a bitmap at their pixel size; screens can draw into
 * their own canvas. Implementations create their `Paint`s once and are not thread-safe: use one painter per thread.
 */
public interface BitmapPainter<in M> {
    /** Draws [model] filling [width] × [height] pixels of [canvas]. */
    public fun draw(
        canvas: Canvas,
        model: M,
        width: Int,
        height: Int,
    )

    /** A new ARGB bitmap of [widthPx] × [heightPx] showing [model]. */
    @SuppressLint("UseKtx") // androidx.core KTX is not a dependency of :core:ui.
    public fun paint(
        model: M,
        widthPx: Int,
        heightPx: Int,
    ): Bitmap {
        require(widthPx > 0 && heightPx > 0) { "Bitmap size must be positive: $widthPx × $heightPx" }
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), model, widthPx, heightPx)
        return bitmap
    }
}

/** A text paint in [environment]'s typeface, centered by default. */
internal fun textPaint(environment: PainterEnvironment): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = environment.typeface
        textAlign = Paint.Align.CENTER
    }

/** Sets this paint's text [size] and [color]. */
internal fun Paint.textStyle(
    size: Float,
    color: Int,
): Paint =
    apply {
        textSize = size
        this.color = color
    }

/**
 * Draws [text] with [paint]'s alignment at [x], vertically centered on [centerY], shrinking the paint's text size so
 * the text fits [maxWidth]. Empty texts draw nothing.
 */
internal fun Canvas.drawFittedText(
    text: String,
    x: Float,
    centerY: Float,
    maxWidth: Float,
    paint: Paint,
) {
    if (text.isEmpty()) return
    paint.textSize = TextFit.size(paint.textSize, paint.measureText(text), maxWidth)
    drawText(text, x, centerY - (paint.descent() + paint.ascent()) / 2, paint)
}
