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
 * Part 27 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_27: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.nuclear-weapons-elimination-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی ریشه‌کنی کامل سلاح‌های هسته‌ای",
                        "en" to "International Day for the Total Elimination of Nuclear Weapons",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 26),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_013,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/nuclear-weapons-elimination-day",
                            title = "International Day for the Total Elimination of Nuclear Weapons — resolution 68/32 (December 2013)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/nuclear-weapons-elimination-day",
                        title = "International Day for the Total Elimination of Nuclear Weapons (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/nuclear-weapons-elimination-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.oceans-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی اقیانوس ها",
                        "ar" to "اليوم العالمي للمحيطات",
                        "en" to "World Oceans Day",
                        "es" to "Día Mundial de los Océanos",
                        "fr" to "Journée mondiale de l'océan",
                        "ru" to "Всемирный день океанов",
                        "zh" to "世界海洋日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 8),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/oceans-day",
                        title = "World Oceans Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/63/111",
                    "un" to "https://www.un.org/en/observances/oceans-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.older-persons-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی سالمندان",
                        "ar" to "اليوم الدولي للمسنين",
                        "en" to "International Day of Older Persons",
                        "es" to "Día Internacional de las Personas de Edad",
                        "fr" to "Journée internationale pour les personnes âgées",
                        "ru" to "Международный день пожилых людей",
                        "zh" to "国际老年人日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 1),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/older-persons-day",
                        title = "International Day of Older Persons (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/45/106",
                    "un" to "https://www.un.org/en/observances/older-persons-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.ozone-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی حفظ لایه اوزون",
                        "ar" to "اليوم الدولي لحفظ طبقة الأوزون",
                        "en" to "International Day for the Preservation of the Ozone Layer",
                        "es" to "Día Internacional de la Preservación de la Capa de Ozono",
                        "fr" to "Journée internationale de la protection de la couche d'ozone",
                        "ru" to "Международный день охраны озонового слоя",
                        "zh" to "保护臭氧层国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 16),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/ozone-day",
                        title = "International Day for the Preservation of the Ozone Layer (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/49/114",
                    "un" to "https://www.un.org/en/observances/ozone-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.parents-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی والدین",
                        "en" to "Global Day of Parents",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 1),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_012,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/parents-day",
                            title = "Global Day of Parents — A/RES/66/292 (2012)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/parents-day",
                        title = "Global Day of Parents (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/parents-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.parliamentarism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پارلمانتاریسم",
                        "en" to "International Day of Parliamentarism",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/parliamentarism-day",
                            title = "International Day of Parliamentarism — A/RES/72/278 (2018)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/parliamentarism-day",
                        title = "International Day of Parliamentarism (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/parliamentarism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.peaceful-coexistence-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی همزیستی مسالمت‌آمیز",
                        "en" to "International Day of Peaceful Coexistence",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 28),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/peaceful-coexistence-day",
                            title = "International Day of Peaceful Coexistence — A/RES/79/269 (4 March 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/peaceful-coexistence-day",
                        title = "International Day of Peaceful Coexistence (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/peaceful-coexistence-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.peacekeepers-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی حافظان صلح ملل متحد",
                        "ar" to "اليوم الدولي لحفظة السلام",
                        "en" to "International Day of UN Peacekeepers",
                        "es" to "Día Internacional del Personal de Paz de las Naciones Unidas",
                        "fr" to "Journée internationale des Casques bleus des Nations Unies",
                        "ru" to "Международный день миротворцев ООН",
                        "zh" to "联合国维持和平人员国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/peacekeepers-day",
                        title = "International Day of UN Peacekeepers (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/57/129",
                    "un" to "https://www.un.org/en/observances/peacekeepers-day",
                ),
        ),
    )
