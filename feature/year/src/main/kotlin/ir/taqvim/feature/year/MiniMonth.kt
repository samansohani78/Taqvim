/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

private val MINI_MONTH_PADDING = 4.dp

/** Share of the smaller cell side used by a day number's font size. */
private const val TEXT_FRACTION = 0.55f

/** Share of the smaller cell side used by the radius of today's disc. */
private const val TODAY_RADIUS_FRACTION = 0.48f

/**
 * Cached text layouts shared by every mini month on every year page.
 *
 * One page lays out 12 months x (31 day numbers + 7 weekday names); the day numbers of one month are the same
 * strings as the next month's and the next year's, so the whole year view needs only 31 numbers in the three day
 * colors (normal, day off, today) plus 7 weekday names = 100 entries. 128 leaves room for a font size still being
 * evicted after a pinch zoom, which changes the size and so the key.
 *
 * It must be remembered **above** the pager: a cache created per mini month died with its page, so every page
 * change laid out all 989 texts again, which is what made a year swipe take 54-63 ms (T-805, trace evidence in
 * docs/STATUS_REPORT.md).
 */
internal const val MINI_MONTH_TEXT_CACHE: Int = 128

/**
 * One month of the year grid: its name over a drawn six-week grid (T-805). The whole month is one button announced
 * with the month's summary; tapping it opens the month.
 */
@Composable
internal fun MiniMonthView(
    month: MiniMonth,
    weekdayLabels: List<String>,
    onOpen: () -> Unit,
    /** Shared with every other mini month so a day number is laid out once per year view, not once per page. */
    measurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    val openLabel = stringResource(R.string.year_open_month)
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = month.description
                role = Role.Button
                onClick(openLabel) {
                    onOpen()
                    true
                }
            }.padding(MINI_MONTH_PADDING),
    ) {
        Text(
            text = month.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            autoSize =
                TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.titleSmall.fontSize),
        )
        MiniMonthGrid(
            month,
            weekdayLabels,
            measurer,
            Modifier
                .fillMaxWidth()
                .aspectRatio(YearPageBuilder.DAYS_PER_WEEK / (YearPageBuilder.WEEKS + 1f)),
        )
    }
}

/** Colors of a mini month grid. */
private class MiniMonthColors(
    val weekday: Color,
    val normal: Color,
    val offDay: Color,
    val todayDisc: Color,
    val todayText: Color,
)

/** The weekday row and the six weeks of [month], drawn with cached text layouts. */
@Composable
private fun MiniMonthGrid(
    month: MiniMonth,
    weekdayLabels: List<String>,
    measurer: TextMeasurer,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val colors =
        MiniMonthColors(scheme.onSurfaceVariant, scheme.onSurface, scheme.error, scheme.primary, scheme.onPrimary)
    val style = MaterialTheme.typography.labelSmall
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier) {
        val cell = MiniCell(size.width / YearPageBuilder.DAYS_PER_WEEK, size.height / (YearPageBuilder.WEEKS + 1), rtl)
        val textStyle = style.copy(fontSize = (min(cell.width, cell.height) * TEXT_FRACTION).toSp())
        weekdayLabels.forEachIndexed { column, label ->
            drawCentered(measurer, label, textStyle.copy(color = colors.weekday), cell.center(column, 0))
        }
        month.cells.forEachIndexed { index, day ->
            if (day != null) {
                val center =
                    cell.center(
                        index % YearPageBuilder.DAYS_PER_WEEK,
                        index / YearPageBuilder.DAYS_PER_WEEK + 1,
                    )
                drawDay(measurer, day, textStyle, colors, center, min(cell.width, cell.height))
            }
        }
    }
}

/** Cell geometry of a mini month grid; columns run right to left when [rtl]. */
private class MiniCell(
    val width: Float,
    val height: Float,
    private val rtl: Boolean,
) {
    fun center(
        column: Int,
        row: Int,
    ): Offset {
        val visualColumn = if (rtl) YearPageBuilder.DAYS_PER_WEEK - 1 - column else column
        return Offset((visualColumn + HALF) * width, (row + HALF) * height)
    }

    private companion object {
        const val HALF = 0.5f
    }
}

private fun DrawScope.drawDay(
    measurer: TextMeasurer,
    day: MiniDay,
    style: TextStyle,
    colors: MiniMonthColors,
    center: Offset,
    side: Float,
) {
    val color =
        when {
            day.isToday -> colors.todayText
            day.tone == MiniDayTone.OFF_DAY -> colors.offDay
            else -> colors.normal
        }
    if (day.isToday) drawCircle(colors.todayDisc, side * TODAY_RADIUS_FRACTION, center)
    drawCentered(measurer, day.label, style.copy(color = color), center)
}

private fun DrawScope.drawCentered(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    center: Offset,
) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}
