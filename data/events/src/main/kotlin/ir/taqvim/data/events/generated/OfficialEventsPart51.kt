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
 * Part 51 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_51: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.international-translation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی ترجمه",
                        "ar" to "اليوم الدولي للترجمة",
                        "en" to "International Translation Day",
                        "es" to "Día Internacional de la Traducción",
                        "fr" to "Journée internationale de la traduction",
                        "ru" to "Международный день перевода",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/international-translation-day",
                            title = "International Translation Day — resolution 71/288 (24 May 2017)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-translation-day",
                        title = "International Translation Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/international-translation-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.interventional-cardiology-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی قلب و عروق مداخله‌ای",
                        "ar" to "اليوم الدولي لطب القلب التدخلي",
                        "en" to "International Day for Interventional Cardiology",
                        "es" to "Día International de la Cardiología Intervencionista",
                        "ru" to "Международный день интервенционной кардиологии",
                        "zh" to "国际介入心脏病学日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 16),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/interventional-cardiology-day",
                            title = "International Day for Interventional Cardiology — resolution 76/302 (September 2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/interventional-cardiology-day",
                        title = "International Day for Interventional Cardiology (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/interventional-cardiology-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.jazz-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی جاز",
                        "ar" to "اليوم الدولي لموسيقى الجاز",
                        "en" to "International Jazz Day",
                        "es" to "Día Internacional del Jazz",
                        "fr" to "Journée internationale du jazz",
                        "ru" to "Международный день джаза",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_011,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/jazz-day",
                            title = "International Jazz Day — UNESCO General Conference, November 2011",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/jazz-day",
                        title = "International Jazz Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/jazz-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.judicial-well-being-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی رفاه قضایی",
                        "ar" to "اليوم الدولي لرفاه القضاء",
                        "en" to "International Day for Judicial Well-being",
                        "fr" to "Journée internationale pour le bien-être des juges",
                        "ru" to "Международный день благополучия судей и судебных работников",
                        "zh" to "司法福祉国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 25),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/judicial-well-being-day",
                            title = "International Day for Judicial Well-being — A/RES/79/266 (4 March 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/judicial-well-being-day",
                        title = "International Day for Judicial Well-being (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/judicial-well-being-day",
                ),
        ),
    )
