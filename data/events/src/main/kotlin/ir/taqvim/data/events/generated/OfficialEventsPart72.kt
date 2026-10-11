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
 * Part 72 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_72: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.women-and-girls-in-science-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زنان و دختران در علم",
                        "ar" to "اليوم الدولي للمرأة والفتاة في ميدان العلوم",
                        "en" to "International Day of Women and Girls in Science",
                        "fr" to "Journée internationale des femmes et des filles de science",
                        "ru" to "Международный день женщин и девочек в науке",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 11),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_015,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/list-days-weeks",
                            title = "International Day of Women and Girls in Science -- GA resolution A/RES/70/212 (22 Dec 2015)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/women-and-girls-in-science-day/",
                        title = "International Day of Women and Girls in Science (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/women-and-girls-in-science-day/",
                ),
        ),
        EventDefinition(
            id = EventId("un.women-girls-african-descent"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زنان و دختران تبار آفریقایی",
                        "ar" to "اليوم الدولي للنساء والفتيات المنحدرات من أصل أفريقي",
                        "en" to "International Day of Women and Girls of African Descent",
                        "es" to "Día Internacional de las Mujeres y Niñas Afrodescendientes",
                        "fr" to "Journée internationale des femmes et des filles d’ascendance africaine",
                        "ru" to "Международный день женщин и девочек африканского происхождения",
                        "zh" to "非洲裔妇女和女童国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 25),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/women-girls-african-descent",
                            title = "International Day of Women and Girls of African Descent — A/RES/78/323 (13 August 2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/women-girls-african-descent",
                        title = "International Day of Women and Girls of African Descent (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/women-girls-african-descent",
                ),
        ),
        EventDefinition(
            id = EventId("un.women-in-diplomacy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زنان در دیپلماسی",
                        "ar" to "اليوم الدولي للمرأة في العمل الدبلوماسي",
                        "en" to "International Day of Women in Diplomacy",
                        "es" to "Día Internacional de las Mujeres en la Diplomacia",
                        "fr" to "Journée internationale des femmes dans la diplomatie",
                        "ru" to "Международный день женщин в дипломатии",
                        "zh" to "女外交官国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 24),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/women-in-diplomacy-day",
                            title = "International Day of Women in Diplomacy (A/RES/76/269, adopted 20 June 2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/women-in-diplomacy-day",
                        title = "International Day of Women in Diplomacy (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/women-in-diplomacy-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.women-judges-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی قضات زن",
                        "ar" to "اليوم الدولي للقاضيات",
                        "en" to "International Day of Women Judges",
                        "es" to "Día Internacional de las Juezas",
                        "fr" to "Journée internationale des femmes juges",
                        "ru" to "Международный день женщин-судей",
                        "zh" to "女法官国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 10),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/women-judges-day",
                            title = "International Day of Women Judges — resolution 75/274 (26 April 2021)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/women-judges-day",
                        title = "International Day of Women Judges (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/women-judges-day",
                ),
        ),
    )
