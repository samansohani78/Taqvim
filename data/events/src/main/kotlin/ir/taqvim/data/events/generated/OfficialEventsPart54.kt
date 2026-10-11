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
 * Part 54 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_54: List<EventDefinition> =
    listOf(
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
                        "ar" to "يوم المؤسسات المتناهية الصغر والصغيرة والمتوسطة",
                        "en" to "Micro-, Small and Medium-sized Enterprises Day",
                        "es" to "Día de las Microempresas y las Pequeñas y Medianas Empresa",
                        "fr" to "Journée des micro-, petites et moyennes entreprises",
                        "ru" to "День микро-, малых и средних предприятий",
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
                        "fa" to "روز بین‌المللی مهاجران",
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
                    fromYear = 2_018,
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
    )
