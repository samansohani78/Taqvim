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
 * Part 53 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_53: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.markhor-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مارخور",
                        "ar" to "اليوم الدولي لوعل المارخور",
                        "en" to "International Day of the Markhor",
                        "fr" to "Journée internationale du markhor",
                        "ru" to "Международный день мархура",
                        "zh" to "国际捻角山羊日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 24),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/markhor-day",
                            title = "International Day of the Markhor — A/RES/78/278 (2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/markhor-day",
                        title = "International Day of the Markhor (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/markhor-day",
                ),
        ),
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
                        "ar" to "الأسبوع العالمي للدراية الإعلامية والمعلوماتية",
                        "en" to "Global Media and Information Literacy Week",
                        "es" to "Semana Mundial de la Alfabetización Mediática e Informacional",
                        "fr" to "Semaine mondiale de l’éducation aux médias et à l’information",
                        "ru" to "Глобальная неделя медийной и информационной грамотности",
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
                        "ar" to "اليوم العالمي للتأمل",
                        "en" to "World Meditation Day",
                        "ru" to "Всемирный день медитации",
                        "zh" to "世界冥想日",
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
    )
