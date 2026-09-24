/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("NoHardcodedNonLatinText", "MaxLineLength")

package ir.taqvim.data.events.generated

import ir.taqvim.core.events.Citation
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.LocalizedText
import ir.taqvim.core.events.Validity
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday

/**
 * Part 19 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_19: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.genocide-prevention-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی یادبود و کرامت قربانیان جنایت نسل‌کشی و پیشگیری از این جنایت",
                        "en" to "International Day of Commemoration and Dignity of the Victims of the Crime of Genocide and of the Prevention of this Crime",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 9),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_015,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/genocide-prevention-day",
                            title = "International Day of Commemoration and Dignity of the Victims of the Crime of Genocide — A/RES/69/323 (29 September 2015)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/genocide-prevention-day",
                        title = "International Day of Commemoration and Dignity of the Victims of the Crime of Genocide and of the Prevention of this Crime (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/genocide-prevention-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.girl-child-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی دختران",
                        "en" to "International Day of the Girl Child",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 11),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_011,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/girl-child-day",
                            title = "International Day of the Girl Child — resolution 66/170 (19 December 2011)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/girl-child-day",
                        title = "International Day of the Girl Child (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/girl-child-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.girls-in-ict-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی دختران در فناوری اطلاعات و ارتباطات",
                        "en" to "International Girls in ICT Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 23),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_010,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.itu.int/women-and-girls/girls-in-ict/about-international-girls-in-ict-day/",
                            title = "International Girls in ICT Day (ITU Plenipotentiary Conference Resolution 70, 2010)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.itu.int/women-and-girls/girls-in-ict/",
                        title = "International Girls in ICT Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.itu.int/women-and-girls/girls-in-ict/",
                ),
        ),
        EventDefinition(
            id = EventId("un.habitat-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی اسکان بشری",
                        "en" to "World Habitat Day",
                    ),
                ),
            rule = EventRule.NthWeekdayOfMonth(month = 10, weekday = Weekday.MONDAY, n = 1),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/habitat-day",
                        title = "World Habitat Day (observance page)",
                        page = "Background: \"the first Monday of October of every year\"",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/40/202",
                    "un" to "https://www.un.org/en/observances/habitat-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.happiness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی شادی",
                        "en" to "International Day of Happiness",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 20),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_012,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/happiness-day",
                            title = "International Day of Happiness — resolution 66/281 (12 July 2012)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/happiness-day",
                        title = "International Day of Happiness (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/happiness-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.hope-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی امید",
                        "en" to "International Day of Hope",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/hope-day",
                            title = "International Day of Hope — A/RES/79/270 (4 March 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/hope-day",
                        title = "International Day of Hope (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/hope-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.horse-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی اسب",
                        "en" to "World Horse Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 11),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/horse-day",
                            title = "World Horse Day — A/RES/79/291 (3 June 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/horse-day",
                        title = "World Horse Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/horse-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.human-fraternity"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی برادری انسانی",
                        "en" to "International Day of Human Fraternity",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 4),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_020,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/human-fraternity",
                            title = "International Day of Human Fraternity — resolution 75/200",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/human-fraternity",
                        title = "International Day of Human Fraternity (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/human-fraternity",
                ),
        ),
    )
