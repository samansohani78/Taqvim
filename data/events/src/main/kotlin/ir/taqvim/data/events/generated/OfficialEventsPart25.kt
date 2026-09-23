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
import ir.taqvim.core.model.CalendarSystem

/**
 * Part 25 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_25: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.multilateralism-for-peace-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی چندجانبه‌گرایی و دیپلماسی برای صلح",
                        "en" to "International Day of Multilateralism and Diplomacy for Peace",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/Multilateralism-for-Peace-day",
                        title = "International Day of Multilateralism and Diplomacy for Peace (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/Multilateralism-for-Peace-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.nelson-mandela-international-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی نلسون ماندلا",
                        "ar" to "اليوم الدولي لنيلسون مانديلا",
                        "en" to "Nelson Mandela International Day",
                        "es" to "Día Internacional de Nelson Mandela",
                        "fr" to "Journée internationale Nelson Mandela",
                        "ru" to "Международный день Нельсона Манделы",
                        "zh" to "纳尔逊·曼德拉国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 18),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "http://www.un.org/en/events/mandeladay/",
                        title = "Nelson Mandela International Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/64/13",
                    "un" to "http://www.un.org/en/events/mandeladay/",
                ),
        ),
        EventDefinition(
            id = EventId("un.neutrality-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی بی‌طرفی",
                        "en" to "International Day of Neutrality",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 12),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/neutrality-day",
                        title = "International Day of Neutrality (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/neutrality-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.non-violence-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی عدم خشونت",
                        "ar" to "اليوم الدولي للاعنف",
                        "en" to "International Day of Non-Violence",
                        "es" to "Día Internacional de la No Violencia",
                        "fr" to "Journée internationale de la non-violence",
                        "ru" to "Международный день ненасилия",
                        "zh" to "国际非暴力日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 2),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/non-violence-day",
                        title = "International Day of Non-Violence (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/61/271",
                    "un" to "https://www.un.org/en/observances/non-violence-day",
                ),
        ),
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
    )
