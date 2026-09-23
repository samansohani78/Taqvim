/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit

/**
 * The text measurer a [MonthGrid] shares with its cells, or `null` outside a grid. It is the measurer the grid's fit
 * probe has just measured every label of the page with, at the very style a cell draws it in, so a cell's lines are
 * mostly answered from its cache (BUG-2).
 */
internal val LocalCellTextMeasurer: ProvidableCompositionLocal<TextMeasurer?> = staticCompositionLocalOf { null }

/** One text line of a cell: what to draw and in which style and color. */
@Immutable
internal data class CellText(
    val text: String,
    val style: TextStyle,
    val color: Color,
)

/** What a cell inside a grid draws: its text lines from the top and the colors of its event dots. */
@Immutable
internal data class CellDrawing(
    val lines: List<CellText>,
    val dots: List<Color>,
)

/**
 * The lines and event dots of a [DayCell] inside a month grid, drawn by one node (BUG-2).
 *
 * A cell used to be a column of up to three `Text` nodes and a row of dot boxes — about eight layout nodes, each
 * `Text` laying out its own string although a page holds only a few dozen distinct numbers. A page entering the pager
 * therefore laid out some 250 strings in one frame. Here the lines are laid out through [measurer], which caches them
 * across cells and pages, and drawn stacked in the middle of the cell exactly as the column placed them: each line at
 * its own height, the group centred, the dots [DOT_TOP_PADDING] below the last line. The grid has proved every label
 * fits its slot at these sizes ([DayCellFit]), so nothing is clipped (T-1701).
 */
@Composable
internal fun CellLines(
    drawing: CellDrawing,
    measurer: TextMeasurer,
    density: Density,
    modifier: Modifier = Modifier,
) {
    val direction = LocalLayoutDirection.current
    Spacer(
        modifier.fillMaxSize().drawWithCache {
            val lines = drawing.lines
            val layouts = lines.map { measurer.layout(it, density, direction) }
            val dotSize = with(density) { DOT_SIZE.toPx() }
            val dotGap = with(density) { DOT_GAP.toPx() }
            val dotTop = with(density) { DOT_TOP_PADDING.toPx() }
            val shown = drawing.dots.take(MAX_INDICATORS)
            val dotsHeight = if (shown.isEmpty()) 0f else dotTop + dotSize
            val textHeight = layouts.sumOf { it.size.height }
            var y = (size.height - textHeight - dotsHeight) / 2f
            val tops = layouts.map { layout -> y.also { y += layout.size.height } }
            onDrawBehind {
                layouts.forEachIndexed { i, layout ->
                    drawText(
                        layout,
                        color = lines[i].color,
                        topLeft = Offset((size.width - layout.size.width) / 2f, tops[i]),
                    )
                }
                if (shown.isNotEmpty()) {
                    val rowWidth = shown.size * dotSize + (shown.size - 1) * dotGap
                    var x = (size.width - rowWidth) / 2f
                    val centreY = y + dotTop + dotSize / 2f
                    shown.forEach { color ->
                        drawCircle(color, radius = dotSize / 2f, center = Offset(x + dotSize / 2f, centreY))
                        x += dotSize + dotGap
                    }
                }
            }
        },
    )
}

/** [line] laid out on one line at its own size, the way the grid's probe measured it, so the cache answers. */
private fun TextMeasurer.layout(
    line: CellText,
    density: Density,
    direction: LayoutDirection,
): TextLayoutResult =
    measure(
        text = line.text,
        style = line.style,
        maxLines = 1,
        layoutDirection = direction,
        density = density,
    )

/** [style] at [scale] times its size with the font's own line height, as a cell line and the probe both use it. */
internal fun TextStyle.cellLine(scale: Float): TextStyle =
    copy(lineHeight = TextUnit.Unspecified, fontSize = fontSize * scale)

/** The styles of a cell's day number and small lines at the grid's [fit]. */
@Composable
internal fun cellStyles(fit: DayCellTextFit): Pair<TextStyle, TextStyle> =
    MaterialTheme.typography.titleMedium.cellLine(fit.dayScale) to
        MaterialTheme.typography.labelSmall.cellLine(fit.labelScale)
