/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import android.view.View
import androidx.compose.ui.graphics.toArgb
import androidx.glance.color.ColorProviders
import androidx.glance.unit.ColorProvider
import ir.taqvim.core.ui.component.MonthGridModel
import ir.taqvim.core.ui.component.SunArcModel
import ir.taqvim.core.ui.painter.BitmapPainter
import ir.taqvim.core.ui.painter.MonthBitmapPainter
import ir.taqvim.core.ui.painter.MoonBitmapModel
import ir.taqvim.core.ui.painter.MoonBitmapPainter
import ir.taqvim.core.ui.painter.PainterEnvironment
import ir.taqvim.core.ui.painter.PainterPalette
import ir.taqvim.core.ui.painter.SunArcBitmapPainter

/**
 * The T-702 bitmap painters for widgets that show a drawing (month bitmap, sun arc, Moon): colored from the widget's
 * Glance theme and sized for its configuration. Painters are not thread-safe, so a widget creates one set per session.
 * Bitmaps have a transparent background because [WidgetFrame] draws the configured background and transparency.
 */
class WidgetPainters(
    palette: PainterPalette,
    environment: PainterEnvironment,
) {
    val month: BitmapPainter<MonthGridModel> by lazy { MonthBitmapPainter(palette, environment) }
    val sunArc: BitmapPainter<SunArcModel> by lazy { SunArcBitmapPainter(palette, environment) }
    val moon: BitmapPainter<MoonBitmapModel> by lazy { MoonBitmapPainter(palette, environment) }

    companion object {
        private const val PERCENT = 100f
        private const val TRANSPARENT = 0

        /** Painters for a widget with [config], colored by [colors] (e.g. `GlanceTheme.colors`) in [context]. */
        fun of(
            context: Context,
            colors: ColorProviders,
            config: WidgetConfig,
        ): WidgetPainters = WidgetPainters(palette(context, colors), environment(context, config))

        /**
         * The painter palette of a Glance theme. Glance has no container-highest or outline-variant roles, so the
         * surface variant stands for the Moon's shadow and the outline for faint strokes.
         */
        fun palette(
            context: Context,
            colors: ColorProviders,
        ): PainterPalette {
            fun ColorProvider.argb(): Int = getColor(context).toArgb()
            return PainterPalette(
                background = TRANSPARENT,
                surfaceHighest = colors.surfaceVariant.argb(),
                onSurface = colors.onSurface.argb(),
                onSurfaceVariant = colors.onSurfaceVariant.argb(),
                outline = colors.outline.argb(),
                outlineVariant = colors.outline.argb(),
                primary = colors.primary.argb(),
                onPrimary = colors.onPrimary.argb(),
                error = colors.error.argb(),
                tertiary = colors.tertiary.argb(),
                land = colors.secondaryContainer.argb(),
            )
        }

        /** Density, font scale (the user's times the widget's scale) and writing direction of [context]. */
        fun environment(
            context: Context,
            config: WidgetConfig,
        ): PainterEnvironment {
            val resources = context.resources
            return PainterEnvironment(
                density = resources.displayMetrics.density,
                fontScale = resources.configuration.fontScale * config.scalePercent / PERCENT,
                rtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL,
            )
        }
    }
}
