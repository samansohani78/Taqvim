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
 * Part 21 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_21: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.international-day-of-banks"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی بانک‌ها",
                        "en" to "International Day of Banks",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 4),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-of-banks",
                        title = "International Day of Banks (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/international-day-of-banks",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-clean-air-for-blue-skies"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی هوای پاک برای آسمان آبی",
                        "ar" to "اليوم الدولي لنقاوة الهواء من أجل سماء زرقاء",
                        "en" to "International Day of Clean Air for Blue Skies",
                        "es" to "Día Internacional del Aire Limpio por un Cielo Azul",
                        "fr" to "Journée internationale de l’air pur pour des ciels bleus",
                        "ru" to "Международный день чистого воздуха для голубого неба (ЮНЕП)",
                        "zh" to "国际清洁空气蓝天日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.cleanairblueskies.org/",
                        title = "International Day of Clean Air for Blue Skies (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/245526-%D9%BE%DB%8C%D8%A7%D9%85-%D8%AF%D8%A8%DB%8C%D8%B1%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%AC%D9%87%D8%A7%D9%86%DB%8C-%D9%87%D9%88%D8%A7%DB%8C-%D9%BE%D8%A7%DA%A9-%D8%A8%D8%B1%D8%A7%DB%8C-%D8%A2%D8%B3%D9%85%D8%A7%D9%86-%D8%A2%D8%A8%DB%8C-%DB%B2%DB%B0%DB%B2%DB%B3",
                        title = "United Nations in the Islamic Republic of Iran — پیام دبیرکل سازمان ملل متحد به مناسبت روز جهانی هوای پاک برای آسمان آبی ۲۰۲۳",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/74/212",
                    "un" to "https://www.cleanairblueskies.org/",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-families"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی خانواده‌ها",
                        "ar" to "اليوم الدولي للأسر",
                        "en" to "International Day of Families",
                        "es" to "Día Internacional de las Familias",
                        "fr" to "Journée internationale des familles",
                        "ru" to "Международный день семей",
                        "zh" to "国际家庭日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 15),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-of-families",
                        title = "International Day of Families (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/47/237",
                    "un" to "https://www.un.org/en/observances/international-day-of-families",
                ),
        ),
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
    )
