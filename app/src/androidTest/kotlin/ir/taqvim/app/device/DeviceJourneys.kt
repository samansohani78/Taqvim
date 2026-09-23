/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

/**
 * How much of a journey the suite actually verifies (R11, T-1700): [COVERED] and [PARTIAL] both claim a real test
 * exists (checked by [DeviceJourneyCoverageTest]); only [MISSING] makes no claim.
 */
internal enum class JourneyStatus {
    /** The journey's specified outcome is asserted end to end, not only that a screen opened. */
    COVERED,

    /** A test exercises part of the journey (usually navigation) but does not assert its specified outcome. */
    PARTIAL,

    /** No test in this module touches the journey. */
    MISSING,
}

/**
 * One of PLAN §8.3's "40 core scenarios" (docs/PLAN.md:735-736), with a stable id so a scenario can be tracked,
 * required and reported on independently of prose order (R11).
 *
 * PLAN's paragraph is 33 semicolon-separated groups, not 40 (confirmed by counting them; see
 * docs/device-journeys.md for the count). This registry reaches 40 by splitting exactly the groups whose slash- or
 * comma-joined items are genuinely different user actions (different screens or different code paths), one id per
 * item:
 * - "Install fresh (fa/en/ne/ckb/ar)" -> [J01]..[J05], one per language (R11 named this exact split as the gap);
 * - "onboarding skip/complete" -> [J06], [J07];
 * - "edit/delete event" -> [J13], [J14];
 * - "ICS import/export" -> [J31], [J32].
 *
 * That is 4 groups turning into 5+2+2+2 = 11 ids, i.e. +7 ids over the 4 groups they replace: 33 + 7 = 40. Every
 * other group with a quantity in parentheses or "each X" ("NLP input 10 phrases", "theme changes (6)", "deep links
 * (10)", "converter (each calendar)", "add each widget") is read as a required *breadth* within one journey (the
 * existing suite already tests such breadth as one test iterating a list, e.g. [DeviceSmokeTest]'s `LINKS`), not as
 * one id per item — splitting those literally would overshoot 40 by dozens.
 *
 * [id] J40 (wear smoke) is implemented by `:wear`'s own instrumented suite
 * (`wear/src/androidTest/kotlin/ir/taqvim/wear/device/WearDeviceSmokeTest.kt`), not by this module, so
 * [DeviceJourneyCoverageTest] does not try to load it by reflection here.
 */
internal data class Journey(
    val id: String,
    val summary: String,
    val status: JourneyStatus,
    /**
     * Where [status] is substantiated: `"ClassName#methodName"` of a `@Test` in this package, checked by reflection
     * in [DeviceJourneyCoverageTest] so a renamed or deleted test cannot leave a stale claim; several tests are
     * separated by `;`. A `module:` prefix (only J40) names coverage outside this module, which is recorded but not
     * reflectively checked here. `null` only when [status] is [JourneyStatus.MISSING].
     */
    val coveringTest: String? = null,
)

/** Every PLAN §8.3 journey (docs/device-journeys.md has the full table and current state). */
internal object JourneyRegistry {
    private const val SMOKE_LINKS = "DeviceSmokeTest#everyLinkedScreenOpensAndSurvivesRotation"
    private const val SMOKE_TABS = "DeviceSmokeTest#everyTabAndMoreEntryOpens"
    private const val A11Y = "DeviceAccessibilityTest#screensHaveNoAccessibilityErrors"

