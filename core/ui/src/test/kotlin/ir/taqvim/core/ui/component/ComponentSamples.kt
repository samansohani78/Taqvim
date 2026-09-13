/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Synthetic galleries of the T-701 components for the screenshot matrix; texts are Latin sample data. */
val COMPONENT_SAMPLES: Map<String, @Composable () -> Unit> =
    linkedMapOf(
        "top_bar" to { TopBarSample() },
        "screen_surface" to { ScreenSurfaceSample() },
        "segmented_tabs" to { Gallery { TabsSample() } },
        "number_wheel" to { Gallery { WheelSample() } },
        "date_picker" to { Gallery { DatePickerContent(samplePicker(), {}, {}) } },
        "event_chip" to { Gallery { ChipSample() } },
        "day_cell" to { Gallery { DayCellSample() } },
        "month_grid" to { Gallery { MonthGrid(sampleMonth(), {}, Modifier.fillMaxWidth().height(420.dp)) } },
        "moon_disc" to { Gallery { MoonSample() } },
        "sun_arc" to { Gallery { SunSample() } },
        "progress_ring" to { Gallery { RingSample() } },
        "empty_state" to { Gallery { EmptySample() } },
        "tooltip_card" to { Gallery { TooltipSample() } },
    )

@Composable
private fun Gallery(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
}

@Composable
private fun TopBarSample() {
    Column {
        TopBar(
            title = "Farvardin 1405",
            subtitle = "Shawwal 1447 · March 2026",
            navigation = TopBarAction(TestIcon, "Back", {}, mirrorInRtl = true),
            actions = listOf(TopBarAction(TestIcon, "Today", {}), TopBarAction(TestIcon, "Search", {})),
        )
        TopBar(title = "Settings")
    }
}

@Composable
private fun ScreenSurfaceSample() {
    ScreenSurface(
        topBar = { TopBar("Calendar", subtitle = "1405") },
        bottomBar = { Text("Bottom bar", Modifier.padding(16.dp)) },
    ) { padding ->
        EmptyState(
            "No events today",
            Modifier.padding(padding),
            message = "Long-press a day to add one",
            icon = TestIcon,
        )
    }
}

@Composable
private fun TabsSample() {
    SegmentedTabs(listOf("Calendars", "Events", "Times"), 0, {})
    SegmentedTabs(listOf("Day", "Week"), 1, {})
}

@Composable
private fun WheelSample() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        NumberWheel(14, 1..31, {}, "Day", Modifier.width(96.dp))
        NumberWheel(3, 1..12, {}, "Month", Modifier.width(160.dp), { "Month $it" })
    }
}

@Composable
private fun ChipSample() {
    EventChip(EventChipModel("Nowruz", Color(0xFF3F8F5A), "Nowruz", isHoliday = true), onClick = {})
    EventChip(EventChipModel("Team meeting 10:00", Color(0xFF3366CC), "Team meeting"))
    EventChip(
        EventChipModel("International Day of Education with a very long title", Color(0xFF8E44AD), "Education day"),
        Modifier.width(220.dp),
    )
}

@Composable
private fun DayCellSample() {
    val cells =
        listOf(
            DayCellModel("7", "Normal"),
            DayCellModel("8", "Today", secondaryLabels = listOf("19"), isToday = true),
            DayCellModel("9", "Selected", indicators = listOf(Color.Red, Color.Blue), isSelected = true),
            DayCellModel("10", "Holiday", shiftLabel = "N", isHoliday = true),
            DayCellModel("31", "Outside", inCurrentMonth = false),
        )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.forEach { DayCell(it, {}, Modifier.width(64.dp).heightIn(min = 64.dp)) }
    }
}

@Composable
private fun MoonSample() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { MoonDisc(it, waxing = true, "Moon", Modifier.size(52.dp)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(0.9f, 0.5f, 0.2f).forEach { MoonDisc(it, waxing = false, "Moon", Modifier.size(52.dp)) }
        MoonDisc(0.3f, waxing = true, "Moon", Modifier.size(52.dp), rotationDegrees = 180f)
    }
}

@Composable
private fun SunSample() {
    SunArc(SunArcModel(0.35f, "06:12", "18:31", "Day"))
    SunArc(SunArcModel(null, "06:12", "18:31", "Night"))
}

@Composable
private fun RingSample() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ProgressRing(0.25f, "Quarter", Modifier.size(96.dp)) { Text("25%") }
        ProgressRing(0.8f, "Most", Modifier.size(64.dp), strokeWidth = 4.dp)
    }
}

@Composable
private fun EmptySample() {
    EmptyState(
        "No reminders",
        message = "Reminders for official and personal events appear here",
        icon = TestIcon,
        action = ComponentAction("Add reminder") {},
    )
}

@Composable
private fun TooltipSample() {
    TooltipCard(
        title = "Source",
        body = "Official calendar of Iran 1405, published by the Calendar Center",
        footnote = "University of Tehran, Institute of Geophysics — page 3",
        action = ComponentAction("Open source") {},
        dismiss = ComponentAction("Close") {},
    )
}
