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
 * Part 36 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_36: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.world-glaciers-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی یخچال‌های طبیعی",
                        "en" to "World Day for Glaciers",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/world-glaciers-day",
                            title = "World Day for Glaciers -- GA resolution A/RES/77/158 (2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-glaciers-day",
                        title = "World Day for Glaciers (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-glaciers-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-health-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی بهداشت",
                        "en" to "World Health Day",
                        "fr" to "Journée mondiale de la santé",
                        "ru" to "Всемирный день здоровья",
                        "zh" to "世界卫生日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the zh, fr, ru editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-health-day",
                        title = "World Health Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://apps.who.int/iris/bitstream/handle/10665/86099/WHA2.35_eng.pdf",
                    "un" to "https://www.who.int/campaigns/world-health-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-hepatitis-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی هپاتیت",
                        "en" to "World Hepatitis Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 28),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_010,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.who.int/campaigns/world-hepatitis-day",
                            title = "World Hepatitis Day — 63rd World Health Assembly resolution WHA63.18 (19 May 2010)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-hepatitis-day",
                        title = "World Hepatitis Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.who.int/campaigns/world-hepatitis-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-intellectual-property-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مالکیت معنوی",
                        "en" to "World Intellectual Property Day",
                        "es" to "Día Mundial de la Propiedad Intelectual",
                        "fr" to "Journée mondiale de la propriété intellectuelle",
                        "ru" to "Международный день интеллектуальной собственности",
                        "zh" to "世界知识产权日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 26),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "http://www.wipo.int/ip-outreach/en/ipday/",
                        title = "World Intellectual Property Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "http://www.wipo.int/ip-outreach/en/ipday/",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-lake-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی دریاچه",
                        "en" to "World Lake Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 27),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/world-lake-day",
                            title = "World Lake Day (A/RES/79/142, adopted 12 December 2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-lake-day",
                        title = "World Lake Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-lake-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-malaria-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مالاریا",
                        "ar" to "اليوم العالمي للملاريا",
                        "en" to "World Malaria Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 25),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-malaria-day/world-malaria-day-2021",
                        title = "World Malaria Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.who.int/campaigns/world-malaria-day/world-malaria-day-2021",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-mental-health-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی بهداشت روان",
                        "ar" to "اليوم العالمي للصحة النفسية",
                        "en" to "World Mental Health Day",
                        "es" to "Día Mundial de la Salud Mental",
                        "fr" to "Journée mondiale de la santé mentale",
                        "ru" to "Всемирный день психического здоровья",
                        "zh" to "世界精神卫生日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 10),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "http://www.who.int/mental_health/world-mental-health-day/en/",
                        title = "World Mental Health Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "http://www.who.int/mental_health/world-mental-health-day/en/",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-meteorological-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی هواشناسی",
                        "ar" to "اليوم العالمي للأرصاد الجوية",
                        "en" to "World Meteorological Day",
                        "es" to "Día Meteorológico Mundial",
                        "fr" to "Journée météorologique mondiale",
                        "ru" to "Всемирный метеорологический день",
                        "zh" to "世界气象日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://wmo.int/about-wmo/world-meteorological-day",
                        title = "World Meteorological Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://library.wmo.int/viewer/32802?medianame=99_en_#page=66&amp;viewer=picture&amp;o=bookmarks&amp;n=0&amp;q=",
                    "un" to "https://wmo.int/about-wmo/world-meteorological-day",
                ),
        ),
    )
