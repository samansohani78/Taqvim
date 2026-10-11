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

/**
 * Part 44 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_44: List<EventDefinition> =
    listOf(
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
                        "ar" to "اليوم الدولي للأخوة الإنسانية",
                        "en" to "International Day of Human Fraternity",
                        "es" to "Día Internacional de la Fraternidad Humana",
                        "fr" to "Journée internationale de la fraternité humaine",
                        "ru" to "Международный день человеческого братства",
                        "zh" to "人类博爱国际日",
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
        EventDefinition(
            id = EventId("un.human-rights-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی حقوق بشر",
                        "ar" to "يوم حقوق الإنسان",
                        "en" to "Human Rights Day",
                        "es" to "Día de los Derechos Humanos",
                        "fr" to "Journée des droits de l'homme",
                        "ru" to "День прав человека",
                        "zh" to "人权日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 10),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/human-rights-day",
                        title = "Human Rights Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/human-rights-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.human-solidarity-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی همبستگی بشر",
                        "ar" to "اليوم الدولي للتضامن الإنساني",
                        "en" to "International Human Solidarity Day",
                        "es" to "Día Internacional de la Solidaridad Humana",
                        "fr" to "Journée internationale de la solidarité humaine",
                        "ru" to "Международный день солидарности людей",
                        "zh" to "国际人类团结日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/human-solidarity-day",
                        title = "International Human Solidarity Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/60/209",
                    "un" to "https://www.un.org/en/observances/human-solidarity-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.human-spaceflight-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پرواز فضایی انسان",
                        "ar" to "اليوم الدولي للرحلة البشرية إلى الفضاء",
                        "en" to "International Day of Human Space Flight",
                        "es" to "Día Internacional de los Vuelos Espaciales Tripulados",
                        "fr" to "Journée internationale du vol spatial habité",
                        "ru" to "Международный день полета человека в космос",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_011,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/human-spaceflight-day",
                            title = "International Day of Human Space Flight — A/RES/65/271 (7 April 2011)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/human-spaceflight-day",
                        title = "International Day of Human Space Flight (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/human-spaceflight-day",
                ),
        ),
    )
