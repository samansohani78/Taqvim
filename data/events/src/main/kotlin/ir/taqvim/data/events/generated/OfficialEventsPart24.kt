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
 * Part 24 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_24: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.landlocked-developing-countries-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی آگاهی از نیازها و چالش‌های ویژه توسعه‌ای کشورهای درحال‌توسعه محصور در خشکی",
                        "en" to "International Day of Awareness of the Special Development Needs and Challenges of Landlocked Developing Countries",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 6),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/landlocked-developing-countries-day",
                            title = "Landlocked Developing Countries Day — A/RES/79/320 (2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/landlocked-developing-countries-day",
                        title = "International Day of Awareness of the Special Development Needs and Challenges of Landlocked Developing Countries (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/landlocked-developing-countries-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.living-in-peace-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زندگی مسالمت‌آمیز در کنار هم",
                        "en" to "International Day of Living Together in Peace",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 16),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/living-in-peace-day",
                            title = "International Day of Living Together in Peace (UN General Assembly A/RES/72/130, adopted 8 Dec 2017)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/living-in-peace-day",
                        title = "International Day of Living Together in Peace (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/living-in-peace-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.maritime-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی دریانوردی",
                        "ar" to "يوم الملاحة البحرية العالمي",
                        "en" to "World Maritime Day",
                        "es" to "Día Marítimo Mundial",
                        "fr" to "Journée mondiale de la mer",
                        "zh" to "世界海事日",
                    ),
                ),
            rule = EventRule.LastWeekdayOfMonth(month = 9, weekday = Weekday.THURSDAY),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/maritime-day",
                        title = "World Maritime Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-18); copy in docs/sources/unic-tehran-f-event-20121005.html; row \"روز جهانی دریانوردی — 29 سپتامبر (هفته آخر سپتامبر)\"",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/maritime-day",
                ),
        ),
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
                        "en" to "International Day of the Markhor",
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
    )
