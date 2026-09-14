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
 * Part 14 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_14: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.world-childrens-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی کودکان",
                        "ar" to "اليوم العالمي للطفل",
                        "en" to "World Children's Day",
                        "es" to "Día Mundial de la Infancia",
                        "fr" to "Journée mondiale de l'enfance",
                        "ru" to "Всемирный день ребенка",
                        "zh" to "世界儿童日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-childrens-day",
                        title = "World Children's Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/836(IX)",
                    "un" to "https://www.un.org/en/observances/world-childrens-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-day-against-child-labour"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی علیه کار کودک",
                        "ar" to "اليوم العالمي لمكافحة عمل الأطفال",
                        "en" to "World Day Against Child Labour",
                        "es" to "Día Mundial contra el Trabajo Infantil",
                        "fr" to "Journée mondiale contre le travail des enfants",
                        "ru" to "Всемирный день борьбы с детским трудом",
                        "zh" to "世界无童工日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 12),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-day-against-child-labour",
                        title = "World Day Against Child Labour (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-day-against-child-labour",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-food-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی غذا",
                        "ar" to "يوم الأغذية العالمي",
                        "en" to "World Food Day",
                        "es" to "Día Mundial de la Alimentación",
                        "fr" to "Journée mondiale de l'alimentation",
                        "ru" to "Всемирный день продовольствия",
                        "zh" to "世界粮食日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 16),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "http://www.fao.org/world-food-day",
                        title = "World Food Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/35/70",
                    "un" to "http://www.fao.org/world-food-day",
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
