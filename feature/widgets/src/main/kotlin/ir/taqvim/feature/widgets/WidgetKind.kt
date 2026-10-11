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

    /**
     * Whether the shown day is a day off.
     *
     * Its own axis, not a weaker [EVENTS]. A date widget colours a holiday without listing anything, so it must be
     * redrawn when the user switches an official source, a kind of day, holidays-only, the weekend or the Islamic
     * calculation — but a holiday comes from the dataset alone, so a personal event being added or a calendar
     * feed syncing cannot change it, and waking every date widget for those would be work with nothing to show for
     * it. Before this existed, date widgets declared only [DATE] and kept colouring a day red after its source was
     * switched off (2026-10-10).
     */
    HOLIDAYS,

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

    /**
     * T-1216: today in each of the user's calendars and nothing else.
     *
     * It offers no optional parts on purpose — no holiday colour, no events — so it depends on the date alone and
     * draws from the calendar engines without reading a database. That is what lets it appear filled the moment the
     * host asks for it, and what makes it safe on a lock screen: it can show nothing personal because it has none.
     */
    THREE_DATES(
        "three_dates",
        WidgetCells(3, 3),
        setOf(D.DATE),
        emptySet(),
        listOf(WidgetSize.MEDIUM, WidgetSize.EXTRA_LARGE),
    ),

    // The clock is a launcher-ticked TextClock (T-1202), so the widget does not need a wake-up every minute.
    DATE_CLOCK_4X1(
        "date_clock_4x1",
        WidgetCells(4, 1),
        setOf(D.DATE),
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

    // The place only decides which side of the Moon is lit (T-1210).
    MOON(
        "moon",
        WidgetCells(2, 2),
        setOf(D.DATE, D.LOCATION),
        emptySet(),
        listOf(WidgetSize.SMALL, WidgetSize.MEDIUM),
    ),
    MAP("map", WidgetCells(4, 2), setOf(D.MINUTE, D.LOCATION), emptySet(), listOf(WidgetSize.LARGE)),
    COUNTDOWN("countdown", WidgetCells(2, 2), setOf(D.DATE), emptySet(), listOf(WidgetSize.SMALL, WidgetSize.MEDIUM)),
    ;

    /**
     * Everything this widget depends on: what it declares, [WidgetDependency.APPEARANCE], and
     * [WidgetDependency.HOLIDAYS] whenever it offers holiday colouring.
     *
     * The holiday part is derived rather than declared so the two cannot drift apart: a kind that offers
     * [WidgetContent.HOLIDAYS] depends on what decides a holiday, by construction.
     */
    val dependencies: Set<WidgetDependency> =
        own + WidgetDependency.APPEARANCE +
            if (WidgetContent.HOLIDAYS in contents) setOf(WidgetDependency.HOLIDAYS) else emptySet()

    companion object {
        /** The kind with [id], or `null` for an unknown or retired id. */
        fun byId(id: String): WidgetKind? = entries.firstOrNull { it.id == id }
    }
}

private typealias D = WidgetDependency

private val DAY_PARTS = setOf(WidgetContent.WEEKDAY, WidgetContent.SECONDARY_DATE, WidgetContent.HOLIDAYS)
private val MONTH_PARTS = setOf(WidgetContent.SECONDARY_DATE, WidgetContent.HOLIDAYS, WidgetContent.EVENTS)
private val BIG = listOf(WidgetSize.LARGE, WidgetSize.EXTRA_LARGE)
