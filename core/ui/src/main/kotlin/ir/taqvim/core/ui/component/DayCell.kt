/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * One day of a month grid, fully prepared by the caller (T-801 builds these off the main thread):
 * - [dayLabel] in the app's digits and [secondaryLabels] (days in other calendars);
 * - [indicators]: colors of the day's events (at most three dots are drawn) and an optional [shiftLabel];
 * - [contentDescription]: the spoken summary of the date and its events (T-1700);
 * - [isHoliday] covers official holidays and weekend days; [inCurrentMonth] is false for leading/trailing days.
 */
@Immutable
public data class DayCellModel(
    public val dayLabel: String,
    public val contentDescription: String,
    public val secondaryLabels: List<String> = emptyList(),
    public val indicators: List<Color> = emptyList(),
    public val shiftLabel: String? = null,
    public val isToday: Boolean = false,
    public val isSelected: Boolean = false,
    public val isHoliday: Boolean = false,
    public val inCurrentMonth: Boolean = true,
)

/** How the day number of a [DayCellModel] is colored, in order of precedence. */
internal enum class DayTone {
    OUTSIDE_MONTH,
    SELECTED,
    HOLIDAY,
    NORMAL,
    ;

    companion object {
        fun of(model: DayCellModel): DayTone =
            when {
                !model.inCurrentMonth -> OUTSIDE_MONTH
                model.isSelected -> SELECTED
                model.isHoliday -> HOLIDAY
                else -> NORMAL
            }
    }
}

/** Maximum number of event dots under a day number. */
internal const val MAX_INDICATORS = 3

private const val OUTSIDE_MONTH_ALPHA = 0.45f
private val CELL_SHAPE = RoundedCornerShape(12.dp)
private val TODAY_BORDER = 2.dp
private val DOT_SIZE = 5.dp

internal fun DayTone.color(colors: ColorScheme): Color =
    when (this) {
        DayTone.OUTSIDE_MONTH -> colors.onSurface.copy(alpha = OUTSIDE_MONTH_ALPHA)
        DayTone.SELECTED -> colors.onPrimaryContainer
        DayTone.HOLIDAY -> colors.error
        DayTone.NORMAL -> colors.onSurface
    }

/**
 * A month-grid day (T-701): selected days are filled, today is outlined, holidays use the holiday color. Accessibility
 * services read [DayCellModel.contentDescription] and the selection state; [longClickLabel] names the long-press action
 * (e.g. "new event").
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun DayCell(
    model: DayCellModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    longClickLabel: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val selectedFill = if (model.isSelected) Modifier.background(colors.primaryContainer) else Modifier
    val todayRing = if (model.isToday) Modifier.border(TODAY_BORDER, colors.primary, CELL_SHAPE) else Modifier
    Column(
        modifier
            .clip(CELL_SHAPE)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = longClickLabel)
            .clearAndSetSemantics {
                contentDescription = model.contentDescription
                selected = model.isSelected
            }.then(selectedFill)
            .then(todayRing)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            model.dayLabel,
            style = MaterialTheme.typography.titleMedium,
            color = DayTone.of(model).color(colors),
            maxLines = 1,
        )
        model.secondaryLabels.forEach { label ->
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
        }
        model.shiftLabel?.let { shift ->
            Text(
                shift,
                style = MaterialTheme.typography.labelSmall,
                color = colors.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (model.indicators.isNotEmpty()) IndicatorDots(model.indicators)
    }
}

@Composable
private fun IndicatorDots(colors: List<Color>) {
    Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        colors.take(MAX_INDICATORS).forEach { color ->
            Box(Modifier.size(DOT_SIZE).clip(CircleShape).background(color))
        }
    }
}
