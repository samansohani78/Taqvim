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
 * Part 18 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_18: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.equal-pay-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی برابری دستمزد",
                        "en" to "International Equal Pay Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 18),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/equal-pay-day",
                            title = "International Equal Pay Day (A/RES/74/142, 2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/equal-pay-day",
                        title = "International Equal Pay Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/equal-pay-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.fair-play-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی بازی منصفانه",
                        "en" to "World Fair Play Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 19),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/fair-play-day",
                            title = "World Fair Play Day — A/RES/78/310 (1 July 2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/fair-play-day",
                        title = "World Fair Play Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/fair-play-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.female-genital-mutilation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی عدم تحمل ختنه زنان",
                        "en" to "International Day of Zero Tolerance to Female Genital Mutilation",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 6),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_012,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/female-genital-mutilation-day",
                            title = "Zero Tolerance to FGM Day — A/RES/67/146 (2012)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/female-genital-mutilation-day",
                        title = "International Day of Zero Tolerance to Female Genital Mutilation (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/female-genital-mutilation-day",
                ),
        ),
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
                        "en" to "International Day for the Prevention of and Fight against All Forms of Transnational Organized Crime",
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
                        "en" to "World Food Safety Day",
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
                        "en" to "World Football Day",
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
        EventDefinition(
            id = EventId("un.friendship-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی دوستی",
                        "en" to "International Day of Friendship",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_011,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/friendship-day",
                            title = "International Day of Friendship — A/RES/65/275 (2011)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/friendship-day",
                        title = "International Day of Friendship (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/friendship-day",
                ),
        ),
    )
