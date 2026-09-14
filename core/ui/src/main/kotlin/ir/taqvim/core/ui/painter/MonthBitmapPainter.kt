/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import ir.taqvim.core.ui.component.DayCellModel
import ir.taqvim.core.ui.component.DayTone
import ir.taqvim.core.ui.component.MonthGridModel

private const val HEADER_SP = 12f
private const val HEADER_LINE = 2f
private const val WEEK_SP = 10f
private const val WEEK_COLUMN_DP = 24f
private const val DAY_SP = 14f
private const val DAY_HEIGHT_FRACTION = 0.34f
private const val DAY_CENTER_FRACTION = 0.38f
private const val SECONDARY_CENTER_FRACTION = 0.7f
private const val SECONDARY_SCALE = 0.7f
private const val DOT_CENTER_FRACTION = 0.9f
private const val DOT_RADIUS_DP = 2f
private const val MARK_FRACTION = 0.3f
private const val RING_DP = 1.5f
private const val OUTSIDE_ALPHA = 0x61

/**
 * Draws a [MonthGridModel] into a widget bitmap (T-702): weekday headers, optional week numbers on the start edge and
 * each day with its first secondary date, a ring for today, a disc for the selection, the holiday tone and up to three
 * event dots. Mirrors in RTL. The same cell models as the T-701 `MonthGrid` are used, so a widget and the screen agree.
 */
public class MonthBitmapPainter(
    private val palette: PainterPalette,
    private val environment: PainterEnvironment,
) : BitmapPainter<MonthGridModel> {
    private val text = textPaint(environment)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = environment.px(RING_DP)
        }

    override fun draw(
        canvas: Canvas,
        model: MonthGridModel,
        width: Int,
        height: Int,
    ) {
        if (palette.background != 0) canvas.drawColor(palette.background)
        val layout =
            MonthBitmapLayout(
                width = width,
                height = height,
                model = model,
                weekColumnWidth = environment.px(WEEK_COLUMN_DP).toInt(),
                headerHeight = (environment.textPx(HEADER_SP) * HEADER_LINE).toInt(),
                rtl = environment.rtl,
            )
        model.weekdayLabels.forEachIndexed { column, label ->
            val rect = layout.headerRect(column)
            text.textStyle(environment.textPx(HEADER_SP), palette.onSurfaceVariant)
            canvas.drawFittedText(label, rect.centerX, rect.centerY, rect.width, text)
        }
        model.weekNumbers?.forEachIndexed { row, week ->
            val rect = layout.weekRect(row)
            text.textStyle(environment.textPx(WEEK_SP), palette.outline)
            canvas.drawFittedText(week.label, rect.centerX, rect.centerY, rect.width, text)
        }
        val columns = model.weekdayLabels.size
        model.cells.forEachIndexed { index, cell ->
            drawCell(canvas, cell, layout.cellRect(index % columns, index / columns))
        }
    }

    private fun drawCell(
        canvas: Canvas,
        cell: DayCellModel,
        rect: PixelRect,
    ) {
        val tone = DayTone.of(cell)
        val dayY = rect.top + rect.height * DAY_CENTER_FRACTION
        val markRadius = minOf(rect.width, rect.height) * MARK_FRACTION
        if (tone == DayTone.SELECTED) {
            fill.color = palette.primary
            canvas.drawCircle(rect.centerX, dayY, markRadius, fill)
        }
        if (cell.isToday && cell.inCurrentMonth) {
            ring.color = palette.primary
            canvas.drawCircle(rect.centerX, dayY, markRadius, ring)
        }
        val daySize = minOf(environment.textPx(DAY_SP), rect.height * DAY_HEIGHT_FRACTION)
        text.textStyle(daySize, dayColor(tone))
        canvas.drawFittedText(cell.dayLabel, rect.centerX, dayY, rect.width, text)
        cell.secondaryLabels.firstOrNull()?.let { label ->
            text.textStyle(daySize * SECONDARY_SCALE, palette.onSurfaceVariant)
            canvas.drawFittedText(
                label,
                rect.centerX,
                rect.top + rect.height * SECONDARY_CENTER_FRACTION,
                rect.width,
                text,
            )
        }
        drawDots(canvas, cell, rect)
    }

    private fun drawDots(
        canvas: Canvas,
        cell: DayCellModel,
        rect: PixelRect,
    ) {
        val radius = environment.px(DOT_RADIUS_DP)
        val centers = IndicatorGeometry.dotCenters(cell.indicators.size, rect.centerX, radius, radius)
        val ordered = if (environment.rtl) centers.asReversed() else centers
        val y = rect.top + rect.height * DOT_CENTER_FRACTION
        ordered.forEachIndexed { index, x ->
            fill.color = cell.indicators[index].toArgb()
            canvas.drawCircle(x, y, radius, fill)
        }
    }

    private fun dayColor(tone: DayTone): Int =
        when (tone) {
            DayTone.OUTSIDE_MONTH -> PainterColors.withAlpha(palette.onSurfaceVariant, OUTSIDE_ALPHA)
            DayTone.SELECTED -> palette.onPrimary
            DayTone.HOLIDAY -> palette.error
            DayTone.NORMAL -> palette.onSurface
        }
}
