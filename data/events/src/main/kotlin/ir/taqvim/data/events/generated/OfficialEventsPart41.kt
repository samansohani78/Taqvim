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
 * Part 41 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_41: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.fight-against-transnational-crime-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پیشگیری و مبارزه با همه اشکال جرایم سازمان‌یافته فراملی",
                        "ar" to "اليوم الدولي لمنع ومكافحة جميع أشكال الجريمة المنظمة عبر الوطنية",
                        "en" to "International Day for the Prevention of and Fight against All Forms of Transnational Organized Crime",
                        "fr" to "Journée internationale de la prévention de toutes les formes de criminalité transnationale organisée et de la lutte contre ce fléau",
                        "ru" to "Международный день предупреждения всех форм транснациональной организованной преступности и борьбы с ними",
                        "zh" to "预防和打击一切形式跨国有组织犯罪国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 15),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/fight-against-transnational-crime-day",
                            title = "International Day for the Prevention of and Fight against Transnational Organized Crime — A/RES/78/267 (March 2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/fight-against-transnational-crime-day",
                        title = "International Day for the Prevention of and Fight against All Forms of Transnational Organized Crime (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/fight-against-transnational-crime-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.food-safety-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی ایمنی غذا",
                        "ar" to "اليوم العالمي لسلامة الأغذية<",
                        "en" to "World Food Safety Day",
                        "es" to "Día Mundial de la Inocuidad de los Alimentos",
                        "fr" to "Journée internationale de la sécurité sanitaire des aliments",
                        "ru" to "Всемирный день безопасности пищевых продуктов",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/food-safety-day",
                            title = "World Food Safety Day -- GA resolution 73/250 (20 December 2018)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/food-safety-day",
                        title = "World Food Safety Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/food-safety-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.football-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی فوتبال",
                        "ar" to "اليوم العالمي لكرة القدم",
                        "en" to "World Football Day",
                        "fr" to "Journée mondiale du football",
                        "ru" to "Всемирный день футбола",
                        "zh" to "世界足球日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 25),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/football-day",
                            title = "World Football Day — A/RES/78/281 (7 May 2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/football-day",
                        title = "World Football Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/football-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.french-language-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز زبان فرانسوی",
                        "en" to "French Language Day",
                        "es" to "Día de la lengua francesa",
                        "fr" to "Journée de la langue française",
                        "ru" to "День французского языка в ООН",
                        "zh" to "联合国法文日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 20),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_010,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/russian-language-day",
                            title = "UN Language Days established 2010 by the Department of Global Communications for each of the six official languages",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/fr/observances/french-language-day/",
                        title = "French Language Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/fr/observances/french-language-day/",
                ),
        ),
    )
