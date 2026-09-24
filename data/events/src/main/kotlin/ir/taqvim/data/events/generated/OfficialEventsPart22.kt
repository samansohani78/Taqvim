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
 * Part 22 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_22: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.international-day-of-forests"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی جنگل‌ها",
                        "en" to "International Day of Forests",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_012,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.fao.org/international-day-of-forests/en",
                            title = "International Day of Forests -- GA resolution A/RES/67/200 (2012)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.fao.org/international-day-of-forests/en",
                        title = "International Day of Forests (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.fao.org/international-day-of-forests/en",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-light"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی نور",
                        "en" to "International Day of Light",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 16),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.unesco.org/en/days/light",
                            title = "International Day of Light (UNESCO General Conference, 39th session, Resolution 39 C/Resolution 16, Nov 2017)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/light",
                        title = "International Day of Light (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.unesco.org/en/days/light",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-plant-health"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی سلامت گیاهان",
                        "en" to "International Day of Plant Health",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/international-day-plant-health",
                            title = "International Day of Plant Health (UN General Assembly A/RES/76/256, adopted Mar 2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.fao.org/plant-health-day/en",
                        title = "International Day of Plant Health (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.fao.org/plant-health-day/en",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-play"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی بازی",
                        "en" to "International Day of Play",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 11),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/international-day-of-play",
                            title = "International Day of Play — A/RES/78/268 (2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-of-play",
                        title = "International Day of Play (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/international-day-of-play",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-potato"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی سیب‌زمینی",
                        "en" to "International Day of Potato",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_023,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://digitallibrary.un.org/record/4030832",
                            title = "International Day of Potato (A/RES/78/123, adopted early December 2023)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.fao.org/international-potato-day/en",
                        title = "International Day of Potato (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.fao.org/international-potato-day/en",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-reflection-on-the-1994-genocide"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز یادبود قربانیان نسل کشی رواندا",
                        "ar" to "اليوم الدولي للتفكر في الإبادة الجماعية التي وقعت في عام 1994 ضد التوتسي في رواندا",
                        "en" to "International Day of Reflection on the 1994 Genocide against the Tutsi in Rwanda",
                        "es" to "Día Mundial de la Salud",
                        "fr" to "Journée internationale de réflexion sur le génocide des Tutsis au Rwanda en 1994",
                        "ru" to "Международный день памяти о геноциде тутси в Руанде в 1994 году",
                        "zh" to "1994年卢旺达境内针对图西人实施的灭绝种族罪国际反思日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "http://www.un.org/en/preventgenocide/rwanda/commemoration/annualcommemoration.shtml",
                        title = "International Day of Reflection on the 1994 Genocide against the Tutsi in Rwanda (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/58/234",
                    "un" to "http://www.un.org/en/preventgenocide/rwanda/commemoration/annualcommemoration.shtml",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-solidarity-with-the-palestinian-people"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی همبستگی با مردم فلسطین",
                        "ar" to "اليوم الدولي للتضامن مع الشعب الفلسطيني",
                        "en" to "International Day of Solidarity with the Palestinian People",
                        "es" to "Día Internacional de Solidaridad con el Pueblo Palestino",
                        "fr" to "Journée internationale de solidarité avec le peuple palestinien",
                        "ru" to "Международный день солидарности с палестинским народом",
                        "zh" to "声援巴勒斯坦人民国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-of-solidarity-with-the-palestinian-people",
                        title = "International Day of Solidarity with the Palestinian People (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/32/40",
                    "un" to "https://www.un.org/en/observances/international-day-of-solidarity-with-the-palestinian-people",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-peace"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی صلح",
                        "ar" to "اليوم الدولي للسلام",
                        "en" to "International Day of Peace",
                        "es" to "Día Internacional de la Paz",
                        "fr" to "Journée internationale de la paix",
                        "ru" to "Международный день мира",
                        "zh" to "国际和平日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-peace",
                        title = "International Day of Peace (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/36/67",
                    "un" to "https://www.un.org/en/observances/international-day-peace",
                ),
        ),
    )
