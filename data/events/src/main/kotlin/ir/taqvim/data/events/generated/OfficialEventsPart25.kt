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
 * Part 25 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_25: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.media-information-literacy-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته جهانی سواد رسانه‌ای و اطلاعاتی",
                        "en" to "Global Media and Information Literacy Week",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 10, day = 24), lengthDays = 8),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/media-information-literacy-week",
                            title = "General Assembly resolution A/RES/75/267 (2021): Global Media and Information Literacy Week, 24-31 October",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/media-information-literacy-week",
                        title = "General Assembly resolution A/RES/75/267 (2021): Global Media and Information Literacy Week, 24-31 October",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/media-information-literacy-week",
                ),
        ),
        EventDefinition(
            id = EventId("un.meditation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مراقبه",
                        "en" to "World Meditation Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/meditation-day",
                            title = "World Meditation Day — A/RES/79/137 (2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/meditation-day",
                        title = "World Meditation Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/meditation-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.mediterranean-diet-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی رژیم غذایی مدیترانه‌ای",
                        "en" to "International Day of the Mediterranean Diet",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 16),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://docs.un.org/en/A/RES/80/174",
                            title = "International Day of the Mediterranean Diet — General Assembly resolution A/RES/80/174, \"Resolution adopted by the General Assembly on 18 December 2025\", para. 1: \"Decides to designate 16 November as the International Day of the Mediterranean Diet\".",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://docs.un.org/en/A/RES/80/174",
                        title = "International Day of the Mediterranean Diet (UN General Assembly resolution A/RES/80/174)",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://docs.un.org/en/A/RES/80/174",
                ),
        ),
        EventDefinition(
            id = EventId("un.micro-small-medium-businesses-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز کسب‌وکارهای خرد، کوچک و متوسط",
                        "en" to "Micro-, Small and Medium-sized Enterprises Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 27),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/micro-small-medium-businesses-day",
                            title = "Micro-, Small and Medium-sized Enterprises Day — A/RES/71/279 (6 April 2017)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/micro-small-medium-businesses-day",
                        title = "Micro-, Small and Medium-sized Enterprises Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/micro-small-medium-businesses-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.migrants-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی مهاجران",
                        "ar" to "اليوم الدولي للمهاجرين",
                        "en" to "International Migrants Day",
                        "es" to "Día Internacional del Migrante",
                        "fr" to "Journée internationale des migrants",
                        "ru" to "Международный день мигранта",
                        "zh" to "国际移民日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 18),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/migrants-day",
                        title = "International Migrants Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/55/93",
                    "un" to "https://www.un.org/en/observances/migrants-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.migratory-bird-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پرندگان مهاجر",
                        "ar" to "اليوم العالمي للطيور المهاجرة",
                        "en" to "World Migratory Bird Day",
                        "fr" to "Journée mondiale des oiseaux migrateurs",
                        "ru" to "Всемирный день мигрирующих птиц",
                        "zh" to "世界候鸟日",
                    ),
                ),
            rule = EventRule.NthWeekdayOfMonth(month = 5, weekday = Weekday.SATURDAY, n = 2),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.worldmigratorybirdday.org/",
                        title = "World Migratory Bird Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-18); copy in docs/sources/unic-tehran-f-event-20121005.html; row \"روز بین الملل یپرندگان مهاجر — 9 و 10 مه\"",
                    ),
                    Citation(
                        url = "https://www.worldmigratorybirdday.org/",
                        title = "World Migratory Bird Day (CMS, AEWA, EAAFP and Environment for the Americas) — campaign dates",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.worldmigratorybirdday.org/",
                ),
        ),
        EventDefinition(
            id = EventId("un.migratory-bird-day-october"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پرندگان مهاجر",
                        "ar" to "اليوم العالمي للطيور المهاجرة",
                        "en" to "World Migratory Bird Day",
                        "fr" to "Journée mondiale des oiseaux migrateurs",
                        "ru" to "Всемирный день мигрирующих птиц",
                        "zh" to "世界候鸟日",
                    ),
                ),
            rule = EventRule.NthWeekdayOfMonth(month = 10, weekday = Weekday.SATURDAY, n = 2),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.cms.int/campaign/world-migratory-bird-day-wmbd",
                            title = "World Migratory Bird Day (CMS campaign page) — the October peak day dates from the CMS/AEWA/Environment for the Americas partnership announced \"On 26 October 2017 in the margins of the CMS COP12 in Manila\": \"From 2018 onwards, the new joint campaign adopted the single name of “World Migratory Bird Day” and is celebrated twice a year, on the second Saturday in May and in October\". First October observance: 13 October 2018.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.worldmigratorybirdday.org/",
                        title = "World Migratory Bird Day (CMS, AEWA, EAAFP and Environment for the Americas) — campaign dates: \"World Migratory Bird Day 2026 will take place on 9 May and 10 October, recognizing that migration occurs at different times in the northern and southern hemispheres\"",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-18); copy in docs/sources/unic-tehran-f-event-20121005.html; same Persian title as un.migratory-bird-day, the May occurrence of this twice-yearly observance",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.worldmigratorybirdday.org/",
                ),
        ),
        EventDefinition(
            id = EventId("un.mine-awareness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی آگاهی از مین و کمک به اقدام علیه مین",
                        "ar" to "اليوم الدولي للتوعية بخطر الألغام",
                        "en" to "International Day for Mine Awareness and Assistance in Mine Action",
                        "es" to "Día Internacional de información sobre el peligro de las minas y de asistencia para las actividades relativas a las minas",
                        "fr" to "Journée internationale pour la sensibilisation aux mines et l'assistance à la lutte antimines",
                        "ru" to "Международный день просвещения по вопросам минной опасности и помощи в деятельности, связанной с разминированием",
                        "zh" to "国际提高地雷意识和协助地雷行动日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 4),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/mine-awareness-day",
                        title = "International Day for Mine Awareness and Assistance in Mine Action (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/60/97",
                    "un" to "https://www.un.org/en/observances/mine-awareness-day",
                ),
        ),
    )