    val ALL: List<Journey> =
        listOf(
            Journey(
                "J01",
                "Install fresh (fa) opens to first-run defaults",
                JourneyStatus.PARTIAL,
                "$SMOKE_LINKS;$A11Y",
            ),
            Journey(
                "J02",
                "Install fresh (en) opens to first-run defaults",
                JourneyStatus.PARTIAL,
                "$SMOKE_LINKS;$A11Y",
            ),
            Journey("J03", "Install fresh (ne) opens to first-run defaults", JourneyStatus.MISSING),
            Journey("J04", "Install fresh (ckb) opens to first-run defaults", JourneyStatus.MISSING),
            Journey("J05", "Install fresh (ar) opens to first-run defaults", JourneyStatus.MISSING),
            Journey("J06", "Onboarding: skip", JourneyStatus.MISSING),
            Journey("J07", "Onboarding: complete", JourneyStatus.PARTIAL, "DeviceSmokeTest#onboardingPagesOpen"),
            Journey("J08", "Month swipe changes the shown month and its title", JourneyStatus.PARTIAL, SMOKE_LINKS),
            Journey(
                "J09",
                "Select a day opens its tabs, with the day's real content (dataset title, holiday marking)",
                JourneyStatus.COVERED,
                "$SMOKE_LINKS;DeviceHolidayTest#nowruzIsMarkedAndItsTitleMatchesTheDataset",
            ),
            Journey("J10", "Today button returns to the current day", JourneyStatus.MISSING),
            Journey("J11", "Long-press a day opens the event editor on it", JourneyStatus.MISSING),
            Journey(
                "J12",
                "Create a personal event with a reminder: it appears on its day and the agenda, its reminder is " +
                    "scheduled, and the reminder notification fires",
                JourneyStatus.COVERED,
                "DeviceEventLifecycleTest#createdEventShowsOnItsDayAndAgendaAndSchedulesAReminder;" +
                    "DeviceReminderTest#reminderNotificationFires",
            ),
            Journey("J13", "Edit an existing event", JourneyStatus.MISSING),
            Journey("J14", "Delete an event", JourneyStatus.MISSING),
            Journey("J15", "An official-event reminder", JourneyStatus.MISSING),
            Journey("J16", "Search jumps to the matching day", JourneyStatus.PARTIAL, SMOKE_LINKS),
            Journey("J17", "Year view: select a day", JourneyStatus.PARTIAL, SMOKE_TABS),
            Journey("J18", "Timeline: drag-create an event", JourneyStatus.MISSING),
            Journey(
                "J19",
                "Agenda scrolls through upcoming events",
                JourneyStatus.PARTIAL,
                "$SMOKE_TABS;DeviceEventLifecycleTest#createdEventShowsOnItsDayAndAgendaAndSchedulesAReminder",
            ),
            Journey("J20", "Converter: each calendar", JourneyStatus.PARTIAL, SMOKE_LINKS),
            Journey("J21", "NLP input: 10 phrases", JourneyStatus.MISSING),
            Journey("J22", "Distance and workdays tool", JourneyStatus.MISSING),
            Journey("J23", "Enable athan: alarm scheduled, fires, stop", JourneyStatus.MISSING),
            Journey("J24", "Change location by city, GPS or coordinates", JourneyStatus.MISSING),
            Journey(
                "J25",
                "Add each widget, tap it, its deep link opens",
                JourneyStatus.PARTIAL,
                "DeviceSurfacesTest#everyWidgetIsBoundUpdatedAndDrawn",
            ),
            Journey(
                "J26",
                "Notification content is correct",
                JourneyStatus.PARTIAL,
                "DeviceReminderTest#reminderNotificationFires",
            ),
            Journey("J27", "Theme changes (6)", JourneyStatus.MISSING),
            Journey("J28", "Font scale 2.0", JourneyStatus.MISSING),
            Journey("J29", "RTL switch", JourneyStatus.PARTIAL, "$SMOKE_LINKS;$A11Y"),
            Journey(
                "J30",
                "Backup, wipe, restore: the event is back",
                JourneyStatus.COVERED,
                "DeviceBackupRestoreTest#backupWipeRestoreBringsTheEventBack",
            ),
            Journey("J31", "ICS import", JourneyStatus.MISSING),
            Journey("J32", "ICS export", JourneyStatus.MISSING),
            Journey("J33", "Subscription refresh", JourneyStatus.MISSING),
            Journey("J34", "Compass orientation lock", JourneyStatus.PARTIAL, SMOKE_TABS),
            Journey("J35", "Map layers", JourneyStatus.PARTIAL, SMOKE_TABS),
            Journey("J36", "Astronomy slider", JourneyStatus.PARTIAL, SMOKE_LINKS),
            Journey(
                "J37",
                "Settings are table-driven: a changed row (the primary calendar) survives a cold read and the " +
                    "calendar screen shows it",
                JourneyStatus.COVERED,
                "DeviceCalendarPersistenceTest#changedPrimaryCalendarSurvivesAColdReadAndShowsInTheMonthGrid",
            ),
            Journey("J38", "Deep links (10) open their destination", JourneyStatus.PARTIAL, SMOKE_LINKS),
            Journey("J39", "PROCESS_TEXT detection", JourneyStatus.MISSING),
            Journey(
                "J40",
                "Wear smoke",
                JourneyStatus.COVERED,
                "module:wear/src/androidTest/kotlin/ir/taqvim/wear/device/WearDeviceSmokeTest.kt",
            ),
        )

    /** Ids a phone instrumented test in this module could implement (all but J40, which belongs to `:wear`). */
    val REQUIRED_IN_THIS_MODULE: Set<String> = ALL.map { it.id }.toSet() - "J40"
}
