/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings

/** A simple triangle icon for tests (no icon library in `:core:ui`). */
val TestIcon: ImageVector =
    ImageVector
        .Builder("test", 24.dp, 24.dp, 24f, 24f)
        .addPath(
            PathData {
                moveTo(12f, 3f)
                lineTo(21f, 20f)
                lineTo(3f, 20f)
                close()
            },
            fill = SolidColor(Color.Black),
        ).build()

/** The app theme with fixed colors, as the component tests and screenshots use it. */
@Composable
fun TestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}

/** Day cells of a 35-day grid; cell `i` is described as "Day i". */
fun sampleCells(): List<DayCellModel> =
    List(35) { index ->
        val day = index - 2
        DayCellModel(
            dayLabel = if (day < 1 || day > 31) "·" else "$day",
            contentDescription = "Day $index",
            secondaryLabels = if (index % 7 == 0) listOf("${index + 10}") else emptyList(),
            indicators = List(index % 5) { if (it % 2 == 0) Color(0xFF3F8F5A) else Color(0xFFB3261E) },
            shiftLabel = if (index % 9 == 4) "N" else null,
            isToday = index == 15,
            isSelected = index == 17,
            isHoliday = index % 7 == 6,
            inCurrentMonth = day in 1..31,
        )
    }

/** A 5-week month grid with week numbers. */
fun sampleMonth(): MonthGridModel =
    MonthGridModel(
        weekdayLabels = listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr"),
        cells = sampleCells(),
        weekNumbers = List(5) { WeekNumberModel("${it + 1}", "Week ${it + 1}") },
        longClickLabel = "New event",
    )

/** Esfand has 30 days in 1403 and 29 otherwise; months 1–6 have 31 days and 7–11 have 30. */
val sampleDaysInMonth: (Int, Int) -> Int = { year, month ->
    when {
        month <= 6 -> 31
        month < 12 -> 30
        year == 1403 -> 30
        else -> 29
    }
}

/** A Persian-calendar-shaped picker model with Latin sample texts. */
fun samplePicker(initial: DateSelection = DateSelection(1403, 12, 30)): DatePickerModel =
    DatePickerModel(
        initial = initial,
        years = 1390..1420,
        monthNames = List(12) { "Month ${it + 1}" },
        daysInMonth = sampleDaysInMonth,
        formatNumber = Int::toString,
        labels = DatePickerLabels("Go to date", "Year", "Month", "Day", "OK", "Cancel"),
    )
