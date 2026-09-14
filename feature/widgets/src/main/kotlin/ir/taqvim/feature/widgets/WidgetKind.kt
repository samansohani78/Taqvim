/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

/** What a widget's content depends on; an update trigger refreshes only the widgets that depend on it. */
enum class WidgetDependency {
    /** The civil date in the widget's time zone. */
    DATE,

    /** Official, personal, device or subscribed events. */
    EVENTS,

    /** Prayer times: the next time changes when one is reached. */
    PRAYER_TIMES,

    /** Content that changes every minute (clock, sun position, day/night map). */
    MINUTE,

    /** The chosen place (prayer times, sun, map). */
    LOCATION,

    /** Language, digits, theme and calendars; every widget depends on it. */
    APPEARANCE,
}

/** Optional parts a user can switch on or off per widget in its configuration screen. */
enum class WidgetContent {
    WEEKDAY,

    /** The date in the widget's secondary calendar. */
    SECONDARY_DATE,

    /** Holiday coloring. */
    HOLIDAYS,
    EVENTS,
    NEXT_PRAYER,
}

/** A widget's default span in launcher cells. */
data class WidgetCells(
    val columns: Int,
    val rows: Int,
) {
    init {
        require(columns in 1..MAX_CELLS && rows in 1..MAX_CELLS) { "invalid cell span $columns×$rows" }
    }

    private companion object {
        const val MAX_CELLS = 8
    }
}

/**
 * The widgets of PLAN T-1201…T-1212 as the framework knows them (T-1200): their stable [id], default [cells], what they
 * depend on, which optional parts they offer, and the responsive [sizes] they lay out for (smallest first). Each
 * widget task implements the content; spans that the plan does not state are Taqvim's defaults.
 */
enum class WidgetKind(
    val id: String,
    val cells: WidgetCells,
    own: Set<WidgetDependency>,
    val contents: Set<WidgetContent>,
    val sizes: List<WidgetSize>,
) {
    DATE_1X1("date_1x1", WidgetCells(1, 1), setOf(D.DATE), DAY_PARTS, listOf(WidgetSize.SMALL)),
    DATE_CLOCK_4X1(
        "date_clock_4x1",
        WidgetCells(4, 1),
        setOf(D.DATE, D.MINUTE),
        DAY_PARTS,
        listOf(WidgetSize.WIDE),
    ),
    DAY_SUMMARY_2X2(
        "day_summary_2x2",
        WidgetCells(2, 2),
        setOf(D.DATE, D.EVENTS, D.PRAYER_TIMES, D.LOCATION),
        DAY_PARTS + setOf(WidgetContent.EVENTS, WidgetContent.NEXT_PRAYER),
        listOf(WidgetSize.MEDIUM, WidgetSize.LARGE),
    ),
    PRAYER_STRIP_4X2(
        "prayer_strip_4x2",
        WidgetCells(4, 2),
        setOf(D.DATE, D.PRAYER_TIMES, D.LOCATION),
        DAY_PARTS + WidgetContent.NEXT_PRAYER,
        listOf(WidgetSize.WIDE, WidgetSize.LARGE),
    ),
    MONTH_INTERACTIVE("month_interactive", WidgetCells(4, 4), setOf(D.DATE, D.EVENTS), MONTH_PARTS, BIG),
    MONTH_BITMAP("month_bitmap", WidgetCells(4, 4), setOf(D.DATE, D.EVENTS), MONTH_PARTS, BIG),
    WEEK_STRIP("week_strip", WidgetCells(4, 1), setOf(D.DATE, D.EVENTS), MONTH_PARTS, listOf(WidgetSize.WIDE)),
    SCHEDULE("schedule", WidgetCells(4, 4), setOf(D.DATE, D.EVENTS), MONTH_PARTS, BIG),
    SUN_ARC(
        "sun_arc",
        WidgetCells(2, 2),
        setOf(D.DATE, D.MINUTE, D.LOCATION),
        setOf(WidgetContent.NEXT_PRAYER),
        listOf(WidgetSize.MEDIUM, WidgetSize.LARGE),
    ),
    MOON("moon", WidgetCells(2, 2), setOf(D.DATE), emptySet(), listOf(WidgetSize.SMALL, WidgetSize.MEDIUM)),
    MAP("map", WidgetCells(4, 2), setOf(D.MINUTE, D.LOCATION), emptySet(), listOf(WidgetSize.LARGE)),
    COUNTDOWN("countdown", WidgetCells(2, 2), setOf(D.DATE), emptySet(), listOf(WidgetSize.SMALL, WidgetSize.MEDIUM)),
    ;

    /** Everything this widget depends on, [WidgetDependency.APPEARANCE] included. */
    val dependencies: Set<WidgetDependency> = own + WidgetDependency.APPEARANCE

    companion object {
        /** The kind with [id], or `null` for an unknown or retired id. */
        fun byId(id: String): WidgetKind? = entries.firstOrNull { it.id == id }
    }
}

private typealias D = WidgetDependency

private val DAY_PARTS = setOf(WidgetContent.WEEKDAY, WidgetContent.SECONDARY_DATE, WidgetContent.HOLIDAYS)
private val MONTH_PARTS = setOf(WidgetContent.SECONDARY_DATE, WidgetContent.HOLIDAYS, WidgetContent.EVENTS)
private val BIG = listOf(WidgetSize.LARGE, WidgetSize.EXTRA_LARGE)
